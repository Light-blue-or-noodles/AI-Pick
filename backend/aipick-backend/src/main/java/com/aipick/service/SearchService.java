package com.aipick.service;

import com.aipick.dto.NaturalLanguageSearchRequest;
import com.aipick.dto.SearchCriteriaDTO;
import com.aipick.entity.Activity;
import com.aipick.entity.Partner;
import com.aipick.vo.ActivityVO;
import com.aipick.vo.PartnerVO;

import java.util.List;

/**
 * 自然语言搜索服务接口
 * 
 * 将用户的自然语言输入解析为结构化的查询条件，
 * 并执行搜索返回结果
 * 
 * @author AI-Pick
 */
public interface SearchService {

    /**
     * 自然语言搜索活动
     * 
     * @param request 搜索请求
     * @return 活动列表（含匹配度）
     */
    List<ActivityVO> searchActivities(NaturalLanguageSearchRequest request);

    /**
     * 自然语言搜索搭子
     * 
     * @param request 搜索请求
     * @return 搭子列表（含匹配度）
     */
    List<PartnerVO> searchPartners(NaturalLanguageSearchRequest request);

    /**
     * AI 解析自然语言为查询条件
     * 
     * @param query 用户输入的自然语言
     * @return 解析后的搜索条件
     */
    SearchCriteriaDTO parseQuery(String query);

    /**
     * 智能搜索（同时搜索活动和搭子）
     * 
     * @param request 搜索请求
     * @return 搜索结果
     */
    SearchResultVO smartSearch(NaturalLanguageSearchRequest request);

    /**
     * 搜索结果 VO
     */
    class SearchResultVO {
        private List<ActivityVO> activities;
        private List<PartnerVO> partners;
        private SearchCriteriaDTO criteria;

        public List<ActivityVO> getActivities() {
            return activities;
        }

        public void setActivities(List<ActivityVO> activities) {
            this.activities = activities;
        }

        public List<PartnerVO> getPartners() {
            return partners;
        }

        public void setPartners(List<PartnerVO> partners) {
            this.partners = partners;
        }

        public SearchCriteriaDTO getCriteria() {
            return criteria;
        }

        public void setCriteria(SearchCriteriaDTO criteria) {
            this.criteria = criteria;
        }
    }
}