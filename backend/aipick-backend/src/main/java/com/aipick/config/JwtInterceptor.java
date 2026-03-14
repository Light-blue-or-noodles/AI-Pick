package com.aipick.config;

import cn.hutool.core.util.StrUtil;
import com.aipick.common.BusinessException;
import com.aipick.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器
 *
 * @author AI-Pick
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Value("${jwt.header}")
    private String header;

    @Value("${jwt.prefix}")
    private String prefix;

    public JwtInterceptor(JwtUtils jwtUtils, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader(header);

        // 如果请求头没有token，放行（让Controller决定是否需要登录）
        if (StrUtil.isBlank(token)) {
            return true;
        }

        // 移除前缀
        if (token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }

        try {
            // 验证token
            Claims claims = jwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            // 将用户信息存入请求属性
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);

            return true;
        } catch (Exception e) {
            // Token无效时返回401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(Result.unauthorized("登录已过期，请重新登录")));
            return false;
        }
    }
}