package com.aipick.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aipick.common.BaseEntity;

import java.io.Serializable;

/**
 * 用户私信消息实体
 *
 * @author AI-Pick
 */
@TableName("t_user_message")
public class UserMessage extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long conversationId;

    /** 发送者ID */
    private Long senderId;

    /** 接收者ID */
    private Long receiverId;

    /** 消息类型 text/image/location */
    private String type;

    /** 消息内容 */
    private String content;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 是否已读 */
    private Boolean isRead;

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

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

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}