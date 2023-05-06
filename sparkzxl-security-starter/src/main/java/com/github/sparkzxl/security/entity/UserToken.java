package com.github.sparkzxl.security.entity;

import com.github.sparkzxl.core.entity.LoginUserInfo;

import java.io.Serializable;

import lombok.Data;

/**
 * description: token实体类
 *
 * @author zhouxinlei
 */
@Data
public class UserToken implements Serializable {

    private static final long serialVersionUID = 7155188090339340337L;
    /**
     * token
     */
    private String accessToken;

    /**
     * 有效期
     */
    private Long expiration;

    /**
     * 登录用户信息
     */
    private LoginUserInfo loginUserInfo;

    /**
     * 访问令牌头前缀
     */
    private String tokenType;

}
