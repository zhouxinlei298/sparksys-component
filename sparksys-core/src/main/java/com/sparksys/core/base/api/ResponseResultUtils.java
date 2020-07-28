package com.sparksys.core.base.api;


import cn.hutool.json.JSONUtil;
import com.sparksys.core.base.api.result.ApiResult;
import com.sparksys.core.constant.CoreConstant;
import com.sparksys.core.support.ResponseResultStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * description: SecurityResponse工具类
 *
 * @author zhouxinlei
 * @date 2020-05-24 13:40:03
 */
@Slf4j
public class ResponseResultUtils {

    public static String getAuthHeader(HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader(CoreConstant.JwtTokenConstant.JWT_TOKEN_HEADER);
        return StringUtils.removeStart(header, CoreConstant.JwtTokenConstant.JWT_TOKEN_HEAD);
    }

    public static void unauthorized(HttpServletResponse response) {
        try {
            response.setStatus(ResponseResultStatus.UN_AUTHORIZED.getCode());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().println(JSONUtil.parseObj(ApiResult.apiResult(ResponseResultStatus.UN_AUTHORIZED)).toStringPretty());
            response.getWriter().flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void forbidden(HttpServletResponse response) {
        try {
            response.setStatus(ResponseResultStatus.REQ_REJECT.getCode());
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().println(JSONUtil.parseObj(ApiResult.apiResult(ResponseResultStatus.REQ_REJECT)).toStringPretty());
            response.getWriter().flush();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
