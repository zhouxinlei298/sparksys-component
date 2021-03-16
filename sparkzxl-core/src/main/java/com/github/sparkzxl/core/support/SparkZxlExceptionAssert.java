package com.github.sparkzxl.core.support;

import com.github.sparkzxl.core.base.code.BaseEnumCode;

/**
 * description：断言异常全局处理
 *
 * @author zhouxinlei
 */
public class SparkZxlExceptionAssert {


    public static void businessFail(String message) {
        throw new BusinessException(ResponseResultStatus.FAILURE.getCode(), message);
    }

    public static void businessFail(int code, String message) {
        throw new BusinessException(code, message);
    }

    public static void businessFail(BaseEnumCode baseEnumCode) {
        throw new BusinessException(baseEnumCode);
    }

    /**
     * 服务降级异常处理
     */
    public static void serviceDegrade() {
        throw new ServiceDegradeException(ResponseResultStatus.SERVICE_DEGRADATION);
    }

}
