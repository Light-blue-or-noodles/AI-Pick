package com.sparklink.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信小程序登录请求
 *
 * @author AI-Pick
 */
public class WechatLoginRequest {

    /** 小程序 wx.login() 返回的 code */
    @NotBlank(message = "code不能为空")
    private String code;

    /** 用户基本信息（可选） */
    private UserInfo userInfo;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * 用户基本信息
     */
    public static class UserInfo {
        /** 昵称 */
        private String nickname;
        /** 头像URL */
        private String avatar;
        /** 性别（0-未知/1-男/2-女） */
        private Integer gender;

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

        public Integer getGender() {
            return gender;
        }

        public void setGender(Integer gender) {
            this.gender = gender;
        }
    }
}