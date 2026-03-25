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

    /** 内容描述（详情，≤300 字） */
    private String content;

    /** 搭子偏好（短文本） */
    private String preference;

    /** 可见范围位掩码：1 公开、2 同事、4 校友，多选为按位或（如 3=公开+同事） */
    private Integer scope;

    /** 搭子类型 1～15，见 PartnerTypeConstants */
    private Integer type;

    /** 目标人数 */
    private Integer targetCount;

    /** 当前人数 */
    private Integer currentCount;

    /** 活动地点（展示文案，如 POI 名称 · 地址） */
    private String location;

    /**
     * 纬度，GCJ-02，与微信地图选点一致；与 longitude 同时有值时可用于距离与附近推荐
     */
    private Double latitude;

    /**
     * 经度，GCJ-02
     */
    private Double longitude;

    /** 计划时间（活动/集合时间，可选） */
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

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public Integer getScope() {
        return scope;
    }

    public void setScope(Integer scope) {
        this.scope = scope;
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