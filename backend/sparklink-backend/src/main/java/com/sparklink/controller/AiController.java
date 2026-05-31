package com.sparklink.controller;

import com.sparklink.common.Result;
import com.sparklink.dto.AiRecommendRequest;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.dto.NaturalLanguageSearchRequest;
import com.sparklink.service.AiService;
import com.sparklink.service.ChatService;
import com.sparklink.service.NaturalLanguageSearchService;
import com.sparklink.util.PromptSanitizer;
import com.sparklink.vo.AiRecommendVO;
import com.sparklink.vo.NaturalLanguageSearchVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 能力控制器（智能推荐等）
 *
 * @author AI-Pick
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final ChatService chatService;
    private final NaturalLanguageSearchService naturalLanguageSearchService;

    public AiController(AiService aiService,
                        ChatService chatService,
                        NaturalLanguageSearchService naturalLanguageSearchService) {
        this.aiService = aiService;
        this.chatService = chatService;
        this.naturalLanguageSearchService = naturalLanguageSearchService;
    }

    /**
     * AI 智能推荐：基于用户兴趣、行为、位置推荐搭子与活动
     */
    @PostMapping("/recommend")
    public Result<AiRecommendVO> recommend(@Valid @RequestBody AiRecommendRequest request) {
        AiRecommendVO vo = aiService.recommend(request);
        return Result.success("推荐成功", vo);
    }

    /**
     * @deprecated 兼容期接口，请迁移至 {@code POST /chat}。下一版本将删除。
     */
    @Deprecated
    @PostMapping("/chat")
    public Result<ChatResponse> chat(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @Valid @RequestBody ChatRequest request,
            HttpServletResponse response) {
        log.warn("[DEPRECATED] POST /ai/chat invoked, migrate client to POST /chat");
        response.setHeader("Deprecation", "true");
        response.setHeader("Link", "</api/chat>; rel=\"successor-version\"");
        request.setMessage(PromptSanitizer.sanitize(request.getMessage()));
        ChatResponse chatResponse = chatService.chat(userId, request);
        return Result.success("对话成功", chatResponse);
    }

    /**
     * 自然语言搜索：解析地点、时间、类型等槽位并查询活动/搭子
     */
    @PostMapping("/nl-search")
    public Result<NaturalLanguageSearchVO> nlSearch(@Valid @RequestBody NaturalLanguageSearchRequest request) {
        request.setQuery(PromptSanitizer.sanitize(request.getQuery()));
        NaturalLanguageSearchVO vo = naturalLanguageSearchService.search(request);
        return Result.success("搜索成功", vo);
    }
}
