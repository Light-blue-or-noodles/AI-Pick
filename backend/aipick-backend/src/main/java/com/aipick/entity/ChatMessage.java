package com.aipick.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aipick.common.BaseEntity;

import java.io.Serializable;

/**
 * AI对话消息实体
 *
 * @author AI-Pick
 */
@TableName("t_chat_message")
public class ChatMessage extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private String sessionId;

    /** 用户ID */
    private Long userId;

    /** 消息类型 1-用户 2-AI */
    private Integer type;

    /** 消息内容 */
    private String content;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}