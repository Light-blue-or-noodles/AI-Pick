package com.sparklink.dto;

/**
 * 活动文案 AI 优化请求
 *
 * @author AI-Pick
 */
public class ActivityAiRequest {

    /** 活动标题 */
    private String title;

    /** 活动分类（如：运动、羽毛球） */
    private String category;

    /** 活动地点文本 */
    private String location;

    /** 时间文案，如：2026-03-20 10:00-12:00 */
    private String timeText;

    /** 用户已填写的描述，用于在此基础上优化 */
    private String currentDesc;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTimeText() {
        return timeText;
    }

    public void setTimeText(String timeText) {
        this.timeText = timeText;
    }

    public String getCurrentDesc() {
        return currentDesc;
    }

    public void setCurrentDesc(String currentDesc) {
        this.currentDesc = currentDesc;
    }
}

