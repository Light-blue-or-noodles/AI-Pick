package com.sparklink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 更新用户信息请求
 *
 * @author AI-Pick
 */
public class UpdateUserRequest {

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 性别 0-未知 1-男 2-女 */
    @NotNull(message = "性别不能为空")
    private Integer gender;

    /** 生日 yyyy-MM-dd */
    private String birthday;

    /** 个性签名 */
    private String bio;

    /** 兴趣标签 JSON 数组字符串，如 ["游戏","运动"] */
    private String tags;

    /**
     * 常驻/当前位置：城市名或 "纬度,经度"（GCJ-02），用于匹配度位置维；需先执行 add-user-location.sql 有列
     */
    private String location;

    /** 公司名称（可选） */
    private String companyName;

    /** 学校名称（可选） */
    private String schoolName;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getBio() {
        return bio;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }
}
