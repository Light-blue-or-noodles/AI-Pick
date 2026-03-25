package com.aipick.dto;

import com.aipick.common.PartnerTypeConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布搭子请求：标题、类型、偏好、详情（≤300 字）、单封面、可见范围（可多选：1 公开、2 同事、4 校友）
 */
public class CreatePartnerRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题不超过 50 字")
    private String title;

    /** 详情描述，≤300 字 */
    @NotBlank(message = "详情不能为空")
    @Size(max = 300, message = "详情不超过 300 字")
    private String content;

    /** 搭子偏好：英文逗号分隔的标签，见 PartnerPreferenceConstants */
    @NotBlank(message = "请选择搭子偏好")
    @Size(max = 512, message = "偏好标签过长")
    private String preference;

    /** 搭子类型 1～15，见 PartnerTypeConstants */
    @NotNull(message = "搭子类型不能为空")
    @Min(value = PartnerTypeConstants.MIN_CODE, message = "搭子类型无效")
    @Max(value = PartnerTypeConstants.MAX_CODE, message = "搭子类型无效")
    private Integer type;

    /**
     * 可见范围，多选；每项为位值：1-公开、2-同事、4-校友（与 PartnerScopeConstants 一致）
     */
    @NotEmpty(message = "请至少选择一种可见范围")
    private List<Integer> scopes;

    /** 目标人数 */
    @Min(value = 2, message = "目标人数至少为 2")
    private Integer targetCount = 2;

    /** 活动地点展示文案（可选），如地图选点返回的名称与地址 */
    @Size(max = 200, message = "地点描述过长")
    private String location;

    /**
     * 纬度 GCJ-02（可选），须与 longitude 同时提交；用于距离与附近推荐
     */
    private Double latitude;

    /**
     * 经度 GCJ-02（可选）
     */
    private Double longitude;

    /** 计划时间 / 集合时间（可选） */
    private LocalDateTime planTime;

    /** 封面图，单张，相对路径 /static/... */
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

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<Integer> getScopes() {
        return scopes;
    }

    public void setScopes(List<Integer> scopes) {
        this.scopes = scopes;
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

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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
