package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.dto.ChatRequest;
import com.aipick.dto.ChatResponse;
import com.aipick.entity.ChatMessage;
import com.aipick.service.ChatService;
import com.aipick.util.PromptSanitizer;
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

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * AI对话
     */
    @PostMapping
    public Result<ChatResponse> chat(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChatRequest request) {
        request.setMessage(PromptSanitizer.sanitize(request.getMessage()));
        ChatResponse response = chatService.chat(userId, request);
        return Result.success(response);
    }

    /**
     * 获取 AI 会话历史（sessionId 为空时返回空列表，避免 500）
     */
    @GetMapping("/ai/history/{sessionId}")
    public Result<List<ChatMessage>> getHistory(@PathVariable(required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Result.success(List.of());
        }
        List<ChatMessage> history = chatService.getHistory(sessionId);
        return Result.success(history);
    }
}
