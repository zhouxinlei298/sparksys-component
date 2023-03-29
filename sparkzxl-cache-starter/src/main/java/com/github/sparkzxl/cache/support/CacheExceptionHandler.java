package com.github.sparkzxl.cache.support;

import com.github.sparkzxl.core.base.result.ApiResult;
import com.github.sparkzxl.core.constant.enums.BeanOrderEnum;
import com.github.sparkzxl.core.support.code.ResultErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.redis.ClusterRedirectException;
import org.springframework.data.redis.ClusterStateFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.TooManyClusterRedirectionsException;
import org.springframework.data.redis.connection.ClusterCommandExecutionFailureException;
import org.springframework.data.redis.connection.RedisSubscribedConnectionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.exceptions.JedisException;

/**
 * description: 缓存异常处理
 *
 * @author zhouxinlei
 */
@ControllerAdvice
@RestController
@Slf4j
public class CacheExceptionHandler implements Ordered {

    @ExceptionHandler(ClusterRedirectException.class)
    public ApiResult<?> handleClusterRedirectException(ClusterRedirectException e) {
        log.error("ClusterRedirectException 异常:", e);
        return ApiResult.fail(ResultErrorCode.CLUSTER_REDIRECT_EXCEPTION.getErrorCode(),
                ResultErrorCode.CLUSTER_REDIRECT_EXCEPTION.getErrorMsg());
    }

    @ExceptionHandler(JedisException.class)
    public ApiResult<?> handleJedisException(JedisException e) {
        log.error("JedisException 异常:", e);
        return ApiResult.fail(ResultErrorCode.FAILURE.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(ClusterStateFailureException.class)
    public ApiResult<?> handleClusterStateFailureException(ClusterStateFailureException e) {
        log.error("ClusterStateFailureException 异常:", e);
        return ApiResult.fail(
                ResultErrorCode.CLUSTER_STATE_FAILURE_EXCEPTION.getErrorCode(),
                ResultErrorCode.CLUSTER_STATE_FAILURE_EXCEPTION.getErrorMsg());
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ApiResult<?> handleRedisConnectionFailureException(RedisConnectionFailureException e) {
        log.error("RedisConnectionFailureException 异常:", e);
        return ApiResult.fail(
                ResultErrorCode.REDIS_CONNECTION_FAILURE_EXCEPTION.getErrorCode(),
                ResultErrorCode.REDIS_CONNECTION_FAILURE_EXCEPTION.getErrorMsg());
    }

    @ExceptionHandler(RedisSystemException.class)
    public ApiResult<?> handleRedisSystemException(RedisSystemException e) {
        log.error("RedisSystemException 异常:", e);
        return ApiResult.fail(
                ResultErrorCode.REDIS_SYSTEM_EXCEPTION.getErrorCode(), ResultErrorCode.REDIS_SYSTEM_EXCEPTION.getErrorMsg());
    }

    @ExceptionHandler(TooManyClusterRedirectionsException.class)
    public ApiResult<?> handleTooManyClusterRedirectionsException(TooManyClusterRedirectionsException e) {
        log.error("TooManyClusterRedirectionsException 异常:", e);
        return ApiResult.fail(
                ResultErrorCode.TOO_MANY_CLUSTER_REDIRECTIONS_EXCEPTION.getErrorCode(),
                ResultErrorCode.TOO_MANY_CLUSTER_REDIRECTIONS_EXCEPTION.getErrorMsg());
    }

    @ExceptionHandler(ClusterCommandExecutionFailureException.class)
    public ApiResult<?> handleTooManyClusterRedirectionsException(ClusterCommandExecutionFailureException e) {
        log.error("ClusterCommandExecutionFailureException 异常:", e);
        return ApiResult.fail(
                ResultErrorCode.CLUSTER_COMMAND_EXECUTION_FAILURE_EXCEPTION.getErrorCode(),
                ResultErrorCode.CLUSTER_COMMAND_EXECUTION_FAILURE_EXCEPTION.getErrorMsg());
    }

    @ExceptionHandler(RedisSubscribedConnectionException.class)
    public ApiResult<?> handleTooManyClusterRedirectionsException(RedisSubscribedConnectionException e) {
        log.error("RedisSubscribedConnectionException 异常:", e);
        return ApiResult.fail(
                ResultErrorCode.REDIS_SUBSCRIBED_CONNECTION_EXCEPTION.getErrorCode(),
                ResultErrorCode.REDIS_SUBSCRIBED_CONNECTION_EXCEPTION.getErrorMsg());
    }

    @Override
    public int getOrder() {
        return BeanOrderEnum.CACHE_EXCEPTION_ORDER.getOrder();
    }
}
