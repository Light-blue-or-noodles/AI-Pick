package com.aipick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aipick.common.BusinessException;
import com.aipick.dto.SendMessageRequest;
import com.aipick.entity.Conversation;
import com.aipick.entity.User;
import com.aipick.entity.UserMessage;
import com.aipick.mapper.ConversationMapper;
import com.aipick.mapper.UserMessageMapper;
import com.aipick.mapper.UserMapper;
import com.aipick.service.MessageService;
import com.aipick.vo.ChatMessageVO;
import com.aipick.vo.ConversationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息服务实现
 *
 * @author AI-Pick
 */
@Service
public class MessageServiceImpl implements MessageService {

    private final ConversationMapper conversationMapper;
    private final UserMessageMapper userMessageMapper;
    private final UserMapper userMapper;

    public MessageServiceImpl(ConversationMapper conversationMapper, UserMessageMapper userMessageMapper, UserMapper userMapper) {
        this.conversationMapper = conversationMapper;
        this.userMessageMapper = userMessageMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<ConversationVO> getConversationList(Long userId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId1, userId)
                .or()
                .eq(Conversation::getUserId2, userId)
                .orderByDesc(Conversation::getUpdateTime);
        
        List<Conversation> conversations = conversationMapper.selectList(wrapper);
        
        List<ConversationVO> result = new ArrayList<>();
        for (Conversation conversation : conversations) {
            ConversationVO vo = new ConversationVO();
            vo.setConversationId(conversation.getId());
            
            // 获取对方用户信息
            Long otherUserId = conversation.getUserId1().equals(userId) 
                    ? conversation.getUserId2() 
                    : conversation.getUserId1();
            vo.setUserId(otherUserId);
            
            User otherUser = userMapper.selectById(otherUserId);
            if (otherUser != null) {
                vo.setNickname(otherUser.getNickname());
                vo.setAvatar(otherUser.getAvatar());
            }
            
            // 获取最后一条消息
            if (conversation.getLastMessageId() != null) {
                UserMessage lastMessage = userMessageMapper.selectById(conversation.getLastMessageId());
                if (lastMessage != null) {
                    vo.setLastMessage(lastMessage.getContent());
                    vo.setLastMessageTime(lastMessage.getCreateTime());
                }
            }
            
            // 未读数量
            if (conversation.getUserId1().equals(userId)) {
                vo.setUnreadCount(conversation.getUnreadCount1());
            } else {
                vo.setUnreadCount(conversation.getUnreadCount2());
            }
            
            result.add(vo);
        }
        
        return result;
    }

    @Override
    public List<ChatMessageVO> getConversationMessages(Long conversationId, Long userId, Integer page, Integer size) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        
        // 验证用户是否是会话参与者
        if (!conversation.getUserId1().equals(userId) && !conversation.getUserId2().equals(userId)) {
            throw new BusinessException("无权查看此会话");
        }
        
        Page<UserMessage> pageParam = new Page<>(page != null ? page : 1, size != null ? size : 20);
        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMessage::getConversationId, conversationId)
                .orderByAsc(UserMessage::getCreateTime);
        
        Page<UserMessage> messagePage = userMessageMapper.selectPage(pageParam, wrapper);
        
        List<ChatMessageVO> result = new ArrayList<>();
        for (UserMessage message : messagePage.getRecords()) {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(message.getId());
            vo.setSenderId(message.getSenderId());
            vo.setReceiverId(message.getReceiverId());
            vo.setType(message.getType());
            vo.setContent(message.getContent());
            vo.setLatitude(message.getLatitude());
            vo.setLongitude(message.getLongitude());
            vo.setIsRead(message.getIsRead());
            vo.setCreateTime(message.getCreateTime());
            
            // 发送者头像
            User sender = userMapper.selectById(message.getSenderId());
            if (sender != null) {
                vo.setSenderAvatar(sender.getAvatar());
            }
            
            result.add(vo);
        }
        
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserMessage sendMessage(Long userId, SendMessageRequest request) {
        Long receiverId = request.getReceiverId();
        
        // 查找或创建会话
        Conversation conversation = findOrCreateConversation(userId, receiverId);
        
        // 创建消息
        UserMessage message = new UserMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(userId);
        message.setReceiverId(receiverId);
        message.setType(request.getType());
        message.setContent(request.getContent());
        message.setLatitude(request.getLatitude());
        message.setLongitude(request.getLongitude());
        message.setIsRead(false);
        
        userMessageMapper.insert(message);
        
        // 更新会话的最后消息和未读数
        conversation.setLastMessageId(message.getId());
        if (conversation.getUserId1().equals(receiverId)) {
            conversation.setUnreadCount1((conversation.getUnreadCount1() != null ? conversation.getUnreadCount1() : 0) + 1);
        } else {
            conversation.setUnreadCount2((conversation.getUnreadCount2() != null ? conversation.getUnreadCount2() : 0) + 1);
        }
        conversationMapper.updateById(conversation);
        
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long userId, Long conversationId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException("会话不存在");
        }
        
        // 验证用户是否是会话参与者
        if (!conversation.getUserId1().equals(userId) && !conversation.getUserId2().equals(userId)) {
            throw new BusinessException("无权操作此会话");
        }
        
        // 重置未读数
        if (conversation.getUserId1().equals(userId)) {
            conversation.setUnreadCount1(0);
        } else {
            conversation.setUnreadCount2(0);
        }
        conversationMapper.updateById(conversation);
        
        // 标记消息为已读
        LambdaQueryWrapper<UserMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMessage::getConversationId, conversationId)
                .eq(UserMessage::getReceiverId, userId)
                .eq(UserMessage::getIsRead, false);
        
        List<UserMessage> unreadMessages = userMessageMapper.selectList(wrapper);
        for (UserMessage message : unreadMessages) {
            message.setIsRead(true);
            userMessageMapper.updateById(message);
        }
    }

    /**
     * 查找或创建会话
     */
    private Conversation findOrCreateConversation(Long userId1, Long userId2) {
        // 确保 userId1 < userId2 以便统一查询
        Long minId = Math.min(userId1, userId2);
        Long maxId = Math.max(userId1, userId2);
        
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUserId1, minId)
                .eq(Conversation::getUserId2, maxId);
        Conversation conversation = conversationMapper.selectOne(wrapper);
        
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setUserId1(minId);
            conversation.setUserId2(maxId);
            conversation.setUnreadCount1(0);
            conversation.setUnreadCount2(0);
            conversationMapper.insert(conversation);
        }
        
        return conversation;
    }
}