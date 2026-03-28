package com.aipick.util;

import org.springframework.util.StringUtils;

/**
 * 头像 URL 校验：允许本站 {@code /static/...}、第三方 https（如微信头像）、以及后续 OSS 公网地址。
 * 拒绝临时路径、wxfile 等不可持久化地址。
 *
 * @author AI-Pick
 */
public final class AvatarUtil {

    private AvatarUtil() {
    }

    /** 无效前缀：临时路径、微信本地路径等，不可写入数据库或返回给前端展示 */
    private static final String[] INVALID_PREFIXES = {
            "http://tmp/",
            "https://tmp/",
            "://tmp/",
            "wxfile://",
            "file:///tmp/"
    };

    /**
     * 判断是否为可用的头像引用：本站静态路径、https 第三方、合法 http（少见）等。
     */
    public static boolean isValidAvatarUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String s = url.trim();
        for (String prefix : INVALID_PREFIXES) {
            if (s.contains(prefix)) {
                return false;
            }
        }
        if (s.startsWith("https://")) {
            return true;
        }
        if (s.startsWith("http://")) {
            return s.contains("/static/");
        }
        return s.startsWith("/static/") || s.contains("/static/");
    }

    /**
     * 规范化后若仍无效则返回 null，供 DTO 输出。
     */
    public static String sanitizeForResponse(String avatar) {
        if (!StringUtils.hasText(avatar)) {
            return null;
        }
        String n = MediaPathUtil.normalizeForResponse(avatar.trim());
        return isValidAvatarUrl(n) ? n : null;
    }

    /**
     * 是否为本站 {@code /static/...} 头像（含用户上传、资料里保存的相对路径或带 /static/ 的完整 URL）。
     * 用于微信登录时不让 getUserProfile 的 CDN 头像覆盖用户已改过的头像。
     */
    public static boolean isSiteStaticAvatarRef(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String s = url.trim();
        if (s.startsWith("/static/")) {
            return true;
        }
        if (s.startsWith("http://") || s.startsWith("https://")) {
            return s.contains("/static/");
        }
        return s.contains("/static/avatars/") || s.contains("/static/covers/");
    }
}
