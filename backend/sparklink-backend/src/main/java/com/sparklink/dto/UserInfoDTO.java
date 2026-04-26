package com.sparklink.dto;

/**
 * 用户安全信息 DTO（对外返回使用）
 *
 * 仅包含对外可暴露的基本信息，避免敏感字段泄露。
 *
 * @author AI-Pick
 */
public class UserInfoDTO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别 0-未知 1-男 2-女 */
    private Integer gender;

    /** 个性签名 */
    private String bio;

    /** 公司名称 */
    private String companyName;

    /** 公司是否已验证 */
    private Boolean companyVerified;

    /** 学校名称 */
    private String schoolName;

    /** 生日 yyyy-MM-dd */
    private String birthday;

    /** 兴趣标签 JSON 数组字符串 */
    private String tags;

    /** 常驻/当前位置（城市或 lat,lon） */
    private String location;

    /** 学校是否已验证 */
    private Boolean schoolVerified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getBio() {
        return bio;
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

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
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

    public Boolean getCompanyVerified() {
        return companyVerified;
    }

    public void setCompanyVerified(Boolean companyVerified) {
        this.companyVerified = companyVerified;
    }

    public Boolean getSchoolVerified() {
        return schoolVerified;
    }

    public void setSchoolVerified(Boolean schoolVerified) {
        this.schoolVerified = schoolVerified;
    }
}

