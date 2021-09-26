package com.baidu.fsg.uid.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baidu.fsg.uid.UidGenerator;
import com.baidu.fsg.uid.exception.UidGenerateException;

/**
 * description: 基于Hutool 工具类实现的雪花id生成器
 *
 * @author zhouxinlei
 * @date 2021-09-26 22:18:38
 */
public class HuToolUidGenerator implements UidGenerator {
    private final Snowflake snowflake;

    public HuToolUidGenerator(long workerId, long datacenterId) {
        this.snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }

    @Override
    public long getUid() throws UidGenerateException {
        return snowflake.nextId();
    }

    @Override
    public String parseUid(long uid) {
        long workerId = snowflake.getWorkerId(uid);
        long dataCenterId = snowflake.getDataCenterId(uid);
        long timestamp = snowflake.getGenerateDateTime(uid);

        return String.format("{\"UID\":\"%d\",\"timestamp\":\"%d\",\"workerId\":\"%d\",\"dataCenterId\":\"%d\"}",
                uid, timestamp, workerId, dataCenterId);
    }
}
