package com.aipick.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

/**
 * AI 智能推荐请求
 *
 * @author AI-Pick
 */
@Data
public class AiRecommendRequest {

    /** 用户ID（可选，不传则匿名推荐） */
    private Long userId;

    /** 纬度（可选，用于距离排序） */
    private Double latitude;

    /** 经度（可选，用于距离排序） */
    private Double longitude;

    /** 兴趣类型列表（搭子类型 1-吃饭 2-旅游 3-运动 4-学习 5-游戏 6-其他） */
    private List<Integer> interestTypes;

    /** 活动分类（如 运动/美食/学习/娱乐） */
    private String category;

    /** 推荐搭子数量上限，默认 5 */
    @Min(1)
    @Max(20)
    private Integer partnerLimit = 5;

    /** 推荐活动数量上限，默认 5 */
    @Min(1)
    @Max(20)
    private Integer activityLimit = 5;
}
