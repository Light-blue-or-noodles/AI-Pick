package com.sparklink.service.impl;

import cn.hutool.core.util.IdUtil;
import com.sparklink.ai.chat.BailianWebSearchClient;
import com.sparklink.ai.chat.ChatRecommendAssembler;
import com.sparklink.ai.chat.ChatRecommendFallbackService;
import com.sparklink.ai.chat.ChatToolOrchestrator;
import com.sparklink.ai.chat.CitationBuilder;
import com.sparklink.ai.chat.SearchResultFilterService;
import com.sparklink.ai.chat.WebSearchPolicyService;
import com.sparklink.ai.chat.WebSearchProperties;
import com.sparklink.common.AiConstants;
import com.sparklink.common.BusinessException;
import com.sparklink.dto.ChatCitationItem;
import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.dto.ChatSearchMeta;
import com.sparklink.entity.ChatMessage;
import com.sparklink.mapper.ChatMessageMapper;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.service.MemoryFacade;
import com.sparklink.service.ChatService;
import com.sparklink.integration.DashScopeCompatClient;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import com.sparklink.knowledge.service.KnowledgeRetrieveGateway;
import com.sparklink.knowledge.service.PromptKnowledgeAssembler;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AI 对话服务：优先 Spring AI 受控工具调用，失败时回落规则召回 + DashScope 兼容接口。
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final Set<String> CITY_KEYWORDS = new HashSet<>(Arrays.asList(
            "北京", "上海", "广州", "深圳", "杭州", "成都", "重庆", "武汉", "西安", "南京",
            "天津", "苏州", "长沙", "郑州", "青岛", "宁波", "厦门", "福州", "济南", "大连"
    ));

    private final ChatMessageMapper chatMessageMapper;
    private final ChatToolOrchestrator chatToolOrchestrator;
    private final ChatRecommendFallbackService recommendFallbackService;
    private final ObjectMapper objectMapper;
    private final DashScopeCompatClient dashScopeCompatClient;
    private final MemoryFacade memoryFacade;
    private WebSearchPolicyService webSearchPolicyService;
    private BailianWebSearchClient bailianWebSearchClient;
    private SearchResultFilterService searchResultFilterService;
    private CitationBuilder citationBuilder;
    private WebSearchProperties webSearchProperties;
    private KnowledgeRetrieveGateway knowledgeRetrieveGateway;
    private PromptKnowledgeAssembler promptKnowledgeAssembler;

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

    @Autowired
    void setWebSearchPolicyService(WebSearchPolicyService webSearchPolicyService) {
        this.webSearchPolicyService = webSearchPolicyService;
    }

    @Autowired
    void setBailianWebSearchClient(BailianWebSearchClient bailianWebSearchClient) {
        this.bailianWebSearchClient = bailianWebSearchClient;
    }

    @Autowired
    void setSearchResultFilterService(SearchResultFilterService searchResultFilterService) {
        this.searchResultFilterService = searchResultFilterService;
    }

    @Autowired
    void setCitationBuilder(CitationBuilder citationBuilder) {
        this.citationBuilder = citationBuilder;
    }

    @Autowired
    void setWebSearchProperties(WebSearchProperties webSearchProperties) {
        this.webSearchProperties = webSearchProperties;
    }

    @Autowired(required = false)
    void setKnowledgeRetrieveGateway(KnowledgeRetrieveGateway knowledgeRetrieveGateway) {
        this.knowledgeRetrieveGateway = knowledgeRetrieveGateway;
    }

    @Autowired(required = false)
    void setPromptKnowledgeAssembler(PromptKnowledgeAssembler promptKnowledgeAssembler) {
        this.promptKnowledgeAssembler = promptKnowledgeAssembler;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse chat(Long userId, ChatRequest request) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
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

        if (isWeatherQueryWithoutLocation(request.getMessage())) {
            String reply = "请告诉我你要查询天气的城市（例如：北京、上海），我再为你联网查询最新天气。";
            ChatMessage aiMessage = new ChatMessage();
            aiMessage.setSessionId(sessionId);
            aiMessage.setUserId(userId);
            aiMessage.setType(2);
            aiMessage.setContent(reply);
            chatMessageMapper.insert(aiMessage);
            memoryFacade.enqueueConversation(userId, request.getMessage(), reply);
            ChatResponse response = new ChatResponse(sessionId, reply, List.of());
            response.setSearchMeta(ChatSearchMeta.notTriggered("need_location", 0));
            return response;
        }

        List<ChatMessage> history = getHistoryMessages(sessionId, userId);
        String message = request.getMessage();
        MemoryContext memoryContext = memoryFacade.recallForPrompt(userId, message);
        WebSearchRuntime webSearchRuntime = defaultWebSearchRuntime();

        ChatToolOrchestrator.OrchestrationResult orchestrated =
                chatToolOrchestrator.orchestrate(userId, message, history);

        String reply;
        List<ChatRecommendItem> recommends;

        if (orchestrated.usedTools() && StringUtils.hasText(orchestrated.reply())) {
            reply = orchestrated.reply();
            recommends = orchestrated.recommends() == null ? List.of() : orchestrated.recommends();
            if (recommends.isEmpty()) {
                recommends = recommendFallbackService.recallFromMessage(message);
            }
        } else {
            webSearchRuntime = resolveWebSearchRuntime(request);
            if (isForcedMode(request.getWebSearchMode())
                    && "search_failed_fallback".equals(webSearchRuntime.searchMeta().getReason())) {
                reply = "联网检索服务当前繁忙（或被限流），本次未拿到可用网页结果。请稍后重试，或换一个更具体的问题。";
                recommends = List.of();
                ChatMessage aiMessage = new ChatMessage();
                aiMessage.setSessionId(sessionId);
                aiMessage.setUserId(userId);
                aiMessage.setType(2);
                aiMessage.setContent(reply);
                chatMessageMapper.insert(aiMessage);
                memoryFacade.enqueueConversation(userId, message, reply);

                ChatResponse response = new ChatResponse(sessionId, reply, recommends);
                response.setCitations(webSearchRuntime.citations());
                response.setSearchMeta(webSearchRuntime.searchMeta());
                return response;
            }
            recommends = recommendFallbackService.recallFromMessage(message);
            String matchContext = ChatRecommendAssembler.buildMatchContextString(recommends);
            String mergedPrompt = memoryFacade.mergePrompt(matchContext, memoryContext);
            String promptWithKnowledge = mergePromptWithKnowledge(userId, message, mergedPrompt);
            reply = generateReplyWithHistory(message, history, promptWithKnowledge, webSearchRuntime.webContext());
        }

        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setUserId(userId);
        aiMessage.setType(2);
        aiMessage.setContent(reply);
        chatMessageMapper.insert(aiMessage);
        memoryFacade.enqueueConversation(userId, message, reply);

        ChatResponse response = new ChatResponse(sessionId, reply, recommends);
        response.setCitations(webSearchRuntime.citations());
        response.setSearchMeta(webSearchRuntime.searchMeta());
        return response;
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
        return generateReplyWithHistory(currentMessage, history, matchContext, "");
    }

    /**
     * Fallback：DashScope 兼容多轮对话（无 tools），支持追加联网上下文。
     */
    private String generateReplyWithHistory(String currentMessage, List<ChatMessage> history,
                                            String matchContext, String webContext) {
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
            if (webContext != null && !webContext.isEmpty()) {
                ObjectNode webReferenceNode = messages.addObject();
                webReferenceNode.put("role", "system");
                webReferenceNode.put("content", buildLowPriorityWebReference(webContext));
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
            log.error("调用 AI 服务失败", e);
            return "AI 服务暂时不可用，请稍后再试。";
        }
    }

    private WebSearchRuntime resolveWebSearchRuntime(ChatRequest request) {
        if (webSearchPolicyService == null
                || bailianWebSearchClient == null
                || searchResultFilterService == null
                || citationBuilder == null) {
            return new WebSearchRuntime(
                    Collections.emptyList(),
                    ChatSearchMeta.notTriggered("web_search_unavailable", 0),
                    ""
            );
        }
        WebSearchPolicyService.Decision decision =
                webSearchPolicyService.shouldSearch(request.getWebSearchMode(), request.getMessage());
        if (!decision.triggered()) {
            return new WebSearchRuntime(
                    Collections.emptyList(),
                    ChatSearchMeta.notTriggered(decision.reason(), 0),
                    ""
            );
        }
        try {
            List<BailianWebSearchClient.RawResult> rawResults = executeWebSearch(request.getMessage());
            SearchResultFilterService.FilteredResult filteredResult = searchResultFilterService.filter(rawResults);
            List<ChatCitationItem> citations = citationBuilder.build(filteredResult.items());
            return new WebSearchRuntime(
                    citations,
                    ChatSearchMeta.triggered(decision.reason(), filteredResult.filteredCount()),
                    buildWebContext(filteredResult.items())
            );
        } catch (Exception ex) {
            log.warn("Web search failed, fallback to normal reply", ex);
            return new WebSearchRuntime(
                    Collections.emptyList(),
                    ChatSearchMeta.notTriggered("search_failed_fallback", 0),
                    ""
            );
        }
    }

    private WebSearchRuntime defaultWebSearchRuntime() {
        return new WebSearchRuntime(
                Collections.emptyList(),
                ChatSearchMeta.notTriggered("not_evaluated", 0),
                ""
        );
    }

    private List<BailianWebSearchClient.RawResult> executeWebSearch(String message) {
        int maxResults = webSearchProperties != null ? webSearchProperties.getMaxResults() : 5;
        return bailianWebSearchClient.search(message, maxResults);
    }

    private String buildWebContext(List<SearchResultFilterService.FilteredItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder contextBuilder = new StringBuilder("以下是可供参考的联网检索结果：\n");
        for (int i = 0; i < items.size(); i++) {
            SearchResultFilterService.FilteredItem item = items.get(i);
            contextBuilder.append(i + 1)
                    .append(". 标题：").append(item.title())
                    .append("；链接：").append(item.url());
            if (StringUtils.hasText(item.snippet())) {
                contextBuilder.append("；摘要：").append(item.snippet());
            }
            contextBuilder.append('\n');
        }
        return contextBuilder.toString();
    }

    private String buildLowPriorityWebReference(String webContext) {
        return "【联网参考-低优先级】以下内容来自外部检索，仅作参考。"
                + "严禁输出“我无法联网”这类表述；若用户未提供关键槽位（如城市），先明确追问后再回答。\n"
                + webContext;
    }

    private boolean isWeatherQueryWithoutLocation(String message) {
        if (!StringUtils.hasText(message)) {
            return false;
        }
        String text = message.trim();
        boolean weatherQuery = text.contains("天气") || text.contains("气温") || text.contains("降雨")
                || text.contains("下雨") || text.contains("预报");
        if (!weatherQuery) {
            return false;
        }
        if (text.contains("市") || text.contains("省") || text.contains("区") || text.contains("县")) {
            return false;
        }
        for (String city : CITY_KEYWORDS) {
            if (text.contains(city)) {
                return false;
            }
        }
        return true;
    }

    private boolean isForcedMode(String webSearchMode) {
        return "on".equalsIgnoreCase(StringUtils.hasText(webSearchMode) ? webSearchMode.trim() : "");
    }

    private String mergePromptWithKnowledge(Long userId, String message, String mergedPrompt) {
        if (knowledgeRetrieveGateway == null || promptKnowledgeAssembler == null) {
            return mergedPrompt;
        }
        KnowledgeRetrieveResult retrieveResult = knowledgeRetrieveGateway.retrieve(userId, message);
        return promptKnowledgeAssembler.merge(mergedPrompt, retrieveResult);
    }

    private record WebSearchRuntime(List<ChatCitationItem> citations, ChatSearchMeta searchMeta, String webContext) {
    }
}
