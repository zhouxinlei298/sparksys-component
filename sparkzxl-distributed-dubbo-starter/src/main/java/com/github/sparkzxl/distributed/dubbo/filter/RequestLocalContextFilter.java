package com.github.sparkzxl.distributed.dubbo.filter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import com.github.sparkzxl.core.context.RequestLocalContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * description: dubbo 上下文传递过滤器
 *
 * @author zhouxinlei
 * @since 2022-08-06 14:21:30
 */
@Slf4j
@Activate(group = {CommonConstants.PROVIDER, CommonConstants.CONSUMER}, order = -2)
public class RequestLocalContextFilter implements Filter {

    private static final String REQUEST_LOCAL_CONTEXT = "Request-Local-Context";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcServiceContext context = RpcContext.getServiceContext();
        if (context.isProviderSide()) {
            Map<String, Object> threadLocalMap = Convert.convert(new TypeReference<Map<String, Object>>() {
                @Override
                public Type getType() {
                    return super.getType();
                }
            }, context.getObjectAttachment(REQUEST_LOCAL_CONTEXT));
            RequestLocalContextHolder.setLocalMap(threadLocalMap);
        } else if (RpcContext.getServiceContext().isConsumerSide()) {
            Map<String, Object> threadLocalMap = RequestLocalContextHolder.getLocalMap();
            context.setObjectAttachment(REQUEST_LOCAL_CONTEXT, threadLocalMap);
        }
        return invoker.invoke(invocation);
    }

}
