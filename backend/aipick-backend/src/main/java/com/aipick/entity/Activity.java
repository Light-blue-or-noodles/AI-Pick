package com.aipick.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aipick.common.BaseEntity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 活动实体
 *
 * @author AI-Pick
 */
@TableName("t_activity")
public class Activity extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 发布者ID */
    private Long userId;

    /** 活动标题 */
    private String title;

    /** 活动描述 */
    private String description;

    /** 活动类型 1-线下 2-线上 */
    private Integer type;

    /** 分类 */
    private String category;

    /** 报名截止时间 */
    private LocalDateTime registerEndTime;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 活动地点 */
    private String location;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

    /** 费用 0-免费 */
    private BigDecimal fee;

    /** 人数上限 0-不限 */
    private Integer maxParticipants;

    /** 当前报名人数 */
    private Integer currentParticipants;

    /** 封面图片（与 images 首张一致，兼容列表展示） */
    private String coverImage;

    /** 多图 JSON 数组，首张为封面，其余在详情展示 */
    private String images;

    /** 状态 0-待开始 1-报名中 2-进行中 3-已结束 4-已取消 */
    private Integer status;

    /** 浏览量 */
    private Integer viewCount;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getRegisterEndTime() {
        return registerEndTime;
    }

    public void setRegisterEndTime(LocalDateTime registerEndTime) {
        this.registerEndTime = registerEndTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
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
}