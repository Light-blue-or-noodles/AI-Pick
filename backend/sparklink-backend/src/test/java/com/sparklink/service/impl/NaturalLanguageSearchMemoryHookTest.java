package com.sparklink.service.impl;

import com.sparklink.dto.NaturalLanguageSearchRequest;
import com.sparklink.dto.SearchCriteriaDTO;
import com.sparklink.memory.service.MemoryFacade;
import com.sparklink.service.SearchService;
import com.sparklink.vo.NaturalLanguageSearchVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaturalLanguageSearchMemoryHookTest {

    @Mock
    private SearchService searchService;
    @Mock
    private MemoryFacade memoryFacade;

    @Test
    void search_shouldRecallMergeAndEnqueueMemoryHooks() {
        NaturalLanguageSearchServiceImpl service = new NaturalLanguageSearchServiceImpl(searchService, memoryFacade);
        NaturalLanguageSearchRequest request = new NaturalLanguageSearchRequest();
        request.setUserId(123L);
        request.setQuery("帮我找北京周末羽毛球活动");

        when(memoryFacade.recallForPrompt(123L, "帮我找北京周末羽毛球活动")).thenReturn(null);
        when(memoryFacade.mergePrompt(eq("帮我找北京周末羽毛球活动"), isNull())).thenReturn("merged-query");
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setIntent("activity");
        SearchService.SearchResultVO searchResult = new SearchService.SearchResultVO();
        searchResult.setCriteria(criteria);
        when(searchService.smartSearch(any())).thenReturn(searchResult);

        NaturalLanguageSearchVO result = service.search(request);

        ArgumentCaptor<NaturalLanguageSearchRequest> requestCaptor = ArgumentCaptor.forClass(NaturalLanguageSearchRequest.class);
        InOrder ordered = inOrder(memoryFacade, searchService);
        ordered.verify(memoryFacade).recallForPrompt(123L, "帮我找北京周末羽毛球活动");
        ordered.verify(memoryFacade).mergePrompt(eq("帮我找北京周末羽毛球活动"), isNull());
        ordered.verify(searchService).smartSearch(requestCaptor.capture());
        ordered.verify(memoryFacade).enqueueConversation(eq(123L), eq("帮我找北京周末羽毛球活动"), anyString());

        assertEquals("merged-query", requestCaptor.getValue().getQuery());
        assertEquals("activity", result.getParsed().getIntent());
        verify(memoryFacade).enqueueConversation(eq(123L), eq("帮我找北京周末羽毛球活动"), anyString());
    }

    @Test
    void search_shouldUseResolvedUserIdWhenRequestUserIdMissing() {
        NaturalLanguageSearchServiceImpl service = new NaturalLanguageSearchServiceImpl(searchService, memoryFacade);
        NaturalLanguageSearchRequest request = new NaturalLanguageSearchRequest();
        request.setQuery("帮我找上海跑步活动");
        request.setUserId(null);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute("userId", 888L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        try {
            when(memoryFacade.recallForPrompt(888L, "帮我找上海跑步活动")).thenReturn(null);
            when(memoryFacade.mergePrompt(eq("帮我找上海跑步活动"), isNull())).thenReturn("merged-query-2");
            SearchService.SearchResultVO searchResult = new SearchService.SearchResultVO();
            searchResult.setCriteria(new SearchCriteriaDTO());
            when(searchService.smartSearch(any())).thenReturn(searchResult);

            ArgumentCaptor<NaturalLanguageSearchRequest> requestCaptor = ArgumentCaptor.forClass(NaturalLanguageSearchRequest.class);
            NaturalLanguageSearchVO result = service.search(request);

            verify(searchService).smartSearch(requestCaptor.capture());
            assertEquals(888L, requestCaptor.getValue().getUserId());
            assertNotNull(result);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void search_shouldFailOpenWhenMemoryHookThrows() {
        NaturalLanguageSearchServiceImpl service = new NaturalLanguageSearchServiceImpl(searchService, memoryFacade);
        NaturalLanguageSearchRequest request = new NaturalLanguageSearchRequest();
        request.setUserId(321L);
        request.setQuery("帮我找广州桌游搭子");

        when(memoryFacade.recallForPrompt(321L, "帮我找广州桌游搭子")).thenThrow(new RuntimeException("recall down"));
        when(memoryFacade.mergePrompt("帮我找广州桌游搭子", null)).thenThrow(new RuntimeException("merge down"));
        doThrow(new RuntimeException("enqueue down"))
                .when(memoryFacade).enqueueConversation(eq(321L), eq("帮我找广州桌游搭子"), anyString());
        SearchService.SearchResultVO searchResult = new SearchService.SearchResultVO();
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setIntent("partner");
        searchResult.setCriteria(criteria);
        when(searchService.smartSearch(any())).thenReturn(searchResult);

        ArgumentCaptor<NaturalLanguageSearchRequest> requestCaptor = ArgumentCaptor.forClass(NaturalLanguageSearchRequest.class);
        NaturalLanguageSearchVO result = service.search(request);

        verify(searchService).smartSearch(requestCaptor.capture());
        assertEquals("帮我找广州桌游搭子", requestCaptor.getValue().getQuery());
        assertEquals("partner", result.getParsed().getIntent());
    }
}
