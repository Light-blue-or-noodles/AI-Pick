package com.sparklink.memory.metrics;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 记忆模块埋点记录器：用于聚合关键计数指标并提供可测试快照。
 */
@Component
public class MemoryMetricsRecorder {

    private static final String UNKNOWN_REASON = "unknown";

    private final AtomicLong searchSuccessCount = new AtomicLong(0);
    private final AtomicLong addSuccessCount = new AtomicLong(0);
    private final AtomicLong retryCount = new AtomicLong(0);
    private final AtomicLong dlqCount = new AtomicLong(0);
    private final AtomicLong sensitiveFieldBlockCount = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> dlqReasonCounts = new ConcurrentHashMap<>();

    public void recordSearchSuccess() {
        searchSuccessCount.incrementAndGet();
    }

    public void recordAddSuccess() {
        addSuccessCount.incrementAndGet();
    }

    public void recordRetry() {
        retryCount.incrementAndGet();
    }

    public void recordDlq(String reason) {
        dlqCount.incrementAndGet();
        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : UNKNOWN_REASON;
        dlqReasonCounts.computeIfAbsent(normalizedReason, key -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordSensitiveFieldBlock() {
        sensitiveFieldBlockCount.incrementAndGet();
    }

    public Snapshot snapshot() {
        Map<String, Long> reasonSnapshot = new LinkedHashMap<>();
        dlqReasonCounts.forEach((key, value) -> reasonSnapshot.put(key, value.get()));
        return new Snapshot(
                searchSuccessCount.get(),
                addSuccessCount.get(),
                retryCount.get(),
                dlqCount.get(),
                sensitiveFieldBlockCount.get(),
                Collections.unmodifiableMap(reasonSnapshot)
        );
    }

    public record Snapshot(long searchSuccessCount,
                           long addSuccessCount,
                           long retryCount,
                           long dlqCount,
                           long sensitiveFieldBlockCount,
                           Map<String, Long> dlqReasonCounts) {
    }
}
