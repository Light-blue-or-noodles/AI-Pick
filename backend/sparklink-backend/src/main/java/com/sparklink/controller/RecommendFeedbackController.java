package com.sparklink.controller;

import com.sparklink.common.Result;
import com.sparklink.service.RecommendFeedbackService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 推荐反馈控制器
 * 
 * 记录用户对推荐的反馈（跳过/聊聊），用于持续优化推荐算法
 * 
 * @author AI-Pick
 */
@RestController
@RequestMapping("/recommend")
public class RecommendFeedbackController {

    private final RecommendFeedbackService feedbackService;

    public RecommendFeedbackController(RecommendFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * 记录推荐反馈
     * 
     * @param userId 用户ID
     * @param targetType 目标类型：1-搭子 2-活动
     * @param targetId 目标ID
     * @param feedbackType 反馈类型：1-跳过 2-聊聊 3-举报 4-不合
     * @param matchScore 匹配度分数（可选）
     */
    @PostMapping("/feedback")
    public Result<Void> recordFeedback(
            @RequestParam Long userId,
            @RequestParam Integer targetType,
            @RequestParam Long targetId,
            @RequestParam Integer feedbackType,
            @RequestParam(required = false) Integer matchScore) {

        boolean success = feedbackService.recordFeedback(userId, targetType, targetId, feedbackType, matchScore);
        
        if (success) {
            return Result.success("反馈记录成功", null);
        } else {
            return Result.error("反馈记录失败");
        }
    }

    /**
     * 批量记录反馈
     */
    @PostMapping("/feedback/batch")
    public Result<Map<String, Object>> batchRecordFeedback(@RequestBody Map<String, Object> request) {
        Long userId = getLongParam(request, "userId");
        Integer targetType = getIntParam(request, "targetType");
        java.util.List<Map<String, Object>> feedbacks = (java.util.List<Map<String, Object>>) request.get("feedbacks");

        if (userId == null || targetType == null || feedbacks == null || feedbacks.isEmpty()) {
            return Result.error("参数不完整");
        }

        int successCount = 0;
        for (Map<String, Object> fb : feedbacks) {
            Long targetId = getLongParam(fb, "targetId");
            Integer feedbackType = getIntParam(fb, "feedbackType");
            Integer matchScore = getIntParam(fb, "matchScore");

            if (feedbackService.recordFeedback(userId, targetType, targetId, feedbackType, matchScore)) {
                successCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", feedbacks.size());
        result.put("success", successCount);

        return Result.success("批量反馈记录完成", result);
    }

    /**
     * 获取用户反馈统计
     */
    @GetMapping("/feedback/stats")
    public Result<Map<String, Object>> getFeedbackStats(
            @RequestParam Long userId,
            @RequestParam(required = false) Integer targetType) {

        Map<String, Object> stats = new HashMap<>();

        if (targetType == null || targetType == 1) {
            stats.put("partnerSkipCount", feedbackService.getSkipCount(userId, 1));
            stats.put("partnerNegativeCount", feedbackService.getNegativeFeedbackCount(userId, 1));
        }
        if (targetType == null || targetType == 2) {
            stats.put("activitySkipCount", feedbackService.getSkipCount(userId, 2));
            stats.put("activityNegativeCount", feedbackService.getNegativeFeedbackCount(userId, 2));
        }

        return Result.success("统计成功", stats);
    }

    private Long getLongParam(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Integer getIntParam(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
}