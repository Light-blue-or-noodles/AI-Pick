package com.aipick.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.aipick.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搭子实体
 *
 * @author AI-Pick
 */
@TableName("t_partner")
public class Partner extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发布者ID */
    private Long userId;

    /** 标题 */
    private String title;

    /** 内容描述 */
    private String content;

    /** 搭子类型 1-吃饭 2-旅游 3-运动 4-学习 5-游戏 6-其他 */
    private Integer type;

    /** 目标人数 */
    private Integer targetCount;

    /** 当前人数 */
    private Integer currentCount;

    /** 活动地点 */
    private String location;

    /** 计划时间 */
    private LocalDateTime planTime;

    /** 封面图片 */
    private String coverImage;

    /** 状态 0-待应征 1-已满 2-已结束 */
    private Integer status;

    /** 浏览量 */
    private Integer viewCount;

    /** 乐观锁版本号 - 数据库表暂未包含此字段 */
    @TableField(exist = false)
    private Integer version;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
    }

    public Integer getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getPlanTime() {
        return planTime;
    }

    public void setPlanTime(LocalDateTime planTime) {
        this.planTime = planTime;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}