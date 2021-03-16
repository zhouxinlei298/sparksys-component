package com.github.sparkzxl.security.service;


import org.springframework.security.access.ConfigAttribute;

import java.util.Map;

/**
 * description：动态权限 服务类
 *
 * @author zhouxinlei
 */
public interface DynamicSecurityService {

    /**
     * 加载资源ANT通配符和资源对应MAP
     *
     * @return Map<String, ConfigAttribute>
     */
    Map<String, ConfigAttribute> loadDataSource();

}
