package com.aipick.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话视图对象
 *
 * @author AI-Pick
 */
@Data
public class ConversationVO {

    /** 会话ID */
    private Long conversationId;

    /** 用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 最后一条消息 */
    private String lastMessage;

    /** 最后消息时间 */
    private LocalDateTime lastMessageTime;

    /** 未读数量 */
    private Integer unreadCount;
}