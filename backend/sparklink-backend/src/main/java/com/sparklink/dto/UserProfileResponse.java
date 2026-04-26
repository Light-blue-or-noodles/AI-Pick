package com.sparklink.dto;

import lombok.Data;

/**
 * 用户资料响应DTO
 * 用于用户详情页展示
 *
 * @author AI-Pick
 */
@Data
public class UserProfileResponse {

    /** 用户ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 个性签名 */
    private String bio;

    /** 公司名称 */
    private String companyName;

    /** 学校名称 */
    private String schoolName;

    /** 粉丝数 */
    private Integer followerCount;

    /** 关注数 */
    private Integer followingCount;

    /** 当前登录用户是否已关注该用户 */
    private Boolean isFollowed;

    /** 用户可见的搭子数量 */
    private Integer partnersCount;
}
