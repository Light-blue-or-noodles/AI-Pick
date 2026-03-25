package com.aipick.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搭子视图对象
 *
 * @author AI-Pick
 */
@Data
public class PartnerVO {

    /** 搭子ID */
    private Long id;

    /** 发布者ID */
    private Long userId;

    /** 发布者昵称 */
    private String nickname;

    /** 发布者头像 */
    private String avatar;

    /** 发布者年龄 */
    private Integer age;

    /** 发布者性别 */
    private Integer gender;

    /** 星级评分 */
    private Double starRating;

    /** 活动等级 */
    private String activityLevel;

    /** 个人简介 */
    private String bio;

    /** 兴趣标签 */
    private List<String> tags;

    /** 公司名称 */
    private String companyName;

    /** 公司认证 */
    private Boolean companyVerified;

    /** 学校名称 */
    private String schoolName;

    /** 学校认证 */
    private Boolean schoolVerified;

    /** 标题 */
    private String title;

    /** 描述（详情正文） */
    private String description;

    /** 搭子偏好 */
    private String preference;

    /** 状态 0招募中 1已满 2已结束 */
    private Integer status;

    /** 类型编码 1～15 */
    private Integer typeCode;

    /** 类型展示名，如 游戏、运动 */
    private String typeName;

    /** 封面图 */
    private String coverImage;

    /** 类型（兼容旧字段，可为数字字符串） */
    private String type;

    /** 可见范围位掩码（1 公开、2 同事、4 校友） */
    private Integer scope;

    /** 范围展示名，如「公开、同事」 */
    private String scopeName;

    /** 最大人数 */
    private Integer maxParticipants;

    /** 当前人数 */
    private Integer currentParticipants;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 地址 */
    private String address;

    /** 匹配度 */
    private Integer matchScore;

    /** 是否关注 */
    private Boolean isFollowed;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 计划/集合时间 */
    private LocalDateTime planTime;

    /** 距离 */
    private Double distance;
}