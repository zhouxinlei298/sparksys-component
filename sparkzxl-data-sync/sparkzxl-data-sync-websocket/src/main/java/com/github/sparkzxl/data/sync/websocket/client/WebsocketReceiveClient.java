package com.github.sparkzxl.data.sync.websocket.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import com.github.sparkzxl.core.json.JsonUtils;
import com.github.sparkzxl.core.task.AbstractRoundTask;
import com.github.sparkzxl.core.task.WheelTimerFactory;
import com.github.sparkzxl.core.task.timer.Timer;
import com.github.sparkzxl.core.task.timer.TimerTask;
import com.github.sparkzxl.data.sync.api.BusinessDataSubscriber;
import com.github.sparkzxl.data.sync.api.MetaDataSubscriber;
import com.github.sparkzxl.data.sync.common.entity.PushData;
import com.github.sparkzxl.data.sync.common.enums.DataEventTypeEnum;
import com.github.sparkzxl.data.sync.websocket.handler.WebsocketDataConsumerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * description: The type websocket client.
 *
 * @author zhouxinlei
 * @since 2022-08-25 13:45:02
 */
public class WebsocketReceiveClient extends WebSocketClient {

    private static final Logger logger = LoggerFactory.getLogger(WebsocketReceiveClient.class);
    private final WebsocketDataConsumerHandler consumerHandler;
    private final Timer timer;
    private volatile boolean alreadySync = Boolean.FALSE;
    private TimerTask timerTask;

    /**
     * Instantiates a new websocket client.
     *
     * @param serverUri           the server uri
     * @param metaDataSubscribers the metadata subscribers
     */
    public WebsocketReceiveClient(final URI serverUri,
                                  final List<MetaDataSubscriber> metaDataSubscribers,
                                  final List<BusinessDataSubscriber> businessDataSubscribers) {
        super(serverUri);
        this.consumerHandler = new WebsocketDataConsumerHandler(metaDataSubscribers, businessDataSubscribers);
        this.timer = WheelTimerFactory.getSharedTimer();
        this.connection();
    }

    /**
     * Instantiates a new websocket client.
     *
     * @param serverUri           the server uri
     * @param metaDataSubscribers the metadata subscribers
     */
    public WebsocketReceiveClient(final URI serverUri,
                                  final Map<String, String> headers,
                                  final List<MetaDataSubscriber> metaDataSubscribers,
                                  final List<BusinessDataSubscriber> businessDataSubscribers) {
        super(serverUri, headers);
        this.consumerHandler = new WebsocketDataConsumerHandler(metaDataSubscribers, businessDataSubscribers);
        this.timer = WheelTimerFactory.getSharedTimer();
        this.connection();
    }

    private void connection() {
        this.connectBlocking();
        this.timer.add(timerTask = new AbstractRoundTask(null, TimeUnit.SECONDS.toMillis(10)) {
            @Override
            public void doRun(final String key, final TimerTask timerTask) {
                healthCheck();
            }
        });
    }

    @Override
    public boolean connectBlocking() {
        boolean success = false;
        try {
            success = super.connectBlocking();
        } catch (Exception ignored) {
        }
        if (success) {
            logger.info("websocket connection server[{}] is successful.....", this.getURI().toString());
        } else {
            logger.warn("websocket connection server[{}] is error.....", this.getURI().toString());
        }
        return success;
    }

    @Override
    public void onOpen(final ServerHandshake serverHandshake) {
        if (!alreadySync) {
            send(DataEventTypeEnum.MYSELF.name());
            alreadySync = true;
        }
    }

    @Override
    public void onMessage(final String result) {
        logger.info("websocket received the message [{}].", result);
        handleResult(result);
    }

    @Override
    public void onClose(final int i, final String s, final boolean b) {
        this.close();
    }

    @Override
    public void onError(final Exception e) {
        logger.error("websocket server[{}] is error.....", getURI(), e);
    }

    @Override
    public void close() {
        alreadySync = false;
        if (this.isOpen()) {
            super.close();
        }
    }

    /**
     * Now close. now close. will cancel the task execution.
     */
    public void nowClose() {
        this.close();
        timerTask.cancel();
    }

    private void healthCheck() {
        try {
            if (!this.isOpen()) {
                this.reconnectBlocking();
            } else {
                this.sendPing();
                logger.debug("websocket send to [{}] ping message successful", this.getURI());
            }
        } catch (Exception e) {
            logger.error("websocket connect is error :{}", e.getMessage());
        }
    }

    private void handleResult(final String result) {
        PushData pushData = JsonUtils.getJson().toJavaObject(result, PushData.class);
        String eventType = pushData.getEventType();
        String jsonData = JsonUtils.getJson().toJson(pushData.getData());
        consumerHandler.executor(pushData.getSyncId(), pushData.getConfigGroup(), jsonData, eventType);
    }
}
