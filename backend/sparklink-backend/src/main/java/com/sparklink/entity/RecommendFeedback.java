package com.sparklink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sparklink.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 推荐反馈实体
 * 
 * 记录用户对推荐的反馈（跳过/聊聊），用于持续优化推荐算法
 * 
 * @author AI-Pick
 */
@TableName("t_recommend_feedback")
public class RecommendFeedback extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 反馈类型：1-跳过 2-聊聊 3-举报 4-不合 */
    private Integer feedbackType;

    /** 目标类型：1-搭子 2-活动 */
    private Integer targetType;

    /** 目标ID */
    private Long targetId;

    /** 匹配度分数（推荐时的分数） */
    private Integer matchScore;

    /** 反馈时间 */
    private LocalDateTime feedbackTime;

    /** 扩展字段 JSON */
    private String extra;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getFeedbackType() {
        return feedbackType;
    }

    public void setFeedbackType(Integer feedbackType) {
        this.feedbackType = feedbackType;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public LocalDateTime getFeedbackTime() {
        return feedbackTime;
    }

    public void setFeedbackTime(LocalDateTime feedbackTime) {
        this.feedbackTime = feedbackTime;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}