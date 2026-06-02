package com.sparklink.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparklink.ai.chat.BailianWebSearchClient;
import com.sparklink.ai.chat.ChatRecommendFallbackService;
import com.sparklink.ai.chat.ChatToolOrchestrator;
import com.sparklink.ai.chat.CitationBuilder;
import com.sparklink.ai.chat.SearchResultFilterService;
import com.sparklink.ai.chat.WebSearchPolicyService;
import com.sparklink.ai.chat.WebSearchProperties;
import com.sparklink.dto.ChatCitationItem;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.integration.DashScopeCompatClient;
import com.sparklink.mapper.ChatMessageMapper;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.service.MemoryFacade;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceWebSearchIntegrationTest {

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
    private WebSearchPolicyService webSearchPolicyService;

    @Mock
    private BailianWebSearchClient bailianWebSearchClient;

    @Mock
    private SearchResultFilterService searchResultFilterService;

    @Mock
    private CitationBuilder citationBuilder;

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
        chatService.setWebSearchPolicyService(webSearchPolicyService);
        chatService.setBailianWebSearchClient(bailianWebSearchClient);
        chatService.setSearchResultFilterService(searchResultFilterService);
        chatService.setCitationBuilder(citationBuilder);
        WebSearchProperties webSearchProperties = new WebSearchProperties();
        webSearchProperties.setMaxResults(3);
        chatService.setWebSearchProperties(webSearchProperties);
    }

    @Test
    void shouldUseWebSearchWhenModeOn() {
        Long userId = 1L;
        ChatRequest request = buildRequest("on", "今天北京天气");
        mockCommonFlow(userId, request);
        when(webSearchPolicyService.shouldSearch("on", "今天北京天气"))
                .thenReturn(WebSearchPolicyService.Decision.triggered("mode_on"));
        List<BailianWebSearchClient.RawResult> rawResults = List.of(
                new BailianWebSearchClient.RawResult("北京天气", "https://weather.com/bj", "晴天", "")
        );
        when(bailianWebSearchClient.search("今天北京天气", 3)).thenReturn(rawResults);
        List<SearchResultFilterService.FilteredItem> filteredItems = List.of(
                new SearchResultFilterService.FilteredItem("北京天气", "https://weather.com/bj", "weather.com", "晴天", "")
        );
        when(searchResultFilterService.filter(rawResults))
                .thenReturn(new SearchResultFilterService.FilteredResult(filteredItems, 2));
        when(citationBuilder.build(filteredItems))
                .thenReturn(List.of(new ChatCitationItem("北京天气", "https://weather.com/bj", "weather.com", "晴天", "")));

        ChatResponse response = chatService.chat(userId, request);

        verify(bailianWebSearchClient).search("今天北京天气", 3);
        ArgumentCaptor<ArrayNode> messagesCaptor = ArgumentCaptor.forClass(ArrayNode.class);
        verify(dashScopeCompatClient).completeMessages(messagesCaptor.capture(), isNull());
        ArrayNode messages = messagesCaptor.getValue();
        boolean hasLowPriorityReference = false;
        for (int i = 0; i < messages.size(); i++) {
            String role = messages.get(i).path("role").asText();
            String content = messages.get(i).path("content").asText();
            if ("system".equals(role) && content.contains("【联网参考-低优先级】") && content.contains("https://weather.com/bj")) {
                hasLowPriorityReference = true;
                break;
            }
        }
        assertTrue(hasLowPriorityReference);
        assertTrue(response.getSearchMeta().isTriggered());
        assertEquals("mode_on", response.getSearchMeta().getReason());
        assertEquals(2, response.getSearchMeta().getFilteredCount());
        assertEquals(1, response.getCitations().size());
    }

    @Test
    void shouldSkipWebSearchWhenModeOff() {
        Long userId = 2L;
        ChatRequest request = buildRequest("off", "今天北京天气");
        mockCommonFlow(userId, request);
        when(webSearchPolicyService.shouldSearch("off", "今天北京天气"))
                .thenReturn(WebSearchPolicyService.Decision.notTriggered("mode_off"));

        ChatResponse response = chatService.chat(userId, request);

        verify(bailianWebSearchClient, never()).search(anyString(), anyInt());
        assertFalse(response.getSearchMeta().isTriggered());
        assertEquals("mode_off", response.getSearchMeta().getReason());
        assertTrue(response.getCitations().isEmpty());
    }

    @Test
    void shouldFallbackToNormalReplyWhenSearchThrows() {
        Long userId = 3L;
        ChatRequest request = buildRequest("on", "今天北京天气");
        when(chatMessageMapper.selectList(any())).thenReturn(List.of());
        when(chatToolOrchestrator.orchestrate(eq(userId), eq(request.getMessage()), any()))
                .thenReturn(new ChatToolOrchestrator.OrchestrationResult(false, null, List.of()));
        when(memoryFacade.recallForPrompt(userId, request.getMessage())).thenReturn(null);
        when(webSearchPolicyService.shouldSearch("on", "今天北京天气"))
                .thenReturn(WebSearchPolicyService.Decision.triggered("mode_on"));
        when(bailianWebSearchClient.search("今天北京天气", 3))
                .thenThrow(new RuntimeException("timeout"));

        ChatResponse response = chatService.chat(userId, request);

        assertNotNull(response.getReply());
        assertTrue(response.getReply().contains("联网检索服务当前繁忙"));
        assertFalse(response.getSearchMeta().isTriggered());
        assertEquals("search_failed_fallback", response.getSearchMeta().getReason());
    }

    @Test
    void shouldUseWebSearchInAutoWhenKeywordMatched() {
        Long userId = 4L;
        ChatRequest request = buildRequest("auto", "今天上海新闻");
        mockCommonFlow(userId, request);
        when(webSearchPolicyService.shouldSearch("auto", "今天上海新闻"))
                .thenReturn(WebSearchPolicyService.Decision.triggered("auto_keyword"));
        List<BailianWebSearchClient.RawResult> rawResults = List.of(
                new BailianWebSearchClient.RawResult("上海新闻", "https://news.com/sh", "今日要闻", "")
        );
        when(bailianWebSearchClient.search("今天上海新闻", 3)).thenReturn(rawResults);
        List<SearchResultFilterService.FilteredItem> filteredItems = List.of(
                new SearchResultFilterService.FilteredItem("上海新闻", "https://news.com/sh", "news.com", "今日要闻", "")
        );
        when(searchResultFilterService.filter(rawResults))
                .thenReturn(new SearchResultFilterService.FilteredResult(filteredItems, 0));
        when(citationBuilder.build(filteredItems))
                .thenReturn(List.of(new ChatCitationItem("上海新闻", "https://news.com/sh", "news.com", "今日要闻", "")));

        ChatResponse response = chatService.chat(userId, request);

        verify(bailianWebSearchClient).search("今天上海新闻", 3);
        assertTrue(response.getSearchMeta().isTriggered());
        assertEquals("auto_keyword", response.getSearchMeta().getReason());
        assertEquals(1, response.getCitations().size());
    }

    @Test
    void shouldSkipWebSearchInAutoWhenKeywordNotMatched() {
        Long userId = 5L;
        ChatRequest request = buildRequest("auto", "你好呀");
        mockCommonFlow(userId, request);
        when(webSearchPolicyService.shouldSearch("auto", "你好呀"))
                .thenReturn(WebSearchPolicyService.Decision.notTriggered("auto_skip"));

        ChatResponse response = chatService.chat(userId, request);

        verify(bailianWebSearchClient, never()).search(anyString(), anyInt());
        assertFalse(response.getSearchMeta().isTriggered());
        assertEquals("auto_skip", response.getSearchMeta().getReason());
        assertTrue(response.getCitations().isEmpty());
    }

    @Test
    void shouldSkipWebSearchWhenConfigDisabled() {
        Long userId = 6L;
        ChatRequest request = buildRequest("on", "今天广州天气");
        mockCommonFlow(userId, request);
        when(webSearchPolicyService.shouldSearch("on", "今天广州天气"))
                .thenReturn(WebSearchPolicyService.Decision.notTriggered("config_disabled"));

        ChatResponse response = chatService.chat(userId, request);

        verify(bailianWebSearchClient, never()).search(anyString(), anyInt());
        assertFalse(response.getSearchMeta().isTriggered());
        assertEquals("config_disabled", response.getSearchMeta().getReason());
    }

    @Test
    void shouldSkipWebSearchWhenToolsHandledReply() {
        Long userId = 7L;
        ChatRequest request = buildRequest("on", "今天深圳天气");
        when(chatMessageMapper.selectList(any())).thenReturn(List.of());
        when(memoryFacade.recallForPrompt(userId, request.getMessage())).thenReturn(null);
        when(chatToolOrchestrator.orchestrate(eq(userId), eq(request.getMessage()), any()))
                .thenReturn(new ChatToolOrchestrator.OrchestrationResult(true, "工具直接回复", List.of()));
        when(recommendFallbackService.recallFromMessage(request.getMessage())).thenReturn(List.of());

        ChatResponse response = chatService.chat(userId, request);

        verify(webSearchPolicyService, never()).shouldSearch(anyString(), anyString());
        verify(bailianWebSearchClient, never()).search(anyString(), anyInt());
        assertEquals("工具直接回复", response.getReply());
        assertFalse(response.getSearchMeta().isTriggered());
        assertEquals("not_evaluated", response.getSearchMeta().getReason());
    }

    @Test
    void shouldAskForCityWhenWeatherQueryHasNoLocation() {
        Long userId = 8L;
        ChatRequest request = buildRequest("on", "今天的天气预报");

        ChatResponse response = chatService.chat(userId, request);

        verify(webSearchPolicyService, never()).shouldSearch(anyString(), anyString());
        verify(bailianWebSearchClient, never()).search(anyString(), anyInt());
        assertTrue(response.getReply().contains("告诉我你要查询天气的城市"));
        assertFalse(response.getSearchMeta().isTriggered());
        assertEquals("need_location", response.getSearchMeta().getReason());
    }

    private ChatRequest buildRequest(String mode, String message) {
        ChatRequest request = new ChatRequest();
        request.setSessionId("session-web-search");
        request.setWebSearchMode(mode);
        request.setMessage(message);
        return request;
    }

    private void mockCommonFlow(Long userId, ChatRequest request) {
        when(chatMessageMapper.selectList(any())).thenReturn(List.of());
        when(chatToolOrchestrator.orchestrate(eq(userId), eq(request.getMessage()), any()))
                .thenReturn(new ChatToolOrchestrator.OrchestrationResult(false, null, List.of()));
        when(recommendFallbackService.recallFromMessage(request.getMessage())).thenReturn(List.of());
        when(memoryFacade.recallForPrompt(userId, request.getMessage())).thenReturn(null);
        when(memoryFacade.mergePrompt(any(), isNull(MemoryContext.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(dashScopeCompatClient.completeMessages(any(), isNull())).thenReturn("这是 AI 回复");
    }
}
