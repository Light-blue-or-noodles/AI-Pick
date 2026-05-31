package com.sparklink.service.impl;

import com.sparklink.dto.NaturalLanguageSearchRequest;
import com.sparklink.dto.NlSearchQuerySpec;
import com.sparklink.dto.SearchCriteriaDTO;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.service.MemoryFacade;
import com.sparklink.service.NaturalLanguageSearchService;
import com.sparklink.service.SearchService;
import com.sparklink.vo.NaturalLanguageSearchVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 委托 {@link SearchService#smartSearch}，组装 {@link NaturalLanguageSearchVO}
 *
 * @author AI-Pick
 */
@Service
public class NaturalLanguageSearchServiceImpl implements NaturalLanguageSearchService {

    private final SearchService searchService;
    private final MemoryFacade memoryFacade;

    public NaturalLanguageSearchServiceImpl(SearchService searchService, MemoryFacade memoryFacade) {
        this.searchService = searchService;
        this.memoryFacade = memoryFacade;
    }

    @Override
    public NaturalLanguageSearchVO search(NaturalLanguageSearchRequest request) {
        Long userId = resolveUserId(request);
        String userInput = request.getQuery();
        MemoryContext context = memoryFacade.recallForPrompt(userId, userInput);
        String mergedQuery = memoryFacade.mergePrompt(userInput, context);
        NaturalLanguageSearchRequest mergedRequest = new NaturalLanguageSearchRequest();
        mergedRequest.setUserId(request.getUserId());
        mergedRequest.setLimit(request.getLimit());
        mergedRequest.setQuery(mergedQuery);

        SearchService.SearchResultVO result = searchService.smartSearch(mergedRequest);
        NaturalLanguageSearchVO vo = new NaturalLanguageSearchVO();
        vo.setActivities(result.getActivities());
        vo.setPartners(result.getPartners());
        vo.setParsed(toNlSearchQuerySpec(result.getCriteria()));
        memoryFacade.enqueueConversation(userId, userInput, buildModelOutput(result));
        return vo;
    }

    private static Long resolveUserId(NaturalLanguageSearchRequest request) {
        if (request.getUserId() != null) {
            return request.getUserId();
        }
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest servletRequest = attributes.getRequest();
        Object userIdObj = servletRequest.getAttribute("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private static String buildModelOutput(SearchService.SearchResultVO result) {
        SearchCriteriaDTO criteria = result.getCriteria();
        String intent = criteria != null ? criteria.getIntent() : "both";
        int activitySize = result.getActivities() == null ? 0 : result.getActivities().size();
        int partnerSize = result.getPartners() == null ? 0 : result.getPartners().size();
        return "intent=" + intent + ", activities=" + activitySize + ", partners=" + partnerSize;
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
