package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.dto.AiRecommendRequest;
import com.aipick.dto.ChatRequest;
import com.aipick.dto.ChatResponse;
import com.aipick.dto.NaturalLanguageSearchRequest;
import com.aipick.service.AiService;
import com.aipick.service.NaturalLanguageSearchService;
import com.aipick.util.PromptSanitizer;
import com.aipick.vo.AiRecommendVO;
import com.aipick.vo.NaturalLanguageSearchVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 能力控制器（智能推荐等）
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiService aiService;
    private final NaturalLanguageSearchService naturalLanguageSearchService;

    public AiController(AiService aiService, NaturalLanguageSearchService naturalLanguageSearchService) {
        this.aiService = aiService;
        this.naturalLanguageSearchService = naturalLanguageSearchService;
    }

    /**
     * AI 智能推荐：基于用户兴趣、行为、位置推荐搭子与活动
     *
     * @param request 推荐请求（兴趣类型、分类、数量等）
     * @return 推荐结果，含匹配度
     */
    @PostMapping("/recommend")
    public Result<AiRecommendVO> recommend(@Valid @RequestBody AiRecommendRequest request) {
        AiRecommendVO vo = aiService.recommend(request);
        return Result.success("推荐成功", vo);
    }

    /**
     * AI 社交助手对话：找搭子、发现活动、优化个人资料（MVP 规则回复，后续可接大模型）
     *
     * @param request 对话请求（sessionId、message）
     * @return sessionId 与 AI 回复内容
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        request.setMessage(PromptSanitizer.sanitize(request.getMessage()));
        ChatResponse response = aiService.chat(request);
        return Result.success("对话成功", response);
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
