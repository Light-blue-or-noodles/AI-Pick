package com.aipick.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * 发布搭子请求
 *
 * @author AI-Pick
 */
public class CreatePartnerRequest {

    /** 标题 */
    @NotBlank(message = "标题不能为空")
    private String title;

    /** 内容描述 */
    @NotBlank(message = "内容描述不能为空")
    private String content;

    /** 搭子类型 1-吃饭 2-旅游 3-运动 4-学习 5-游戏 6-其他 */
    @NotNull(message = "搭子类型不能为空")
    private Integer type;

    /** 目标人数 */
    @Min(value = 1, message = "目标人数至少为 1")
    private Integer targetCount = 2;

    /** 活动地点 */
    private String location;

    /** 计划时间 */
    private LocalDateTime planTime;

    /** 封面图片 */
    private String coverImage;

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
}
