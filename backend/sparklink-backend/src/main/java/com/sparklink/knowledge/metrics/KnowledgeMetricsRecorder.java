package com.sparklink.knowledge.metrics;

import com.sparklink.knowledge.model.KnowledgeFallbackReason;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import org.springframework.stereotype.Component;

/**
 * 知识检索指标记录器（最小实现）。
 */
@Component
public class KnowledgeMetricsRecorder {

    private static final String UNKNOWN_REASON = "UNKNOWN";

    private final Object lock = new Object();
    private long successCount;
    private long fallbackCount;
    private String lastFallbackReason = UNKNOWN_REASON;

    public void record(KnowledgeRetrieveResult retrieveResult) {
        if (retrieveResult == null) {
            recordFallback(null);
            return;
        }
        if (retrieveResult.isFallback()) {
            recordFallback(retrieveResult.getReason());
            return;
        }
        if (retrieveResult.isHit()) {
            recordSuccess();
            return;
        }
        recordFallback(retrieveResult.getReason());
    }

    public void recordSuccess() {
        synchronized (lock) {
            successCount++;
        }
    }

    public void recordFallback(KnowledgeFallbackReason reason) {
        synchronized (lock) {
            fallbackCount++;
            lastFallbackReason = reason == null ? UNKNOWN_REASON : reason.name();
        }
    }

    public Snapshot snapshot() {
        synchronized (lock) {
            return new Snapshot(successCount, fallbackCount, lastFallbackReason);
        }
    }

    public static final class Snapshot {

        private final long successCount;
        private final long fallbackCount;
        private final String lastFallbackReason;

        public Snapshot(long successCount, long fallbackCount, String lastFallbackReason) {
            this.successCount = successCount;
            this.fallbackCount = fallbackCount;
            this.lastFallbackReason = lastFallbackReason;
        }

        public long getSuccessCount() {
            return successCount;
        }

        public long getFallbackCount() {
            return fallbackCount;
        }

        public String getLastFallbackReason() {
            return lastFallbackReason;
        }
    }
}
