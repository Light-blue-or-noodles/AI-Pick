package com.sparklink.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 智能推荐结果视图
 *
 * @author AI-Pick
 */
@Data
public class AiRecommendVO {

    /** 推荐搭子列表（含匹配度） */
    private List<PartnerVO> partners;

    /** 推荐活动列表（含匹配度） */
    private List<ActivityVO> activities;
}
