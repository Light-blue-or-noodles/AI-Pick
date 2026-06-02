package com.sparklink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI对话请求
 *
 * @author AI-Pick
 */
public class ChatRequest {

    /** 会话ID（首次为空） */
    private String sessionId;

    /** 消息内容 */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容长度不能超过2000个字符")
    private String message;

    /** 联网搜索模式：auto/on/off */
    private String webSearchMode = "auto";

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWebSearchMode() {
        return webSearchMode;
    }

    public void setWebSearchMode(String webSearchMode) {
        this.webSearchMode = webSearchMode;
    }
}