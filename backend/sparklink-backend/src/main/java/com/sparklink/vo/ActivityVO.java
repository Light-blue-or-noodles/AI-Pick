package com.sparklink.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动视图对象
 *
 * @author AI-Pick
 */
@Data
public class ActivityVO {

    /** 活动ID */
    private Long id;

    /** 发布者ID */
    private Long userId;

    /** 主办方信息 */
    private OrganizerVO organizer;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 类型 */
    private String type;

    /** 活动分类（如 运动/美食，列表与推荐场景使用） */
    private String category;

    /** 标签 */
    private List<String> tags;

    /** 封面图片列表 */
    private List<String> coverImages;

    /** 单张封面图片（兼容旧字段） */
    private String coverImage;

    /** 活动时间 */
    private LocalDateTime eventTime;

    /** 最大人数 */
    private Integer maxParticipants;

    /** 当前人数 */
    private Integer currentParticipants;

    /** 费用 */
    private Double fee;

    /** 范围 */
    private String scope;

    /** 纬度 */
    private Double latitude;

    /** 经度 */
    private Double longitude;

    /** 地址 */
    private String address;

    /** 状态 */
    private Integer status;

    /** 是否已报名 */
    private Boolean isJoined;

    /** 是否已收藏 */
    private Boolean isFavorited;

    /** 参与者列表 */
    private List<ParticipantVO> participants;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 距离 */
    private Double distance;

    /** 匹配度（0-99，AI 推荐时使用） */
    private Integer matchScore;
}