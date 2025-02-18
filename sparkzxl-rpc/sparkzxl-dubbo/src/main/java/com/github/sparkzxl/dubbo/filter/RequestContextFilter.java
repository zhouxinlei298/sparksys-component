package com.github.sparkzxl.dubbo.filter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.github.sparkzxl.core.context.RequestLocalContextHolder;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcServiceContext;

/**
 * description: dubbo 上下文传递过滤器
 *
 * @author zhouxinlei
 * @since 2022-08-06 14:21:30
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -2)
public class RequestContextFilter implements Filter, Filter.Listener {

    private static final String REQUEST_LOCAL_CONTEXT = "request-local-context";

    private final ThreadLocal<String> clientType = new TransmittableThreadLocal<String>() {
        @Override
        protected String initialValue() {
            return "";
        }
    };

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcServiceContext context = RpcContext.getServiceContext();
        if (context.isProviderSide()) {
            clientType.set(CommonConstants.PROVIDER);
            Map<String, Object> attachmentMap = context.getObjectAttachments();
            Map<String, Object> threadLocalMap = Convert.convert(new TypeReference<Map<String, Object>>() {
            }, context.getObjectAttachment(REQUEST_LOCAL_CONTEXT));
            attachmentMap.putAll(threadLocalMap);
            RequestLocalContextHolder.setLocalMap(attachmentMap);
        } else if (context.isConsumerSide()) {
            clientType.set(CommonConstants.CONSUMER);
            Map<String, Object> threadLocalMap = RequestLocalContextHolder.getLocalMap();
            context.setObjectAttachment(REQUEST_LOCAL_CONTEXT, threadLocalMap);
        }
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (CommonConstants.PROVIDER.equalsIgnoreCase(clientType.get())) {
            RequestLocalContextHolder.remove();
        }
        clientType.remove();
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        if (CommonConstants.PROVIDER.equalsIgnoreCase(clientType.get())) {
            RequestLocalContextHolder.remove();
        }
        clientType.remove();
    }
}
