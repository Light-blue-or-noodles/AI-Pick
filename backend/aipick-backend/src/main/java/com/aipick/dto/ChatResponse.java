package com.aipick.dto;

/**
 * AI对话响应
 *
 * @author AI-Pick
 */
public class ChatResponse {

    /** 会话ID */
    private String sessionId;

    /** AI回复内容 */
    private String reply;

    public ChatResponse(String sessionId, String reply) {
        this.sessionId = sessionId;
        this.reply = reply;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }
}