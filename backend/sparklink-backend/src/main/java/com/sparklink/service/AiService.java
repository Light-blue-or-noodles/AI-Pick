package com.sparklink.service;

import com.sparklink.dto.AiRecommendRequest;
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
}
