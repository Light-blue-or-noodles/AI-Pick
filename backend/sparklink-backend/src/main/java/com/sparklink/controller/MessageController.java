package com.sparklink.controller;

import com.sparklink.common.Result;
import com.sparklink.dto.SendMessageRequest;
import com.sparklink.entity.UserMessage;
import com.sparklink.service.MessageService;
import com.sparklink.vo.ChatMessageVO;
import com.sparklink.vo.ConversationVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 消息控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 消息列表（会话列表）
     */
    @GetMapping("/list")
    public Result<List<ConversationVO>> getConversationList(
            @RequestHeader("X-User-Id") Long userId) {
        List<ConversationVO> list = messageService.getConversationList(userId);
        return Result.success(list);
    }

    /**
     * 聊天详情
     */
    @GetMapping("/conversation/{id}")
    public Result<List<ChatMessageVO>> getConversationMessages(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        List<ChatMessageVO> list = messageService.getConversationMessages(id, userId, page, size);
        return Result.success(list);
    }

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<UserMessage> sendMessage(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SendMessageRequest request) {
        UserMessage message = messageService.sendMessage(userId, request);
        return Result.success("发送成功", message);
    }

    /**
     * 标记已读
     */
    @PostMapping("/read")
    public Result<Void> markAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam Long conversationId) {
        messageService.markAsRead(userId, conversationId);
        return Result.success("标记成功", null);
    }
}