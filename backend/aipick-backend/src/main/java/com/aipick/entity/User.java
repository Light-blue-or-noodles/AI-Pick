package com.aipick.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.aipick.common.BaseEntity;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体
 *
 * @author AI-Pick
 */
@TableName("t_user")
public class User extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 性别 0-未知 1-男 2-女 */
    private Integer gender;

    /** 个性签名 */
    private String bio;

    /** 状态 0-正常 1-禁用 */
    private Integer status;

    /** 微信openid */
    private String openid;

    /** 公司名称 */
    private String companyName;

    /** 公司是否已验证 - 数据库暂未包含 */
    @TableField(exist = false)
    private Boolean companyVerified;

    /** 加入公司时间 - 数据库暂未包含 */
    @TableField(exist = false)
    private LocalDateTime companyJoinTime;

    /** 上次变更公司时间 - 数据库暂未包含 */
    @TableField(exist = false)
    private LocalDateTime lastCompanyChangeTime;

    /** 学校名称 */
    private String schoolName;

    /** 生日 yyyy-MM-dd */
    private String birthday;

    /** 兴趣标签 JSON 数组，如 ["游戏","运动"] */
    private String tags;

    /**
     * 常驻位置：城市名或经纬度 "lat,lon"（匹配度等逻辑使用）。
     * 若库表尚未加列，可保持 exist = false，由业务层写入内存对象。
     */
    @TableField(exist = false)
    private String location;

    /**
     * 活跃时间 JSON：{"start":"09:00","end":"22:00"}（匹配度时间维度使用）。
     */
    @TableField(exist = false)
    private String activeTime;

    /** 学校是否已验证 - 数据库暂未包含 */
    @TableField(exist = false)
    private Boolean schoolVerified;

    /** 加入学校时间 - 数据库暂未包含 */
    @TableField(exist = false)
    private LocalDateTime schoolJoinTime;

    /** 上次变更学校时间 - 数据库暂未包含 */
    @TableField(exist = false)
    private LocalDateTime lastSchoolChangeTime;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
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

    public String getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(String activeTime) {
        this.activeTime = activeTime;
    }

    public Boolean getCompanyVerified() {
        return companyVerified;
    }

    public void setCompanyVerified(Boolean companyVerified) {
        this.companyVerified = companyVerified;
    }

    public LocalDateTime getCompanyJoinTime() {
        return companyJoinTime;
    }

    public void setCompanyJoinTime(LocalDateTime companyJoinTime) {
        this.companyJoinTime = companyJoinTime;
    }

    public LocalDateTime getLastCompanyChangeTime() {
        return lastCompanyChangeTime;
    }

    public void setLastCompanyChangeTime(LocalDateTime lastCompanyChangeTime) {
        this.lastCompanyChangeTime = lastCompanyChangeTime;
    }

    public Boolean getSchoolVerified() {
        return schoolVerified;
    }

    public void setSchoolVerified(Boolean schoolVerified) {
        this.schoolVerified = schoolVerified;
    }

    public LocalDateTime getSchoolJoinTime() {
        return schoolJoinTime;
    }

    public void setSchoolJoinTime(LocalDateTime schoolJoinTime) {
        this.schoolJoinTime = schoolJoinTime;
    }

    public LocalDateTime getLastSchoolChangeTime() {
        return lastSchoolChangeTime;
    }

    public void setLastSchoolChangeTime(LocalDateTime lastSchoolChangeTime) {
        this.lastSchoolChangeTime = lastSchoolChangeTime;
    }
}