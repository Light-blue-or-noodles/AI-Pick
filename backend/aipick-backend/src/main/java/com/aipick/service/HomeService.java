package com.aipick.service;

import com.aipick.vo.HomeRecommendVO;

/**
 * 首页推荐服务接口
 *
 * @author AI-Pick
 */
public interface HomeService {

    /**
     * 获取首页推荐
     *
     * @param userId   用户ID（可选，用于个性化推荐）
     * @param latitude 纬度（可选，用于距离计算）
     * @param longitude 经度（可选，用于距离计算）
     * @return 推荐结果
     */
    HomeRecommendVO getRecommend(Long userId, Double latitude, Double longitude);
}