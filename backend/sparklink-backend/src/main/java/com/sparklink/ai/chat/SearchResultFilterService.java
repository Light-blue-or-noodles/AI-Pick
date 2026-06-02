package com.sparklink.ai.chat;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 对搜索结果做域名过滤、URL 去重与摘要裁剪。
 */
@Component
public class SearchResultFilterService {

    private static final int MAX_SNIPPET_LENGTH = 180;

    private final WebSearchProperties webSearchProperties;

    public SearchResultFilterService(WebSearchProperties webSearchProperties) {
        this.webSearchProperties = webSearchProperties;
    }

    public FilteredResult filter(List<BailianWebSearchClient.RawResult> rawResults) {
        if (rawResults == null || rawResults.isEmpty()) {
            return new FilteredResult(Collections.emptyList(), 0);
        }

        List<FilteredItem> items = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        int filteredCount = 0;
        for (BailianWebSearchClient.RawResult rawResult : rawResults) {
            if (rawResult == null) {
                filteredCount++;
                continue;
            }

            String normalizedUrl = normalizeText(rawResult.url());
            String canonicalUrlKey = buildCanonicalUrlKey(normalizedUrl);
            if (canonicalUrlKey.isEmpty() || !seenUrls.add(canonicalUrlKey)) {
                filteredCount++;
                continue;
            }

            String domain = extractDomain(normalizedUrl);
            if (domain.isEmpty() || !allowed(domain)) {
                filteredCount++;
                continue;
            }

            items.add(new FilteredItem(
                    normalizeText(rawResult.title()),
                    normalizedUrl,
                    domain,
                    truncateSnippet(rawResult.snippet()),
                    normalizeText(rawResult.publishedAt())
            ));
        }
        return new FilteredResult(items, filteredCount);
    }

    boolean allowed(String domain) {
        Set<String> blacklist = normalizeDomainRules(webSearchProperties.getDomainBlacklist());
        if (matchesAnyRule(domain, blacklist)) {
            return false;
        }

        Set<String> whitelist = normalizeDomainRules(webSearchProperties.getDomainWhitelist());
        return whitelist.isEmpty() || matchesAnyRule(domain, whitelist);
    }

    private boolean matchesAnyRule(String domain, Set<String> rules) {
        for (String rule : rules) {
            if (domain.equals(rule) || domain.endsWith("." + rule)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> normalizeDomainRules(List<String> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> normalized = new HashSet<>();
        for (String rule : rules) {
            String normalizedRule = normalizeDomain(rule);
            if (!normalizedRule.isEmpty()) {
                normalized.add(normalizedRule);
            }
        }
        return normalized;
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "";
            }
            return normalizeDomain(host);
        } catch (Exception ex) {
            return "";
        }
    }

    private String buildCanonicalUrlKey(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url.trim());
            String host = normalizeDomain(uri.getHost());
            if (host.isEmpty()) {
                return "";
            }
            // 统一 http/https 变体，避免同一资源被重复计入。
            String scheme = "https";
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return scheme + "://" + host + path;
            }
            return scheme + "://" + host + path + "?" + query;
        } catch (Exception ex) {
            return "";
        }
    }

    private String normalizeDomain(String domain) {
        if (domain == null || domain.isBlank()) {
            return "";
        }
        String normalized = domain.trim().toLowerCase(Locale.ROOT);
        int schemeIndex = normalized.indexOf("://");
        if (schemeIndex >= 0) {
            normalized = normalized.substring(schemeIndex + 3);
        }
        int slashIndex = normalized.indexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(0, slashIndex);
        }
        int colonIndex = normalized.indexOf(':');
        if (colonIndex >= 0) {
            normalized = normalized.substring(0, colonIndex);
        }
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.startsWith("www.")) {
            normalized = normalized.substring(4);
        }
        return normalized;
    }

    private String truncateSnippet(String snippet) {
        String normalized = normalizeText(snippet);
        if (normalized.length() <= MAX_SNIPPET_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_SNIPPET_LENGTH).trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.trim();
    }

    public record FilteredItem(String title, String url, String domain, String snippet, String publishedAt) {
    }

    public record FilteredResult(List<FilteredItem> items, int filteredCount) {
    }
}
