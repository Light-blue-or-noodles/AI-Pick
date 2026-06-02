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

    /** 联网引用列表 */
    private List<ChatCitationItem> citations;

    /** 联网搜索元信息 */
    private ChatSearchMeta searchMeta;

    public ChatResponse(String sessionId, String reply) {
        this.sessionId = sessionId;
        this.reply = reply;
        this.recommends = Collections.emptyList();
        this.citations = Collections.emptyList();
        this.searchMeta = ChatSearchMeta.notTriggered("not_evaluated", 0);
    }

    public ChatResponse(String sessionId, String reply, List<ChatRecommendItem> recommends) {
        this.sessionId = sessionId;
        this.reply = reply;
        this.recommends = recommends != null ? recommends : Collections.emptyList();
        this.citations = Collections.emptyList();
        this.searchMeta = ChatSearchMeta.notTriggered("not_evaluated", 0);
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

    public List<ChatCitationItem> getCitations() {
        return citations;
    }

    public void setCitations(List<ChatCitationItem> citations) {
        this.citations = citations != null ? citations : Collections.emptyList();
    }

    public ChatSearchMeta getSearchMeta() {
        return searchMeta;
    }

    public void setSearchMeta(ChatSearchMeta searchMeta) {
        this.searchMeta = searchMeta != null ? searchMeta : ChatSearchMeta.notTriggered("not_evaluated", 0);
    }
}