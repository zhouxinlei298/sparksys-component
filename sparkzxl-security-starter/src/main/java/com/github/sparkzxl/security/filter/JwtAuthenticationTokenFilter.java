package com.github.sparkzxl.security.filter;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.github.sparkzxl.core.base.result.ResponseInfoStatus;
import com.github.sparkzxl.core.support.ExceptionAssert;
import com.github.sparkzxl.core.util.HttpRequestUtils;
import com.github.sparkzxl.entity.core.JwtUserInfo;
import com.github.sparkzxl.jwt.service.JwtTokenService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * description: jwt认证授权过滤器
 *
 * @author zhouxinlei
 */
@Slf4j
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private JwtTokenService jwtTokenService;
    private UserDetailsService userDetailsService;

    public void setJwtTokenService(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String accessToken = HttpRequestUtils.getAuthHeader(request);
        if (StringUtils.isNotEmpty(accessToken)) {
            JwtUserInfo jwtUserInfo = null;
            try {
                jwtUserInfo = jwtTokenService.verifyTokenByHmac(accessToken);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("校验token发生异常：[{}]", ExceptionUtil.getMessage(e));
                ExceptionAssert.failure(ResponseInfoStatus.TOKEN_EXPIRED_ERROR);
            }
            String username = jwtUserInfo.getUsername();
            log.info("checking username:[{}]", username);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails authUserDetail = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(authUserDetail,
                        null, authUserDetail.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                log.info("authenticated user:[{}]", username);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }

}
