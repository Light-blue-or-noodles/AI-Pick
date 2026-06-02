package com.sparklink.ai.chat;

import java.util.Locale;

/**
 * 联网搜索模式。
 */
public enum WebSearchMode {
    AUTO,
    ON,
    OFF;

    public static WebSearchMode from(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return AUTO;
        }
        return switch (rawMode.trim().toLowerCase(Locale.ROOT)) {
            case "on" -> ON;
            case "off" -> OFF;
            default -> AUTO;
        };
    }
}
