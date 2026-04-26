package com.sparklink.service;

import com.sparklink.dto.NaturalLanguageSearchRequest;
import com.sparklink.vo.NaturalLanguageSearchVO;

/**
 * 自然语言搜索（供 /ai/nl-search 等统一入口）
 *
 * @author AI-Pick
 */
public interface NaturalLanguageSearchService {

    /**
     * 解析自然语言并返回活动、搭子结果
     *
     * @param request 搜索请求
     * @return 解析条件与列表结果
     */
    NaturalLanguageSearchVO search(NaturalLanguageSearchRequest request);
}
