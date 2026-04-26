package com.sparklink.dto;

/**
 * 登录响应
 *
 * @author AI-Pick
 */
public class LoginResponse {

    /** 访问令牌 */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 是否新用户 */
    private Boolean isNew;

    public LoginResponse(String token, Long userId, String username, String nickname, String avatar) {
        this(token, userId, username, nickname, avatar, false);
    }

    public LoginResponse(String token, Long userId, String username, String nickname, String avatar, Boolean isNew) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.isNew = isNew;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }
}