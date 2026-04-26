package com.sparklink.service;

import com.sparklink.dto.MatchScoreDTO;

/**
 * 匹配度计算服务接口
 *
 * @author AI-Pick
 */
public interface MatchScoreService {

    /**
     * 计算匹配度：在搭子详情等场景可传 {@code partnerId}，以活动偏好标签与活动位置参与计算。
     *
     * @param userId    当前用户
     * @param targetId  对方用户（发布者）
     * @param partnerId 搭子活动 ID，为 null 时仅按两用户与位置规则（重新分配权重）
     */
    MatchScoreDTO calculateMatchScore(Long userId, Long targetId, Long partnerId);

    /**
     * 仅两用户（无活动维度），等效于 {@code partnerId == null}。
     */
    default MatchScoreDTO calculateMatchScore(Long userId, Long targetId) {
        return calculateMatchScore(userId, targetId, null);
    }
}
