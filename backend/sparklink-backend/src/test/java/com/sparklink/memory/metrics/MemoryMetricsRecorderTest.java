package com.sparklink.memory.metrics;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryMetricsRecorderTest {

    @Test
    void shouldRecordAllMetricsIntoSnapshot() {
        MemoryMetricsRecorder recorder = new MemoryMetricsRecorder();

        recorder.recordSearchSuccess();
        recorder.recordAddSuccess();
        recorder.recordRetry();
        recorder.recordRetry();
        recorder.recordDlq("timeout");
        recorder.recordDlq("timeout");
        recorder.recordDlq("invalid_payload");
        recorder.recordSensitiveFieldBlock();

        MemoryMetricsRecorder.Snapshot snapshot = recorder.snapshot();
        assertEquals(1L, snapshot.searchSuccessCount());
        assertEquals(1L, snapshot.addSuccessCount());
        assertEquals(2L, snapshot.retryCount());
        assertEquals(3L, snapshot.dlqCount());
        assertEquals(1L, snapshot.sensitiveFieldBlockCount());
        assertEquals(2L, snapshot.dlqReasonCounts().get("timeout"));
        assertEquals(1L, snapshot.dlqReasonCounts().get("invalid_payload"));
    }

    @Test
    void shouldNormalizeBlankDlqReason() {
        MemoryMetricsRecorder recorder = new MemoryMetricsRecorder();

        recorder.recordDlq(" ");

        Map<String, Long> dlqReasonCounts = recorder.snapshot().dlqReasonCounts();
        assertEquals(1L, dlqReasonCounts.get("unknown"));
    }
}
