package com.sparklink.ai.chat;

import com.sparklink.dto.ChatCitationItem;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 将过滤后的联网检索结果转换为对话引用结构。
 */
@Component
public class CitationBuilder {

    public List<ChatCitationItem> build(List<SearchResultFilterService.FilteredItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .filter(Objects::nonNull)
                .map(item -> new ChatCitationItem(
                        item.title(),
                        item.url(),
                        item.domain(),
                        item.snippet(),
                        item.publishedAt()
                ))
                .toList();
    }
}
