package com.sparklink.service;

import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;

/**
 * AI对话服务接口
 *
 * @author AI-Pick
 */
public interface ChatService {

    /**
     * AI对话
     *
     * @param userId  用户ID
     * @param request 对话请求
     * @return 对话响应
     */
    ChatResponse chat(Long userId, ChatRequest request);

    /**
     * 获取会话历史
     *
     * @param sessionId 会话ID
     * @return 历史消息
     */
    java.util.List<com.sparklink.entity.ChatMessage> getHistory(String sessionId);
}
