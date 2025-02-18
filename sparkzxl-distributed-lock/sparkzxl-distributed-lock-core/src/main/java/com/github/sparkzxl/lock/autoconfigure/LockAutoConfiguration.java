package com.github.sparkzxl.lock.autoconfigure;

import com.github.sparkzxl.lock.DefaultLockFailureStrategy;
import com.github.sparkzxl.lock.DefaultLockKeyBuilder;
import com.github.sparkzxl.lock.LockFailureStrategy;
import com.github.sparkzxl.lock.LockKeyBuilder;
import com.github.sparkzxl.lock.LockTemplate;
import com.github.sparkzxl.lock.annotation.DistributedLock;
import com.github.sparkzxl.lock.aop.LockAnnotationAdvisor;
import com.github.sparkzxl.lock.aop.LockInterceptor;
import com.github.sparkzxl.lock.executor.LockExecutor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * description: 分布式锁自动配置器
 *
 * @author zhouxinlei
 * @since 2022-05-01 22:02:36
 */
@Configuration
@EnableConfigurationProperties(DistributedLockProperties.class)
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
public class LockAutoConfiguration {

    private final DistributedLockProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public LockTemplate lockTemplate(List<LockExecutor> executors) {
        LockTemplate lockTemplate = new LockTemplate();
        lockTemplate.setProperties(properties);
        lockTemplate.setExecutors(executors);
        return lockTemplate;
    }

    @Bean
    @ConditionalOnMissingBean
    public LockKeyBuilder lockKeyBuilder() {
        return new DefaultLockKeyBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockFailureStrategy lockFailureStrategy() {
        return new DefaultLockFailureStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    public LockInterceptor lockInterceptor(LockTemplate lockTemplate, LockKeyBuilder lockKeyBuilder) {
        return new LockInterceptor(lockTemplate, lockKeyBuilder, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public LockAnnotationAdvisor lockAnnotationAdvisor(LockInterceptor lockInterceptor) {
        return new LockAnnotationAdvisor(lockInterceptor, DistributedLock.class, Ordered.HIGHEST_PRECEDENCE);
    }

}
