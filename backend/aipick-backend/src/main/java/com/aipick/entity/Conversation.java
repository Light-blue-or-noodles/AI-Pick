package com.aipick.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aipick.common.BaseEntity;

import java.io.Serializable;

/**
 * 用户会话实体（用于私信）
 *
 * @author AI-Pick
 */
@TableName("t_conversation")
public class Conversation extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户1 ID */
    private Long userId1;

    /** 用户2 ID */
    private Long userId2;

    /** 最后一条消息ID */
    private Long lastMessageId;

    /** 未读数量（用户1） */
    private Integer unreadCount1;

    /** 未读数量（用户2） */
    private Integer unreadCount2;

    public Long getUserId1() {
        return userId1;
    }

    public void setUserId1(Long userId1) {
        this.userId1 = userId1;
    }

    public Long getUserId2() {
        return userId2;
    }

    public void setUserId2(Long userId2) {
        this.userId2 = userId2;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public Integer getUnreadCount1() {
        return unreadCount1;
    }

    public void setUnreadCount1(Integer unreadCount1) {
        this.unreadCount1 = unreadCount1;
    }

    public Integer getUnreadCount2() {
        return unreadCount2;
    }

    public void setUnreadCount2(Integer unreadCount2) {
        this.unreadCount2 = unreadCount2;
    }
}