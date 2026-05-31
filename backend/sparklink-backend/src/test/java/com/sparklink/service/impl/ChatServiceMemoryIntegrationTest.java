package com.sparklink.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparklink.ai.chat.ChatRecommendFallbackService;
import com.sparklink.ai.chat.ChatToolOrchestrator;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.entity.ChatMessage;
import com.sparklink.integration.DashScopeCompatClient;
import com.sparklink.mapper.ChatMessageMapper;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.service.MemoryFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceMemoryIntegrationTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ChatToolOrchestrator chatToolOrchestrator;

    @Mock
    private ChatRecommendFallbackService recommendFallbackService;

    @Mock
    private DashScopeCompatClient dashScopeCompatClient;

    @Mock
    private MemoryFacade memoryFacade;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(
                chatMessageMapper,
                chatToolOrchestrator,
                recommendFallbackService,
                new ObjectMapper(),
                dashScopeCompatClient,
                memoryFacade
        );
    }

    @Test
    void shouldRecallBeforeReplyAndEnqueueAfterReply() {
        Long userId = 10086L;
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-memory-1");
        request.setMessage("我想找附近羽毛球活动");
        when(chatMessageMapper.selectList(any())).thenReturn(List.of());
        when(chatToolOrchestrator.orchestrate(eq(userId), eq(request.getMessage()), any()))
                .thenReturn(new ChatToolOrchestrator.OrchestrationResult(false, null, List.of()));
        when(recommendFallbackService.recallFromMessage(request.getMessage())).thenReturn(List.of());
        when(memoryFacade.recallForPrompt(userId, request.getMessage())).thenReturn(null);
        when(memoryFacade.mergePrompt(any(), isNull(MemoryContext.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dashScopeCompatClient.completeMessages(any(), isNull())).thenReturn("这是 AI 回复");

        ChatResponse response = chatService.chat(userId, request);

        InOrder inOrder = inOrder(memoryFacade, dashScopeCompatClient);
        inOrder.verify(memoryFacade).recallForPrompt(userId, request.getMessage());
        inOrder.verify(memoryFacade).mergePrompt(any(), isNull(MemoryContext.class));
        inOrder.verify(dashScopeCompatClient).completeMessages(any(), isNull());
        inOrder.verify(memoryFacade).enqueueConversation(userId, request.getMessage(), "这是 AI 回复");
        verify(chatMessageMapper, org.mockito.Mockito.times(2)).insert(any(ChatMessage.class));
        assertEquals("session-memory-1", response.getSessionId());
        assertEquals("这是 AI 回复", response.getReply());
    }
}
