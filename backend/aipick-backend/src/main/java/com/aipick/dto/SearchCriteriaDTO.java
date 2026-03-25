package com.aipick.dto;

import lombok.Data;

/**
 * AI 解析后的搜索条件 DTO
 * 
 * 将自然语言转换为结构化的查询条件
 * 
 * @author AI-Pick
 */
@Data
public class SearchCriteriaDTO {

    /** 解析到的地点关键词 */
    private String location;

    /** 解析到的时间（今天/明天/周末/具体日期） */
    private String timeKeyword;

    /** 解析到的活动类型/分类 */
    private String category;

    /** 解析到的意图类型：activity-找活动，partner-找搭子 */
    private String intent;

    /** 距离范围（公里），0 表示不限制 */
    private Double distanceKm;

    /** 原始查询 */
    private String originalQuery;

    /** AI 解析的置信度 */
    private Double confidence;
}