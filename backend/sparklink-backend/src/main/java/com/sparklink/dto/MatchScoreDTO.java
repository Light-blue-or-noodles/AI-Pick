package com.sparklink.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 匹配度计算结果 DTO
 * <p>
 * 输入：userId, targetId（发布者）, partnerId（搭子活动 ID，可选；无则按两用户间规则并重新分配权重）
 * 输出：总分 + 各维度分 + 理由 + 建议
 * <p>
 * 维度（有搭子 ID 时）：活动标签 30% + 发布者标签 10% + 活动位置 30% + AI 30%（无时间维）；无搭子 ID 时：发布者标签 40% + 用户间位置 30% + AI 30%。
 */
public class MatchScoreDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请求用户ID */
    private Long userId;

    /** 目标用户ID（发布者） */
    private Long targetId;

    /**
     * 搭子活动 ID；传入时以活动维计算「活动标签 / 活动位置」。
     */
    private Long partnerId;

    /** 总分 (0-100) */
    private Integer totalScore;

    /**
     * 与「本搭子活动偏好/标签」的匹配分：我的标签 vs 活动 preference 解析出的标签 (0-100)。
     * 兼容旧字段名 interestScore 时，interestScore 与此字段一致。
     */
    private Integer activityTagScore;

    /**
     * 与「发布者个人兴趣标签」的匹配分：我的标签 vs 发布者用户 tags (0-100)。
     */
    private Integer publisherTagScore;

    /**
     * 兼容历史客户端：同 activityTagScore（活动侧标签分）。
     */
    private Integer interestScore;

    /** 位置匹配：有搭子时为「我 vs 活动位置」，无搭子时为「我 vs 对方用户」的常驻位置 (0-100) */
    private Integer locationScore;

    /** 已弃用，恒为 0 */
    private Integer timeScore;

    /** AI 综合评分 (0-100) */
    private Integer aiScore;

    /** 匹配理由 */
    private String reason;

    /** 改进建议列表 */
    private List<String> suggestions;

    public MatchScoreDTO() {
    }

    // Getters and Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getActivityTagScore() {
        return activityTagScore;
    }

    public void setActivityTagScore(Integer activityTagScore) {
        this.activityTagScore = activityTagScore;
    }

    public Integer getPublisherTagScore() {
        return publisherTagScore;
    }

    public void setPublisherTagScore(Integer publisherTagScore) {
        this.publisherTagScore = publisherTagScore;
    }

    public Integer getInterestScore() {
        return interestScore;
    }

    public void setInterestScore(Integer interestScore) {
        this.interestScore = interestScore;
    }

    public Integer getLocationScore() {
        return locationScore;
    }

    public void setLocationScore(Integer locationScore) {
        this.locationScore = locationScore;
    }

    public Integer getTimeScore() {
        return timeScore;
    }

    public void setTimeScore(Integer timeScore) {
        this.timeScore = timeScore;
    }

    public Integer getAiScore() {
        return aiScore;
    }

    public void setAiScore(Integer aiScore) {
        this.aiScore = aiScore;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
