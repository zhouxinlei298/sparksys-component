package com.github.sparkzxl.gateway.plugin.core.context;

import org.springframework.web.server.ServerWebExchange;

/**
 * description: gateway context builder
 *
 * @author zhouxinlei
 * @since 2022-01-07 11:07:10
 */
public interface GatewayContextBuilder {

    /**
     * Build gateway context.
     *
     * @param exchange the exchange
     * @return GatewayContext
     */
    GatewayContext build(ServerWebExchange exchange);

}
