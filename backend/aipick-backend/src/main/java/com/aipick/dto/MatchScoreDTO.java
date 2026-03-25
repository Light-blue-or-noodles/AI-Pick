package com.aipick.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 匹配度计算结果 DTO
 * 
 * 输入：userId, targetId
 * 输出：总分 + 各维度分 + 理由 + 建议
 * 
 * @author AI-Pick
 */
public class MatchScoreDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请求用户ID */
    private Long userId;

    /** 目标用户ID */
    private Long targetId;

    /** 总分 (0-100) */
    private Integer totalScore;

    /** 兴趣标签匹配分数 (权重40%) */
    private Integer interestScore;

    /** 地理位置匹配分数 (权重30%) */
    private Integer locationScore;

    /** 时间偏好匹配分数 (权重20%) */
    private Integer timeScore;

    /** AI综合评分 (权重10%) */
    private Integer aiScore;

    /** 匹配理由 */
    private String reason;

    /** 改进建议列表 */
    private List<String> suggestions;

    public MatchScoreDTO() {
    }

    public MatchScoreDTO(Long userId, Long targetId, Integer totalScore, Integer interestScore,
                         Integer locationScore, Integer timeScore, Integer aiScore,
                         String reason, List<String> suggestions) {
        this.userId = userId;
        this.targetId = targetId;
        this.totalScore = totalScore;
        this.interestScore = interestScore;
        this.locationScore = locationScore;
        this.timeScore = timeScore;
        this.aiScore = aiScore;
        this.reason = reason;
        this.suggestions = suggestions;
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

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
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