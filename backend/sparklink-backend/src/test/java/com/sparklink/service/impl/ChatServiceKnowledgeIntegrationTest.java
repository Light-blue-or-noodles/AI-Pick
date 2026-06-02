package com.sparklink.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparklink.ai.chat.ChatRecommendFallbackService;
import com.sparklink.ai.chat.ChatToolOrchestrator;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.integration.DashScopeCompatClient;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import com.sparklink.knowledge.service.KnowledgeRetrieveGateway;
import com.sparklink.knowledge.service.PromptKnowledgeAssembler;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceKnowledgeIntegrationTest {

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

    @Mock
    private KnowledgeRetrieveGateway knowledgeRetrieveGateway;

    @Mock
    private PromptKnowledgeAssembler promptKnowledgeAssembler;

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
        chatService.setKnowledgeRetrieveGateway(knowledgeRetrieveGateway);
        chatService.setPromptKnowledgeAssembler(promptKnowledgeAssembler);
    }

    @Test
    void shouldRetrieveAndMergeKnowledgeInFallbackBranch() {
        Long userId = 10086L;
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-knowledge-1");
        request.setMessage("我想了解羽毛球活动规则");
        request.setWebSearchMode("off");
        KnowledgeRetrieveResult retrieveResult = new KnowledgeRetrieveResult();

        when(chatMessageMapper.selectList(any())).thenReturn(List.of());
        when(chatToolOrchestrator.orchestrate(eq(userId), eq(request.getMessage()), any()))
                .thenReturn(new ChatToolOrchestrator.OrchestrationResult(false, null, List.of()));
        when(recommendFallbackService.recallFromMessage(request.getMessage())).thenReturn(List.of());
        when(memoryFacade.recallForPrompt(userId, request.getMessage())).thenReturn(null);
        when(memoryFacade.mergePrompt(any(), isNull(MemoryContext.class))).thenReturn("memory-merged");
        when(knowledgeRetrieveGateway.retrieve(userId, request.getMessage())).thenReturn(retrieveResult);
        when(promptKnowledgeAssembler.merge("memory-merged", retrieveResult)).thenReturn("knowledge-merged");
        when(dashScopeCompatClient.completeMessages(any(), isNull())).thenReturn("这是 AI 回复");

        ChatResponse response = chatService.chat(userId, request);

        InOrder inOrder = inOrder(memoryFacade, knowledgeRetrieveGateway, promptKnowledgeAssembler, dashScopeCompatClient);
        inOrder.verify(memoryFacade).recallForPrompt(userId, request.getMessage());
        inOrder.verify(memoryFacade).mergePrompt(any(), isNull(MemoryContext.class));
        inOrder.verify(knowledgeRetrieveGateway).retrieve(userId, request.getMessage());
        inOrder.verify(promptKnowledgeAssembler).merge("memory-merged", retrieveResult);
        inOrder.verify(dashScopeCompatClient).completeMessages(any(), isNull());
        verify(memoryFacade).enqueueConversation(userId, request.getMessage(), "这是 AI 回复");
        assertEquals("这是 AI 回复", response.getReply());
    }

    @Test
    void shouldNotRetrieveKnowledgeWhenToolsHandledReply() {
        Long userId = 10010L;
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-knowledge-2");
        request.setMessage("工具分支测试");

        when(chatMessageMapper.selectList(any())).thenReturn(List.of());
        when(memoryFacade.recallForPrompt(userId, request.getMessage())).thenReturn(null);
        when(chatToolOrchestrator.orchestrate(eq(userId), eq(request.getMessage()), any()))
                .thenReturn(new ChatToolOrchestrator.OrchestrationResult(true, "工具已回复", List.of()));
        when(recommendFallbackService.recallFromMessage(request.getMessage())).thenReturn(List.of());

        ChatResponse response = chatService.chat(userId, request);

        verify(knowledgeRetrieveGateway, never()).retrieve(any(), any());
        verify(promptKnowledgeAssembler, never()).merge(any(), any());
        assertEquals("工具已回复", response.getReply());
    }

    @Test
    void shouldContinueWhenGatewayReturnsFallbackResult() {
        Long userId = 10011L;
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-knowledge-3");
        request.setMessage("知识降级场景");
        request.setWebSearchMode("off");
        KnowledgeRetrieveResult fallbackResult = new KnowledgeRetrieveResult();
        fallbackResult.setFallback(true);

        when(chatMessageMapper.selectList(any())).thenReturn(List.of());
        when(chatToolOrchestrator.orchestrate(eq(userId), eq(request.getMessage()), any()))
                .thenReturn(new ChatToolOrchestrator.OrchestrationResult(false, null, List.of()));
        when(recommendFallbackService.recallFromMessage(request.getMessage())).thenReturn(List.of());
        when(memoryFacade.recallForPrompt(userId, request.getMessage())).thenReturn(null);
        when(memoryFacade.mergePrompt(any(), isNull(MemoryContext.class))).thenReturn("memory-merged");
        when(knowledgeRetrieveGateway.retrieve(userId, request.getMessage())).thenReturn(fallbackResult);
        when(promptKnowledgeAssembler.merge("memory-merged", fallbackResult)).thenReturn("memory-merged");
        when(dashScopeCompatClient.completeMessages(any(), isNull())).thenReturn("正常回复");

        ChatResponse response = chatService.chat(userId, request);

        verify(knowledgeRetrieveGateway).retrieve(userId, request.getMessage());
        verify(promptKnowledgeAssembler).merge("memory-merged", fallbackResult);
        assertEquals("正常回复", response.getReply());
    }

    @Test
    void shouldContinueWhenGatewayReturnsNullResult() {
        Long userId = 10012L;
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-knowledge-4");
        request.setMessage("知识空结果场景");
        request.setWebSearchMode("off");

        when(chatMessageMapper.selectList(any())).thenReturn(List.of());
        when(chatToolOrchestrator.orchestrate(eq(userId), eq(request.getMessage()), any()))
                .thenReturn(new ChatToolOrchestrator.OrchestrationResult(false, null, List.of()));
        when(recommendFallbackService.recallFromMessage(request.getMessage())).thenReturn(List.of());
        when(memoryFacade.recallForPrompt(userId, request.getMessage())).thenReturn(null);
        when(memoryFacade.mergePrompt(any(), isNull(MemoryContext.class))).thenReturn("memory-merged");
        when(knowledgeRetrieveGateway.retrieve(userId, request.getMessage())).thenReturn(null);
        when(promptKnowledgeAssembler.merge("memory-merged", null)).thenReturn("memory-merged");
        when(dashScopeCompatClient.completeMessages(any(), isNull())).thenReturn("正常回复");

        ChatResponse response = chatService.chat(userId, request);

        verify(knowledgeRetrieveGateway).retrieve(userId, request.getMessage());
        verify(promptKnowledgeAssembler).merge("memory-merged", null);
        assertEquals("正常回复", response.getReply());
    }
}
