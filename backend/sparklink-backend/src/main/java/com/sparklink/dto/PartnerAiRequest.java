package com.sparklink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 搭子详情 AI 润色请求
 */
public class PartnerAiRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题过长")
    private String title;

    /** 搭子类型中文，如 运动、游戏 */
    private String typeName;

    /** 偏好关键词 */
    @Size(max = 100, message = "偏好过长")
    private String preference;

    /** 用户已写的初稿（可选） */
    @Size(max = 300, message = "初稿过长")
    private String currentDesc;

    /** 计划时间说明（可选，如已选日期时间） */
    @Size(max = 100, message = "时间说明过长")
    private String planTimeHint;

    /** 地点说明（可选，如地图选点文案） */
    @Size(max = 200, message = "地点说明过长")
    private String locationHint;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public String getCurrentDesc() {
        return currentDesc;
    }

    public void setCurrentDesc(String currentDesc) {
        this.currentDesc = currentDesc;
    }

    public String getPlanTimeHint() {
        return planTimeHint;
    }

    public void setPlanTimeHint(String planTimeHint) {
        this.planTimeHint = planTimeHint;
    }

    public String getLocationHint() {
        return locationHint;
    }

    public void setLocationHint(String locationHint) {
        this.locationHint = locationHint;
    }
}
