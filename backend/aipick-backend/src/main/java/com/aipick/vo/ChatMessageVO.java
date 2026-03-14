package com.aipick.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息视图对象
 *
 * @author AI-Pick
 */
@Data
public class ChatMessageVO {

    /** 消息ID */
    private Long id;

    /** 发送者ID */
    private Long senderId;

    /** 发送者头像 */
    private String senderAvatar;

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

    /** 创建时间 */
    private LocalDateTime createTime;
}