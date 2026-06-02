package com.sparklink.knowledge.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识检索结果。
 */
public class KnowledgeRetrieveResult {

    /**
     * 是否命中知识。
     */
    private boolean hit;

    /**
     * 是否发生降级。
     */
    private boolean fallback;

    /**
     * 降级原因。
     */
    private KnowledgeFallbackReason reason;

    /**
     * 检索记录。
     */
    private List<KnowledgeRecord> records = new ArrayList<>();

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }

    public KnowledgeFallbackReason getReason() {
        return reason;
    }

    public void setReason(KnowledgeFallbackReason reason) {
        this.reason = reason;
    }

    public List<KnowledgeRecord> getRecords() {
        return records;
    }

    public void setRecords(List<KnowledgeRecord> records) {
        this.records = records == null ? new ArrayList<>() : records;
    }
}
