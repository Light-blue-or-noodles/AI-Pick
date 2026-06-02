package com.sparklink.knowledge.service;

import com.sparklink.knowledge.config.DifyKnowledgeProperties;
import com.sparklink.knowledge.model.KnowledgeRecord;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将知识检索结果拼装到用户问题中。
 */
@Component
public class PromptKnowledgeAssembler {

    private final int maxInjectRecords;
    private final int maxInjectCharsPerRecord;

    @Autowired
    public PromptKnowledgeAssembler(DifyKnowledgeProperties properties) {
        this(properties.getRetrieve().getMaxInjectRecords(),
                properties.getRetrieve().getMaxInjectCharsPerRecord());
    }

    public PromptKnowledgeAssembler(int maxInjectRecords, int maxInjectCharsPerRecord) {
        if (maxInjectRecords <= 0) {
            throw new IllegalArgumentException("maxInjectRecords must be greater than 0");
        }
        if (maxInjectCharsPerRecord <= 0) {
            throw new IllegalArgumentException("maxInjectCharsPerRecord must be greater than 0");
        }
        this.maxInjectRecords = maxInjectRecords;
        this.maxInjectCharsPerRecord = maxInjectCharsPerRecord;
    }

    public String merge(String userMessage, KnowledgeRetrieveResult retrieveResult) {
        String originalMessage = userMessage == null ? "" : userMessage;
        List<KnowledgeRecord> records = retrieveResult == null ? List.of() : retrieveResult.getRecords();
        if (records == null || records.isEmpty()) {
            return originalMessage;
        }

        List<KnowledgeRecord> sortedRecords = new ArrayList<>();
        for (KnowledgeRecord record : records) {
            if (record != null) {
                sortedRecords.add(record);
            }
        }
        sortedRecords.sort(Comparator.comparing(
                        KnowledgeRecord::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        List<String> injectedContents = new ArrayList<>();
        Set<String> seenSegmentIds = new LinkedHashSet<>();
        for (KnowledgeRecord record : sortedRecords) {
            String content = trim(record.getContent());
            if (content.isEmpty()) {
                continue;
            }
            String segmentId = trim(record.getSegmentId());
            if (!segmentId.isEmpty() && !seenSegmentIds.add(segmentId)) {
                continue;
            }

            injectedContents.add(truncate(content, maxInjectCharsPerRecord));
            if (injectedContents.size() >= maxInjectRecords) {
                break;
            }
        }

        if (injectedContents.isEmpty()) {
            return originalMessage;
        }

        StringBuilder prompt = new StringBuilder(originalMessage)
                .append("\n\n【知识参考】");
        for (int index = 0; index < injectedContents.size(); index++) {
            prompt.append("\n")
                    .append(index + 1)
                    .append(". ")
                    .append(injectedContents.get(index));
        }
        return prompt.toString();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int limit) {
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
