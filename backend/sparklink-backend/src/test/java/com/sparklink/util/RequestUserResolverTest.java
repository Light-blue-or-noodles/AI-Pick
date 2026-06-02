package com.sparklink.util;

import com.sparklink.common.BusinessException;
import com.sparklink.config.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestUserResolverTest {

    private RequestUserResolver resolver;
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = mock(JwtUtils.class);
        resolver = new RequestUserResolver(jwtUtils);
        ReflectionTestUtils.setField(resolver, "jwtHeader", "Authorization");
        ReflectionTestUtils.setField(resolver, "jwtPrefix", "Bearer ");
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void resolveUserId_prefersHeaderArgument() {
        assertEquals(42L, resolver.resolveUserId(42L));
    }

    @Test
    void resolveUserId_fallsBackToJwtAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("userId", 100L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals(100L, resolver.resolveUserId(null));
    }

    @Test
    void resolveUserId_fallsBackToAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-value");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Claims claims = mock(Claims.class);
        when(claims.get("userId")).thenReturn(88);
        when(jwtUtils.parseToken("token-value")).thenReturn(claims);

        assertEquals(88L, resolver.resolveUserId(null));
    }

    @Test
    void requireUserId_throwsWhenMissing() {
        assertThrows(BusinessException.class, () -> resolver.requireUserId(null));
    }
}
