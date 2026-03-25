package com.aipick.service;

import com.aipick.entity.RecommendFeedback;

import java.util.Map;
import java.util.Set;

/**
 * 推荐反馈服务接口
 * 
 * 记录用户对推荐的反馈（跳过/聊聊/不合），用于持续优化推荐算法
 * 
 * @author AI-Pick
 */
public interface RecommendFeedbackService {

    /**
     * 记录推荐反馈
     * 
     * @param userId 用户ID
     * @param targetType 目标类型：1-搭子 2-活动
     * @param targetId 目标ID
     * @param feedbackType 反馈类型：1-跳过 2-聊聊 3-举报 4-不合
     * @param matchScore 匹配度分数
     * @return 是否成功
     */
    boolean recordFeedback(Long userId, Integer targetType, Long targetId, 
                          Integer feedbackType, Integer matchScore);

    /**
     * 获取用户对某目标的反馈次数
     * 
     * @param userId 用户ID
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @return 反馈次数
     */
    int getFeedbackCount(Long userId, Integer targetType, Long targetId);

    /**
     * 获取用户跳过某类型的次数（用于降低推荐权重）
     * 
     * @param userId 用户ID
     * @param targetType 目标类型
     * @return 跳过次数
     */
    int getSkipCount(Long userId, Integer targetType);

    /**
     * 负向反馈（跳过 + 不合）条数，用于统计
     *
     * @param userId 用户ID
     * @param targetType 目标类型
     * @return 条数
     */
    int getNegativeFeedbackCount(Long userId, Integer targetType);

    /**
     * 用户已标记为跳过或不合的目标 ID，用于推荐过滤
     *
     * @param userId 用户ID
     * @param targetType 目标类型
     * @return 目标主键集合
     */
    Set<Long> findNegativeTargetIds(Long userId, Integer targetType);

    /**
     * 按搭子类型统计负向反馈次数（同类型多次跳过会提高惩罚）
     *
     * @param userId 用户ID
     * @return key：搭子类型码，value：负向反馈次数
     */
    Map<Integer, Integer> countNegativeByPartnerType(Long userId);

    /**
     * 按活动分类统计负向反馈次数
     *
     * @param userId 用户ID
     * @return key：分类名，value：负向反馈次数
     */
    Map<String, Integer> countNegativeByActivityCategory(Long userId);
}