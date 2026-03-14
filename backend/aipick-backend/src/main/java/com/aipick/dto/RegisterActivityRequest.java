package com.aipick.dto;

/**
 * 报名活动请求
 *
 * @author AI-Pick
 */
public class RegisterActivityRequest {

    /** 报名留言 */
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}