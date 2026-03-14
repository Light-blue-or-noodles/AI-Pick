package com.aipick.service.impl;

import cn.hutool.core.util.IdUtil;
import com.aipick.common.AiConstants;
import com.aipick.dto.ChatRequest;
import com.aipick.dto.ChatResponse;
import com.aipick.entity.ChatMessage;
import com.aipick.mapper.ChatMessageMapper;
import com.aipick.service.ChatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 对话服务实现（阿里云百炼）
 * 支持多轮对话上下文，读取历史消息构建完整对话场景
 *
 * @author AI-Pick
 */
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatMessageMapper chatMessageMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse chat(Long userId, ChatRequest request) {
        // 获取或生成会话 ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = IdUtil.fastSimpleUUID();
        }

        // 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setType(1);
        userMessage.setContent(request.getMessage());
        chatMessageMapper.insert(userMessage);

        // 读取历史消息，构建多轮对话上下文
        List<ChatMessage> history = getHistoryMessages(sessionId, userId);

        // 调用阿里云百炼 API（带上下文）
        String reply = generateReplyWithHistory(request.getMessage(), history);

        // 保存 AI 消息
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setUserId(userId);
        aiMessage.setType(2);
        aiMessage.setContent(reply);
        chatMessageMapper.insert(aiMessage);

        return new ChatResponse(sessionId, reply);
    }

    @Override
    public List<ChatMessage> getHistory(String sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper);
    }

    /**
     * 获取历史消息（用于构建多轮对话上下文）
     * @param sessionId 会话 ID
     * @param userId 用户 ID
     * @return 历史消息列表（按时间正序）
     */
    private List<ChatMessage> getHistoryMessages(String sessionId, Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + AiConstants.DEFAULT_HISTORY_MESSAGE_COUNT);
        List<ChatMessage> history = chatMessageMapper.selectList(wrapper);
        // 反转为正序（旧消息在前，新消息在后）
        Collections.reverse(history);
        return history;
    }

    /**
     * 调用阿里云百炼 API 生成回复（带历史上下文）
     * @param currentMessage 当前消息
     * @param history 历史消息列表
     * @return AI 回复内容
     */
    private String generateReplyWithHistory(String currentMessage, List<ChatMessage> history) {
        if (currentMessage == null || currentMessage.trim().isEmpty()) {
            return "请告诉我你想聊些什么？";
        }

        if (apiKey == null || apiKey.isEmpty()) {
            return "AI 服务未配置 API Key，请设置 DASHSCOPE_API_KEY 环境变量或配置 spring.ai.dashscope.api-key";
        }

        try {
            // 构建消息列表（包含历史上下文）
            List<String> messages = new ArrayList<>();
            
            // 添加历史消息
            for (ChatMessage msg : history) {
                String role = msg.getType() == 1 ? "user" : "assistant";
                String content = msg.getContent()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                messages.add(String.format("{\"role\":\"%s\",\"content\":\"%s\"}", role, content));
            }
            
            // 添加当前消息
            String currentContent = currentMessage
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            messages.add(String.format("{\"role\":\"user\",\"content\":\"%s\"}", currentContent));
            
            // 构建请求体
            String requestBody = String.format(
                    "{\"model\":\"qwen-max\",\"input\":{\"messages\":[%s]}}",
                    String.join(",", messages)
            );

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
                    entity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode output = root.path("output");
                // 兼容两种响应格式：新版 output.text，旧版 output.choices[0].message.content
                String content = output.path("text").asText();
                if (content.isEmpty()) {
                    JsonNode choices = output.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode firstChoice = choices.get(0);
                        JsonNode msg = firstChoice.path("message");
                        content = msg.path("content").asText();
                    }
                }
                if (!content.isEmpty()) {
                    return content;
                }
            }

            return "AI 响应格式异常";
        } catch (Exception e) {
            return "调用 AI 服务失败：" + e.getMessage();
        }
    }
}
