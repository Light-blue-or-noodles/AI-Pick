package com.sparklink.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户搭子列表响应DTO
 * 用于用户详情页展示TA发布的搭子
 *
 * @author AI-Pick
 */
@Data
public class UserPartnerResponse {

    /** 搭子ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 分类编码 */
    private Integer typeCode;

    /** 分类名称 */
    private String typeName;

    /** 封面图片 */
    private String coverImage;

    /** 状态 0-招募中 1-已满 2-已结束 */
    private Integer status;

    /** 状态名称 */
    private String statusName;

    /** 搭子偏好 */
    private String preference;

    /** 标签列表 */
    private List<String> tags;

    /** 当前成员数 */
    private Integer currentCount;

    /** 最大成员数 */
    private Integer maxCount;

    /** 可见性编码 1-公开 2-公司 4-校友 */
    private Integer scope;

    /** 可见性名称 */
    private String scopeName;

    /** 创建时间 */
    private LocalDateTime createTime;
}
