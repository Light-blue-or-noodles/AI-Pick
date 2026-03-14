package com.aipick.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aipick.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动报名记录实体
 *
 * @author AI-Pick
 */
@TableName("t_activity_registration")
public class ActivityRegistration extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    private Long activityId;

    /** 报名人ID */
    private Long userId;

    /** 报名留言 */
    private String message;

    /** 状态 0-已报名 1-已取消 */
    private Integer status;

    /** 签到时间 */
    private LocalDateTime checkInTime;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }
}