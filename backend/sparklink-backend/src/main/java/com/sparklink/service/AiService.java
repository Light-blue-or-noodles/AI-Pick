package com.sparklink.service;

import com.sparklink.dto.AiRecommendRequest;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.vo.AiRecommendVO;

/**
 * AI 推荐服务接口
 *
 * @author AI-Pick
 */
public interface AiService {

    /**
     * AI 智能推荐：基于用户兴趣、位置、时间等推荐搭子与活动
     *
     * @param request 推荐请求参数
     * @return 推荐结果（搭子+活动，含匹配度）
     */
    AiRecommendVO recommend(AiRecommendRequest request);

    /**
     * AI 社交助手对话：找搭子、发现活动、优化个人资料
     *
     * @param request 对话请求（sessionId、message）
     * @return 会话ID 与 AI 回复内容
     */
    ChatResponse chat(ChatRequest request);
}
