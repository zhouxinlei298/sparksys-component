package com.github.sparkzxl.data.sync.nacos.handler;

import cn.hutool.core.util.IdUtil;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.google.common.collect.Maps;
import lombok.Getter;
import com.github.sparkzxl.core.json.JsonUtils;
import com.github.sparkzxl.data.sync.api.DataSubscriber;
import com.github.sparkzxl.data.sync.common.entity.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * description: Nacos cache handler.
 *
 * @author zhouxinlei
 * @since 2022-09-06 10:00:54
 */
public class NacosCacheHandler {

    protected static final Map<String, List<Listener>> LISTENERS = Maps.newConcurrentMap();

    private static final Logger logger = LoggerFactory.getLogger(NacosCacheHandler.class);

    @Getter
    private final ConfigService configService;

    private final Map<String, List<DataSubscriber>> dataSubscriberMap = Maps.newConcurrentMap();


    public NacosCacheHandler(final ConfigService configService,
                             final List<DataSubscriber> dataSubscribers) {
        this.configService = configService;
        dataSubscriberMap.putAll(dataSubscribers.stream().collect(Collectors.groupingBy(DataSubscriber::group)));
    }


    protected void updateDataMap(final String dataId, final String configInfo) {
        try {
            List<DataSubscriber> subscribers = dataSubscriberMap.get(dataId);
            Map<String, MetaData> metaDataMap = JsonUtils.getJson().toMap(configInfo, MetaData.class);
            String syncId = IdUtil.fastSimpleUUID();
            metaDataMap.values().forEach(metaData -> subscribers.forEach(subscriber -> {
                subscriber.unSubscribe(syncId, metaData);
                subscriber.onSubscribe(syncId, metaData);
            }));
        } catch (Exception e) {
            logger.error("sync meta data have error:", e);
        }
    }

    private String getConfigAndSignListener(final String dataId, final String group, final Listener listener) {
        String config = null;
        try {
            config = configService.getConfigAndSignListener(dataId, group, 6000, listener);
        } catch (NacosException e) {
            logger.error(e.getMessage(), e);
        }
        if (Objects.isNull(config)) {
            config = "{}";
        }
        return config;
    }

    protected void watcherData(final String dataId, final String group, final OnChange oc) {
        Listener listener = new Listener() {

            @Override
            public void receiveConfigInfo(final String configInfo) {
                oc.change(configInfo);
            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        };
        oc.change(getConfigAndSignListener(dataId, group, listener));
        LISTENERS.computeIfAbsent(dataId, key -> new ArrayList<>()).add(listener);
    }

    protected interface OnChange {

        void change(String changeData);
    }
}
