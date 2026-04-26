package com.sparklink.util;

import org.springframework.util.StringUtils;

import java.net.URI;

/**
 * 媒体路径规范化：持久化与 API 返回统一为相对路径（本站 /static/...），避免把本机 IP、localhost 写入数据库；
 * 第三方头像（微信 CDN）与对象存储公网 URL 保持完整 https 地址。
 *
 * @author AI-Pick
 */
public final class MediaPathUtil {

    private MediaPathUtil() {
    }

    /**
     * 写入数据库前：将完整后端地址、带 /api 前缀的路径转为可迁移的相对路径
     *
     * @param raw 前端或上传接口传入的地址
     * @return 规范化后的存储值；无法识别时返回去首尾空白后的原串
     */
    public static String normalizeForPersistence(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = raw.trim();
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            if (s.startsWith("/api/")) {
                return s.substring(4);
            }
            return s;
        }
        try {
            URI u = URI.create(s);
            String path = u.getPath();
            if (path == null) {
                return s;
            }
            // 第三方头像等：路径不含本站 /static/，保留完整 URL
            if (!path.contains("/static/")) {
                return s;
            }
            if (path.startsWith("/api/")) {
                return path.substring(4);
            }
            return path.startsWith("/static/") ? path : s;
        } catch (IllegalArgumentException e) {
            return s;
        }
    }

    /**
     * 返回给前端：本站资源统一为 /static/...；第三方与 OSS 完整 URL 原样返回
     */
    public static String normalizeForResponse(String stored) {
        if (!StringUtils.hasText(stored)) {
            return null;
        }
        String s = stored.trim();
        if (!s.startsWith("http://") && !s.startsWith("https://")) {
            if (s.startsWith("/api/")) {
                return s.substring(4);
            }
            return s;
        }
        try {
            URI u = URI.create(s);
            String path = u.getPath();
            if (path == null) {
                return s;
            }
            if (!path.contains("/static/")) {
                return s;
            }
            if (path.startsWith("/api/")) {
                return path.substring(4);
            }
            return path.startsWith("/static/") ? path : s;
        } catch (IllegalArgumentException e) {
            return s;
        }
    }
}
