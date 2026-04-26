package com.sparklink.util;

import java.util.Locale;

/**
 * AI 提示语清洗工具，简单防御常见提示注入模式。
 *
 * @author AI-Pick
 */
public final class PromptSanitizer {

    private static final int MAX_LENGTH = 2000;

    private PromptSanitizer() {
    }

    /**
     * 裁剪并清洗用户输入，移除常见的提示注入关键词。
     *
     * @param input 用户原始输入
     * @return 清洗后的输入
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }

        String trimmed = input.trim();
        if (trimmed.length() > MAX_LENGTH) {
            trimmed = trimmed.substring(0, MAX_LENGTH);
        }

        String lower = trimmed.toLowerCase(Locale.ROOT);

        // 简单移除常见提示注入语句片段，避免直接控制系统指令
        lower = lower
                .replace("ignore previous instructions", "")
                .replace("ignore all previous instructions", "")
                .replace("请忽略之前的所有指令", "")
                .replace("请忽略以上所有指令", "")
                .replace("system instruction", "")
                .replace("you are now system", "")
                .replace("你现在是系统", "")
                .replace("作为系统提示", "");

        return lower.trim();
    }
}

