package com.sparklink.memory.service;

/**
 * 记忆库用户身份键解析器。
 *
 * @author AI-Pick
 */
public final class MemoryUserIdResolver {

    private static final String USER_KEY_PREFIX = "sparklink:user:";

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
}
