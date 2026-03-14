package com.aipick.service;

import com.aipick.dto.SendMessageRequest;
import com.aipick.entity.Conversation;
import com.aipick.entity.UserMessage;
import com.aipick.vo.ChatMessageVO;
import com.aipick.vo.ConversationVO;

import java.util.List;

/**
 * 消息服务接口
 *
 * @author AI-Pick
 */
public interface MessageService {

    /**
     * 获取消息列表（会话列表）
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ConversationVO> getConversationList(Long userId);

    /**
     * 获取聊天详情
     *
     * @param conversationId 会话ID
     * @param userId         当前用户ID
     * @param page           页码
     * @param size           每页大小
     * @return 消息列表
     */
    List<ChatMessageVO> getConversationMessages(Long conversationId, Long userId, Integer page, Integer size);

    /**
     * 发送消息
     *
     * @param userId   发送者ID
     * @param request  发送请求
     * @return 消息
     */
    UserMessage sendMessage(Long userId, SendMessageRequest request);

    /**
     * 标记已读
     *
     * @param userId         用户ID
     * @param conversationId 会话ID
     */
    void markAsRead(Long userId, Long conversationId);
}