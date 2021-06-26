package com.github.sparkzxl.feign.interceptor;

import cn.hutool.core.util.StrUtil;
import com.github.sparkzxl.constant.BaseContextConstants;
import com.github.sparkzxl.core.context.BaseContextHolder;
import com.github.sparkzxl.feign.properties.FeignProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * description: feign client 拦截器，
 * 实现将 feign 调用方的 请求头封装到 被调用方的请求头
 *
 * @author zhouxinlei
 */
@Slf4j
public class FeignAddHeaderRequestInterceptor implements RequestInterceptor {

    private FeignProperties feignProperties;

    @Autowired
    public void setFeignProperties(FeignProperties feignProperties) {
        this.feignProperties = feignProperties;
    }

    public static final List<String> HEADER_NAME_LIST = Arrays.asList(
            BaseContextConstants.TENANT, BaseContextConstants.JWT_KEY_USER_ID,
            BaseContextConstants.JWT_KEY_ACCOUNT, BaseContextConstants.JWT_KEY_NAME,
            BaseContextConstants.TRACE_ID_HEADER, BaseContextConstants.JWT_TOKEN_HEADER, "X-Real-IP", "x-forwarded-for"
    );

    public FeignAddHeaderRequestInterceptor() {
    }

    @Override
    public void apply(RequestTemplate template) {
        template.header(BaseContextConstants.REMOTE_CALL, BaseContextConstants.REMOTE_CALL);
        if (feignProperties.isEnable()) {
            String xid = RootContext.getXID();
            if (StrUtil.isNotEmpty(xid)) {
                template.header(RootContext.KEY_XID, xid);
            }
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            HEADER_NAME_LIST.forEach((headerName) -> template.header(headerName, BaseContextHolder.get(headerName)));
            return;
        }

        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        if (request == null) {
            log.warn("path={}, 在FeignClient API接口未配置FeignConfiguration类， 故而无法在远程调用时获取请求头中的参数!", template.path());
            return;
        }
        HEADER_NAME_LIST.forEach((headerName) -> {
            String header = request.getHeader(headerName);
            template.header(headerName, StringUtils.isEmpty(header) ? BaseContextHolder.get(headerName) : header);
        });
    }
}
