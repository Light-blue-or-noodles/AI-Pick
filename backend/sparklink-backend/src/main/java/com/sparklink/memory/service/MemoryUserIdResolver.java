package com.sparklink.memory.service;

/**
 * 记忆库用户身份键解析器。
 *
 * @author AI-Pick
 */
public final class MemoryUserIdResolver {

    private static final String USER_KEY_PREFIX = "sparklink:user:";
    private static final String ENV_USER_KEY_PREFIX = "sparklink:%s:user:%s";

    private MemoryUserIdResolver() {
    }

    /**
     * 将用户 ID 转为记忆库身份键。
     *
     * @param userId 用户 ID
     * @return 身份键，例如 sparklink:user:10086
     */
    public static String resolve(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        return USER_KEY_PREFIX + userId;
    }

    /**
     * 将用户 ID 转为包含环境标识的记忆库身份键。
     *
     * @param userId 用户 ID
     * @param environmentTag 环境标识（如 dev、prod）
     * @return 身份键，例如 sparklink:dev:user:10086
     */
    public static String resolve(Long userId, String environmentTag) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (environmentTag == null || environmentTag.isBlank()) {
            return resolve(userId);
        }
        String normalizedTag = normalizeEnvironmentTag(environmentTag);
        return String.format(ENV_USER_KEY_PREFIX, normalizedTag, userId);
    }

    static String normalizeEnvironmentTag(String environmentTag) {
        String normalized = environmentTag.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9_-]", "-");
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("environmentTag cannot be blank");
        }
        return normalized;
    }
}
