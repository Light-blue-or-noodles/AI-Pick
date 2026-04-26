package com.sparklink.service.impl;

import com.sparklink.dto.NaturalLanguageSearchRequest;
import com.sparklink.dto.NlSearchQuerySpec;
import com.sparklink.dto.SearchCriteriaDTO;
import com.sparklink.service.NaturalLanguageSearchService;
import com.sparklink.service.SearchService;
import com.sparklink.vo.NaturalLanguageSearchVO;
import org.springframework.stereotype.Service;

/**
 * 委托 {@link SearchService#smartSearch}，组装 {@link NaturalLanguageSearchVO}
 *
 * @author AI-Pick
 */
@Service
public class NaturalLanguageSearchServiceImpl implements NaturalLanguageSearchService {

    private final SearchService searchService;

    public NaturalLanguageSearchServiceImpl(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    public NaturalLanguageSearchVO search(NaturalLanguageSearchRequest request) {
        SearchService.SearchResultVO result = searchService.smartSearch(request);
        NaturalLanguageSearchVO vo = new NaturalLanguageSearchVO();
        vo.setActivities(result.getActivities());
        vo.setPartners(result.getPartners());
        vo.setParsed(toNlSearchQuerySpec(result.getCriteria()));
        return vo;
    }

    private static NlSearchQuerySpec toNlSearchQuerySpec(SearchCriteriaDTO c) {
        NlSearchQuerySpec spec = new NlSearchQuerySpec();
        if (c == null) {
            return spec;
        }
        String intent = c.getIntent();
        if ("activity".equals(intent)) {
            spec.setIntent("activity");
        } else if ("partner".equals(intent)) {
            spec.setIntent("partner");
        } else {
            spec.setIntent("both");
        }
        spec.setLocationKeyword(c.getLocation());
        spec.setCategory(c.getCategory());
        if (c.getOriginalQuery() != null && !c.getOriginalQuery().isBlank()) {
            spec.getKeywords().add(c.getOriginalQuery().trim());
        }
        String timeKeyword = c.getTimeKeyword();
        if (timeKeyword != null && timeKeyword.contains("周末")) {
            spec.setWeekendOnly(true);
        }
        return spec;
    }
}
