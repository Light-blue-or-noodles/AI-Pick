package com.sparklink.controller;

import com.sparklink.common.Result;
import com.sparklink.dto.NaturalLanguageSearchRequest;
import com.sparklink.service.SearchService;
import com.sparklink.vo.ActivityVO;
import com.sparklink.vo.PartnerVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自然语言搜索控制器
 * 
 * 提供 AI 智能搜索功能，将用户的自然语言输入转换为结构化查询
 * 
 * @author AI-Pick
 */
@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * 自然语言搜索活动
     * 
     * 示例："帮我找北京周末的羽毛球活动"
     */
    @PostMapping("/activities")
    public Result<List<ActivityVO>> searchActivities(@Valid @RequestBody NaturalLanguageSearchRequest request) {
        List<ActivityVO> activities = searchService.searchActivities(request);
        return Result.success("搜索成功", activities);
    }

    /**
     * 自然语言搜索搭子
     * 
     * 示例："帮我找上海周末一起打篮球的搭子"
     */
    @PostMapping("/partners")
    public Result<List<PartnerVO>> searchPartners(@Valid @RequestBody NaturalLanguageSearchRequest request) {
        List<PartnerVO> partners = searchService.searchPartners(request);
        return Result.success("搜索成功", partners);
    }

    /**
     * 智能搜索（同时搜索活动和搭子）
     * 
     * 示例："帮我找北京周末的羽毛球活动或者一起打球的搭子"
     */
    @PostMapping("/smart")
    public Result<Map<String, Object>> smartSearch(@Valid @RequestBody NaturalLanguageSearchRequest request) {
        SearchService.SearchResultVO result = searchService.smartSearch(request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("criteria", result.getCriteria());
        response.put("activities", result.getActivities());
        response.put("partners", result.getPartners());
        
        return Result.success("搜索成功", response);
    }
}