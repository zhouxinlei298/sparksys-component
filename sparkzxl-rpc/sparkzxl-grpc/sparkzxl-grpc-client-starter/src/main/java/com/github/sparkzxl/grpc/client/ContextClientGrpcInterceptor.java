package com.github.sparkzxl.grpc.client;

import com.github.sparkzxl.core.context.RequestLocalContextHolder;
import com.github.sparkzxl.core.json.JsonUtils;
import com.github.sparkzxl.grpc.core.GrpcHeaderContextHolder;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.util.Map;

/**
 * description: 客户端请求拦截器
 *
 * @author zhouxinlei
 * @since 2022-01-25 10:39:47
 */
public class ContextClientGrpcInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions,
            Channel channel) {
        Map<String, Object> threadLocalMap = RequestLocalContextHolder.getLocalMap();
        String threadLocalMapJsonString = JsonUtils.getJson().toJson(threadLocalMap);
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (threadLocalMapJsonString != null) {
                    headers.put(GrpcHeaderContextHolder.HEADER_KEY, threadLocalMapJsonString);
                }
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}
