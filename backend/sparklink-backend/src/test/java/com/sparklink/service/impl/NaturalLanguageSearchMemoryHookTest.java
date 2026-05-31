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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
}
