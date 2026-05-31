package com.sparklink.service.impl;

import cn.hutool.core.util.IdUtil;
import com.sparklink.ai.chat.ChatRecommendAssembler;
import com.sparklink.ai.chat.ChatRecommendFallbackService;
import com.sparklink.ai.chat.ChatToolOrchestrator;
import com.sparklink.common.AiConstants;
import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.entity.ChatMessage;
import com.sparklink.mapper.ChatMessageMapper;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.service.MemoryFacade;
import com.sparklink.service.ChatService;
import com.sparklink.integration.DashScopeCompatClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * AI 对话服务：优先 Spring AI 受控工具调用，失败时回落规则召回 + DashScope 兼容接口。
 */
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final ChatToolOrchestrator chatToolOrchestrator;
    private final ChatRecommendFallbackService recommendFallbackService;
    private final ObjectMapper objectMapper;
    private final DashScopeCompatClient dashScopeCompatClient;
    private final MemoryFacade memoryFacade;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper,
                           ChatToolOrchestrator chatToolOrchestrator,
                           ChatRecommendFallbackService recommendFallbackService,
                           ObjectMapper objectMapper,
                           DashScopeCompatClient dashScopeCompatClient,
                           MemoryFacade memoryFacade) {
        this.chatMessageMapper = chatMessageMapper;
        this.chatToolOrchestrator = chatToolOrchestrator;
        this.recommendFallbackService = recommendFallbackService;
        this.objectMapper = objectMapper;
        this.dashScopeCompatClient = dashScopeCompatClient;
        this.memoryFacade = memoryFacade;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse chat(Long userId, ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = IdUtil.fastSimpleUUID();
        }

        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setType(1);
        userMessage.setContent(request.getMessage());
        chatMessageMapper.insert(userMessage);

        List<ChatMessage> history = getHistoryMessages(sessionId, userId);
        String message = request.getMessage();
        MemoryContext memoryContext = memoryFacade.recallForPrompt(userId, message);

        ChatToolOrchestrator.OrchestrationResult orchestrated =
                chatToolOrchestrator.orchestrate(userId, message, history);

        String reply;
        List<ChatRecommendItem> recommends;

        if (orchestrated.usedTools() && StringUtils.hasText(orchestrated.reply())) {
            reply = orchestrated.reply();
            recommends = orchestrated.recommends();
            if (recommends.isEmpty()) {
                recommends = recommendFallbackService.recallFromMessage(message);
            }
        } else {
            recommends = recommendFallbackService.recallFromMessage(message);
            String matchContext = ChatRecommendAssembler.buildMatchContextString(recommends);
            String mergedPrompt = memoryFacade.mergePrompt(matchContext, memoryContext);
            reply = generateReplyWithHistory(message, history, mergedPrompt);
        }

        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setUserId(userId);
        aiMessage.setType(2);
        aiMessage.setContent(reply);
        chatMessageMapper.insert(aiMessage);
        memoryFacade.enqueueConversation(userId, message, reply);

        return new ChatResponse(sessionId, reply, recommends);
    }

    @Override
    public List<ChatMessage> getHistory(String sessionId, Long userId) {
        if (!StringUtils.hasText(sessionId) || userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getUserId, userId)
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper);
    }

    private List<ChatMessage> getHistoryMessages(String sessionId, Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + AiConstants.DEFAULT_HISTORY_MESSAGE_COUNT);
        List<ChatMessage> history = chatMessageMapper.selectList(wrapper);
        Collections.reverse(history);
        return history;
    }

    /**
     * Fallback：DashScope 兼容多轮对话（无 tools）。
     */
    private String generateReplyWithHistory(String currentMessage, List<ChatMessage> history, String matchContext) {
        if (currentMessage == null || currentMessage.trim().isEmpty()) {
            return "请告诉我你想聊些什么？";
        }

        try {
            ArrayNode messages = objectMapper.createArrayNode();
            if (matchContext != null && !matchContext.isEmpty()) {
                ObjectNode sys = messages.addObject();
                sys.put("role", "system");
                sys.put("content", matchContext);
            }
            for (ChatMessage msg : history) {
                String role = msg.getType() == 1 ? "user" : "assistant";
                ObjectNode o = messages.addObject();
                o.put("role", role);
                o.put("content", msg.getContent() == null ? "" : msg.getContent());
            }
            ObjectNode userNode = messages.addObject();
            userNode.put("role", "user");
            userNode.put("content", currentMessage);

            String content = dashScopeCompatClient.completeMessages(messages, null);
            if (content != null && !content.isEmpty()) {
                return content;
            }
            return "AI 响应为空，请检查 DASHSCOPE_API_KEY 与百炼限流/额度";
        } catch (Exception e) {
            return "调用 AI 服务失败：" + e.getMessage();
        }
    }
}
