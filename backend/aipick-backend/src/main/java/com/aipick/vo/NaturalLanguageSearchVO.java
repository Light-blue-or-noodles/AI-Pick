package com.aipick.vo;

import com.aipick.dto.NlSearchQuerySpec;
import lombok.Data;

import java.util.List;

/**
 * 自然语言搜索结果
 *
 * @author AI-Pick
 */
@Data
public class NaturalLanguageSearchVO {

    /** 解析得到的查询条件（前端可展示「为您理解成…」） */
    private NlSearchQuerySpec parsed;

    /** 活动列表 */
    private List<ActivityVO> activities;

    /** 搭子列表 */
    private List<PartnerVO> partners;
}
