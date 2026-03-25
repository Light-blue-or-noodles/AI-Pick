package com.aipick.service;

import com.aipick.dto.MatchScoreDTO;

/**
 * 匹配度计算服务接口
 * 
 * @author AI-Pick
 */
public interface MatchScoreService {

    /**
     * 计算两个用户之间的匹配度
     *
     * @param userId   请求用户ID
     * @param targetId 目标用户ID
     * @return 匹配度计算结果
     */
    MatchScoreDTO calculateMatchScore(Long userId, Long targetId);
}