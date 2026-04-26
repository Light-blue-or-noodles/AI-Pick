package com.sparklink.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 发送消息请求
 *
 * @author AI-Pick
 */
public class SendMessageRequest {

    /** 接收者ID */
    @NotNull(message = "接收者ID不能为空")
    private Long receiverId;

    /** 消息类型 text/image/location */
    @NotNull(message = "消息类型不能为空")
    private String type;

    /** 消息内容 */
    @NotNull(message = "消息内容不能为空")
    private String content;

    /** 纬度（位置类型） */
    private Double latitude;

    /** 经度（位置类型） */
    private Double longitude;

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}