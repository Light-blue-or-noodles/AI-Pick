package com.sparklink.memory.service;

import cn.hutool.core.util.IdUtil;
import com.sparklink.memory.client.MemoryLibraryClient;
import com.sparklink.memory.config.MemoryLibraryProperties;
import com.sparklink.memory.filter.MemoryFieldWhitelistFilter;
import com.sparklink.memory.metrics.MemoryMetricsRecorder;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.queue.MemoryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆能力统一门面：负责召回、Prompt 合并、异步入队。
 */
@Slf4j
@Service
public class MemoryFacade {

    private final MemoryEventProducer memoryEventProducer;
    private final MemoryLibraryClient memoryLibraryClient;
    private final MemoryMetricsRecorder memoryMetricsRecorder;
    private final String memoryEnvironmentTag;

    public MemoryFacade(MemoryEventProducer memoryEventProducer, MemoryLibraryClient memoryLibraryClient) {
        this(memoryEventProducer, memoryLibraryClient, new MemoryMetricsRecorder(), "");
    }

    MemoryFacade(MemoryEventProducer memoryEventProducer, MemoryMetricsRecorder memoryMetricsRecorder) {
        this(memoryEventProducer, null, memoryMetricsRecorder, "");
    }

    public MemoryFacade(MemoryEventProducer memoryEventProducer,
                        MemoryLibraryClient memoryLibraryClient,
                        MemoryMetricsRecorder memoryMetricsRecorder) {
        this(memoryEventProducer, memoryLibraryClient, memoryMetricsRecorder, "");
    }

    @Autowired
    public MemoryFacade(MemoryEventProducer memoryEventProducer,
                        MemoryLibraryClient memoryLibraryClient,
                        MemoryMetricsRecorder memoryMetricsRecorder,
                        MemoryLibraryProperties memoryLibraryProperties) {
        this(memoryEventProducer, memoryLibraryClient, memoryMetricsRecorder,
                memoryLibraryProperties == null ? "" : memoryLibraryProperties.getEnvironmentTag());
    }

    private MemoryFacade(MemoryEventProducer memoryEventProducer,
                         MemoryLibraryClient memoryLibraryClient,
                         MemoryMetricsRecorder memoryMetricsRecorder,
                         String memoryEnvironmentTag) {
        this.memoryEventProducer = memoryEventProducer;
        this.memoryLibraryClient = memoryLibraryClient;
        this.memoryMetricsRecorder = memoryMetricsRecorder;
        this.memoryEnvironmentTag = memoryEnvironmentTag;
    }

    /**
     * 召回记忆上下文。当前版本兜底为空，避免阻塞主业务链路。
     */
    public MemoryContext recallForPrompt(Long userId, String query) {
        if (userId == null || !StringUtils.hasText(query)) {
            return MemoryContext.empty();
        }
        try {
            if (memoryLibraryClient == null) {
                return MemoryContext.empty();
            }
            String memoryUserId = MemoryUserIdResolver.resolve(userId, memoryEnvironmentTag);
            MemoryContext context = memoryLibraryClient.searchMemory(memoryUserId, query);
            if (context == null || context.isEmpty()) {
                return MemoryContext.empty();
            }
            Map<String, String> filteredProfile = MemoryFieldWhitelistFilter.filterProfileForPrompt(context.getProfileAttributes());
            MemoryContext sanitized = context.withProfileAttributes(filteredProfile);
            if (!sanitized.isEmpty()) {
                memoryMetricsRecorder.recordSearchSuccess();
            }
            return sanitized;
        } catch (Exception ex) {
            memoryMetricsRecorder.recordSearchFailure();
            log.warn("recall memory failed, userId={}, reason={}", userId, ex.getMessage());
            return MemoryContext.empty();
        }
    }

    /**
     * 合并 Prompt 与记忆上下文。当前版本保留原始 Prompt，后续可扩展注入策略。
     */
    public String mergePrompt(String rawPrompt, MemoryContext context) {
        if (rawPrompt == null) {
            return "";
        }
        if (context == null || context.isEmpty()) {
            return rawPrompt;
        }
        List<String> memoryLines = new ArrayList<>();
        if (!context.getMemorySnippets().isEmpty()) {
            memoryLines.add("【历史记忆】");
            memoryLines.addAll(context.getMemorySnippets().stream()
                    .filter(StringUtils::hasText)
                    .map(item -> "- " + item.trim())
                    .collect(Collectors.toList()));
        }
        if (!context.getProfileAttributes().isEmpty()) {
            memoryLines.add("【用户画像】");
            context.getProfileAttributes().forEach((key, value) -> {
                if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                    memoryLines.add("- " + key.trim() + ": " + value.trim());
                }
            });
        }
        if (memoryLines.isEmpty()) {
            return rawPrompt;
        }
        return rawPrompt + "\n\n" + String.join("\n", memoryLines);
    }

    /**
     * 将一轮会话异步入队到记忆写入链路。
     */
    public void enqueueConversation(Long userId, String userMessage, String assistantMessage) {
        if (userId == null || !StringUtils.hasText(userMessage) || !StringUtils.hasText(assistantMessage)) {
            return;
        }
        try {
            String memoryUserId = MemoryUserIdResolver.resolve(userId, memoryEnvironmentTag);
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", userMessage),
                    Map.of("role", "assistant", "content", assistantMessage)
            );
            String idempotencyKey = buildIdempotencyKey(memoryUserId, userMessage, assistantMessage);
            memoryEventProducer.publish(memoryUserId, messages, idempotencyKey);
            memoryMetricsRecorder.recordEnqueueSuccess();
        } catch (Exception ex) {
            // 记忆写入为异步增强能力，不应影响主业务响应
            memoryMetricsRecorder.recordEnqueueFailure();
            log.warn("enqueue conversation to memory failed, userId={}, reason={}", userId, ex.getMessage());
        }
    }

    private String buildIdempotencyKey(String memoryUserId, String userMessage, String assistantMessage) {
        String source = memoryUserId + "|" + userMessage.trim() + "|" + assistantMessage.trim();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            log.warn("build idempotency key failed, fallback uuid, reason={}", ex.getMessage());
            return IdUtil.fastSimpleUUID();
        }
    }
}
