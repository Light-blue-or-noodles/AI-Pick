package com.aipick.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发布活动请求
 *
 * @author AI-Pick
 */
public class CreateActivityRequest {

    /** 活动标题 */
    @NotBlank(message = "活动标题不能为空")
    private String title;

    /** 活动描述 */
    @NotBlank(message = "活动描述不能为空")
    private String description;

    /** 活动类型 1-线下 2-线上 */
    @NotNull(message = "活动类型不能为空")
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
    @DecimalMin(value = "0", message = "费用不能为负数")
    private BigDecimal fee = BigDecimal.ZERO;

    /** 人数上限 0-不限 */
    @Min(value = 0, message = "人数上限不能为负数")
    private Integer maxParticipants = 0;

    /** 封面图片 */
    private String coverImage;

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

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }
}
