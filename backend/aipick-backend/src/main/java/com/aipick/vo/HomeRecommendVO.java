package com.aipick.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页推荐视图对象
 *
 * @author AI-Pick
 */
@Data
public class HomeRecommendVO {

    /** 推荐搭子列表 */
    private List<PartnerVO> partners;

    /** 推荐活动列表 */
    private List<ActivityVO> activities;
}