package com.github.sparkzxl.gateway.plugin.dubbo.message;

import com.github.sparkzxl.core.base.result.R;
import com.github.sparkzxl.core.support.code.ResultErrorCode;
import com.github.sparkzxl.spi.Join;
import org.springframework.web.server.ServerWebExchange;

/**
 * description: 统一响应结果
 *
 * @author zhouxinlei
 * @since 2022-12-30 15:55:01
 */
@Join
public class ResponseDubboMessageConverter implements DubboMessageConverter {

    @Override
    public Object convert(ServerWebExchange exchange, Object source) {
        if (source instanceof R) {
            return source;
        } else if (source instanceof Boolean && !(Boolean) source) {
            return R.failDetail(
                    ResultErrorCode.FAILURE.getErrorCode(), ResultErrorCode.FAILURE.getErrorMsg());
        } else {
            return R.success(source);
        }
    }
}
