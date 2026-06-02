package com.sparklink.controller;

import com.sparklink.common.Result;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.entity.ChatMessage;
import com.sparklink.service.ChatService;
import com.sparklink.util.PromptSanitizer;
import com.sparklink.util.RequestUserResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI对话控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final RequestUserResolver requestUserResolver;

    public ChatController(ChatService chatService, RequestUserResolver requestUserResolver) {
        this.chatService = chatService;
        this.requestUserResolver = requestUserResolver;
    }

    /**
     * AI对话
     */
    @PostMapping
    public Result<ChatResponse> chat(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ChatRequest request) {
        request.setMessage(PromptSanitizer.sanitize(request.getMessage()));
        Long resolvedUserId = requestUserResolver.requireUserId(userId);
        ChatResponse response = chatService.chat(resolvedUserId, request);
        return Result.success(response);
    }

    /**
     * 获取 AI 会话历史（sessionId 为空时返回空列表，避免 500）
     */
    @GetMapping("/ai/history/{sessionId}")
    public Result<List<ChatMessage>> getHistory(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable(required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Result.success(List.of());
        }
        Long resolvedUserId = requestUserResolver.resolveUserId(userId);
        if (resolvedUserId == null) {
            return Result.success(List.of());
        }
        List<ChatMessage> history = chatService.getHistory(sessionId, resolvedUserId);
        return Result.success(history);
    }
}
