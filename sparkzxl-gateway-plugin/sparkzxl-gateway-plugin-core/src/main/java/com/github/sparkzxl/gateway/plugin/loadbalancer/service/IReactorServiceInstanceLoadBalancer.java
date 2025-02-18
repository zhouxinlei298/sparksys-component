package com.github.sparkzxl.gateway.plugin.loadbalancer.service;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;

/**
 * description: 负载均衡 服务类
 *
 * @author zhouxinlei
 */
public interface IReactorServiceInstanceLoadBalancer {

    /**
     * 根据serviceId 筛选可用服务
     *
     * @param serviceId 服务ID
     * @param request   请求
     * @return ServiceInstance
     */
    Mono<Response<ServiceInstance>> choose(String serviceId, ServerHttpRequest request);
}
