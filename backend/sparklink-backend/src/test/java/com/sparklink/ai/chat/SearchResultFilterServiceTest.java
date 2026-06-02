package com.sparklink.ai.chat;

import com.sparklink.dto.ChatCitationItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SearchResultFilterServiceTest {

    private SearchResultFilterService service;
    private CitationBuilder builder;
    private WebSearchProperties properties;

    @BeforeEach
    void setUp() {
        properties = new WebSearchProperties();
        service = new SearchResultFilterService(properties);
        builder = new CitationBuilder();
    }

    @Test
    void shouldFilterByBlacklistAndDedupe() {
        properties.setDomainBlacklist(List.of("bad.com"));
        List<BailianWebSearchClient.RawResult> input = List.of(
                new BailianWebSearchClient.RawResult("A", "https://news.bad.com/a", "x", ""),
                new BailianWebSearchClient.RawResult("B", "https://ok.com/1", "first", ""),
                new BailianWebSearchClient.RawResult("B2", "https://ok.com/1", "dup", "")
        );

        SearchResultFilterService.FilteredResult output = service.filter(input);
        assertEquals(1, output.items().size());
        assertEquals("https://ok.com/1", output.items().get(0).url());
        assertEquals(2, output.filteredCount());
    }

    @Test
    void shouldAllowOnlyWhitelistWhenNotEmpty() {
        properties.setDomainWhitelist(List.of("trusted.com"));
        List<BailianWebSearchClient.RawResult> input = List.of(
                new BailianWebSearchClient.RawResult("A", "https://trusted.com/a", "ok", ""),
                new BailianWebSearchClient.RawResult("B", "https://other.com/b", "drop", "")
        );

        SearchResultFilterService.FilteredResult output = service.filter(input);
        assertEquals(1, output.items().size());
        assertEquals("trusted.com", output.items().get(0).domain());
        assertEquals(1, output.filteredCount());
    }

    @Test
    void shouldPrioritizeBlacklistOverWhitelist() {
        properties.setDomainWhitelist(List.of("trusted.com"));
        properties.setDomainBlacklist(List.of("trusted.com"));
        List<BailianWebSearchClient.RawResult> input = List.of(
                new BailianWebSearchClient.RawResult("A", "https://trusted.com/a", "x", "")
        );

        SearchResultFilterService.FilteredResult output = service.filter(input);
        assertEquals(0, output.items().size());
        assertEquals(1, output.filteredCount());
    }

    @Test
    void shouldTruncateSnippetToConfiguredLimit() {
        String longSnippet = "a".repeat(200);
        List<BailianWebSearchClient.RawResult> input = List.of(
                new BailianWebSearchClient.RawResult("A", "https://ok.com/a", longSnippet, "2026-05-31")
        );

        SearchResultFilterService.FilteredResult output = service.filter(input);
        assertEquals(1, output.items().size());
        assertEquals(180, output.items().get(0).snippet().length());
        assertEquals(0, output.filteredCount());
    }

    @Test
    void shouldBuildCitationItemsFromFilteredItems() {
        List<ChatCitationItem> citations = builder.build(List.of(
                new SearchResultFilterService.FilteredItem("标题", "https://ok.com/a", "ok.com", "摘要", "")
        ));
        assertEquals(1, citations.size());
        assertEquals("ok.com", citations.get(0).getDomain());
        assertFalse(citations.get(0).getUrl().isBlank());
    }

    @Test
    void shouldDedupeUrlWithCaseVariantAndSchemeVariant() {
        List<BailianWebSearchClient.RawResult> input = List.of(
                new BailianWebSearchClient.RawResult("A", "HTTP://Example.COM/path?a=1", "x", ""),
                new BailianWebSearchClient.RawResult("B", "https://example.com/path?a=1", "y", "")
        );

        SearchResultFilterService.FilteredResult output = service.filter(input);
        assertEquals(1, output.items().size());
        assertEquals(1, output.filteredCount());
    }

    @Test
    void shouldIgnoreNullItemWhenBuildingCitations() {
        List<SearchResultFilterService.FilteredItem> input = new ArrayList<>();
        input.add(new SearchResultFilterService.FilteredItem("标题", "https://ok.com/a", "ok.com", "摘要", ""));
        input.add(null);

        List<ChatCitationItem> citations = builder.build(input);
        assertEquals(1, citations.size());
        assertEquals("ok.com", citations.get(0).getDomain());
    }

    @Test
    void shouldFilterInvalidOrEmptyUrlAndCountFilteredItems() {
        List<BailianWebSearchClient.RawResult> input = List.of(
                new BailianWebSearchClient.RawResult("A", "", "x", ""),
                new BailianWebSearchClient.RawResult("B", "not-a-url", "y", ""),
                new BailianWebSearchClient.RawResult("C", "   ", "z", "")
        );

        SearchResultFilterService.FilteredResult output = service.filter(input);
        assertEquals(0, output.items().size());
        assertEquals(3, output.filteredCount());
    }
}
