package com.sparklink.dto;

import java.util.Collections;
import java.util.List;

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

    /** 推荐列表（系统内搭子/活动，供前端展示卡片） */
    private List<ChatRecommendItem> recommends;

    public ChatResponse(String sessionId, String reply) {
        this.sessionId = sessionId;
        this.reply = reply;
        this.recommends = Collections.emptyList();
    }

    public ChatResponse(String sessionId, String reply, List<ChatRecommendItem> recommends) {
        this.sessionId = sessionId;
        this.reply = reply;
        this.recommends = recommends != null ? recommends : Collections.emptyList();
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

    public List<ChatRecommendItem> getRecommends() {
        return recommends;
    }

    public void setRecommends(List<ChatRecommendItem> recommends) {
        this.recommends = recommends != null ? recommends : Collections.emptyList();
    }
}