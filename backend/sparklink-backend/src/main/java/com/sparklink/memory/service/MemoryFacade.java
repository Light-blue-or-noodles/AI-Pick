package com.sparklink.memory.service;

import cn.hutool.core.util.IdUtil;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.queue.MemoryEventProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 记忆能力统一门面：负责召回、Prompt 合并、异步入队。
 */
@Slf4j
@Service
public class MemoryFacade {

    private final MemoryEventProducer memoryEventProducer;

    public MemoryFacade(MemoryEventProducer memoryEventProducer) {
        this.memoryEventProducer = memoryEventProducer;
    }

    /**
     * 召回记忆上下文。当前版本兜底为空，避免阻塞主业务链路。
     */
    public MemoryContext recallForPrompt(Long userId, String query) {
        if (userId == null || !StringUtils.hasText(query)) {
            return null;
        }
        return null;
    }

    /**
     * 合并 Prompt 与记忆上下文。当前版本保留原始 Prompt，后续可扩展注入策略。
     */
    public String mergePrompt(String rawPrompt, MemoryContext context) {
        if (rawPrompt == null) {
            return "";
        }
        return rawPrompt;
    }

    /**
     * 将一轮会话异步入队到记忆写入链路。
     */
    public void enqueueConversation(Long userId, String userMessage, String assistantMessage) {
        if (userId == null || !StringUtils.hasText(userMessage) || !StringUtils.hasText(assistantMessage)) {
            return;
        }
        try {
            String memoryUserId = MemoryUserIdResolver.resolve(userId);
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", userMessage),
                    Map.of("role", "assistant", "content", assistantMessage)
            );
            memoryEventProducer.publish(memoryUserId, messages, IdUtil.fastSimpleUUID());
        } catch (Exception ex) {
            // 记忆写入为异步增强能力，不应影响主业务响应
            log.warn("enqueue conversation to memory failed, userId={}, reason={}", userId, ex.getMessage());
        }
    }
}
