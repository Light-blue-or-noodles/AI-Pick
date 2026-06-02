package com.sparklink.util;

import cn.hutool.core.util.StrUtil;
import com.sparklink.common.BusinessException;
import com.sparklink.config.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从 X-User-Id、JWT 拦截器属性或 Authorization 头解析当前登录用户 ID。
 */
@Component
public class RequestUserResolver {

    private final JwtUtils jwtUtils;

    @Value("${jwt.header}")
    private String jwtHeader;

    @Value("${jwt.prefix}")
    private String jwtPrefix;

    public RequestUserResolver(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    public Long resolveUserId(Long headerUserId) {
        if (headerUserId != null) {
            return headerUserId;
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();

        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }

        String xUserId = request.getHeader("X-User-Id");
        if (xUserId != null && !xUserId.isBlank()) {
            try {
                return Long.parseLong(xUserId.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return parseUserIdFromAuthorization(request);
    }

    public Long requireUserId(Long headerUserId) {
        Long userId = resolveUserId(headerUserId);
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    private Long parseUserIdFromAuthorization(HttpServletRequest request) {
        String token = request.getHeader(jwtHeader);
        if (StrUtil.isBlank(token)) {
            return null;
        }
        if (token.startsWith(jwtPrefix)) {
            token = token.substring(jwtPrefix.length()).trim();
        }
        try {
            Claims claims = jwtUtils.parseToken(token);
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Number number) {
                return number.longValue();
            }
            if (userIdObj instanceof String str && !str.isBlank()) {
                return Long.parseLong(str.trim());
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
