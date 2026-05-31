package com.sparklink.ai.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 对话工具编排配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.chat")
public class ChatAiProperties {

    /** 是否启用 Spring AI 工具调用路径 */
    private boolean toolsEnabled = true;

    /** 工具编排超时（毫秒） */
    private long toolsTimeoutMs = 25_000L;
}
