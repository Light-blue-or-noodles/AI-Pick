package com.sparklink.knowledge;

import com.sparklink.knowledge.client.DifyKnowledgeClient;
import com.sparklink.knowledge.metrics.KnowledgeMetricsRecorder;
import com.sparklink.knowledge.model.KnowledgeFallbackReason;
import com.sparklink.knowledge.model.KnowledgeRecord;
import com.sparklink.knowledge.model.KnowledgeRetrieveRequest;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import com.sparklink.knowledge.service.KnowledgeRetrieveGateway;
import com.sparklink.knowledge.service.PromptKnowledgeAssembler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeE2ETest {

    @Test
    void shouldRecordSuccessAndInjectKnowledgeIntoPrompt() {
        DifyKnowledgeClient client = mock(DifyKnowledgeClient.class);
        when(client.retrieve(any())).thenReturn(mockSuccessResult());

        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(client, true);
        gateway.setApiKey("mock-api-key");
        gateway.setDatasetId("mock-dataset-id");
        PromptKnowledgeAssembler assembler = new PromptKnowledgeAssembler(3, 100);
        KnowledgeMetricsRecorder metricsRecorder = new KnowledgeMetricsRecorder();

        Long userId = 1001L;
        String query = "如何办理签到";
        KnowledgeRetrieveResult retrieveResult = gateway.retrieve(userId, query);
        metricsRecorder.record(retrieveResult);
        String mergedPrompt = assembler.merge(query, retrieveResult);

        ArgumentCaptor<KnowledgeRetrieveRequest> requestCaptor = ArgumentCaptor.forClass(KnowledgeRetrieveRequest.class);
        verify(client).retrieve(requestCaptor.capture());
        assertEquals(userId, requestCaptor.getValue().getUserId());
        assertEquals(query, requestCaptor.getValue().getQuery());

        assertTrue(retrieveResult.isHit());
        assertFalse(retrieveResult.isFallback());
        assertTrue(mergedPrompt.contains("【知识参考】"));
        assertTrue(mergedPrompt.contains("签到可在活动详情页完成"));

        KnowledgeMetricsRecorder.Snapshot snapshot = metricsRecorder.snapshot();
        assertEquals(1L, snapshot.getSuccessCount());
        assertEquals(0L, snapshot.getFallbackCount());
        assertEquals("UNKNOWN", snapshot.getLastFallbackReason());
    }

    @Test
    void shouldRecordFallbackWhenSearchFailsAndKeepOriginalPrompt() {
        DifyKnowledgeClient client = mock(DifyKnowledgeClient.class);
        when(client.retrieve(any())).thenThrow(new RuntimeException("network failed"));

        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(client, true);
        gateway.setApiKey("mock-api-key");
        gateway.setDatasetId("mock-dataset-id");
        PromptKnowledgeAssembler assembler = new PromptKnowledgeAssembler(3, 100);
        KnowledgeMetricsRecorder metricsRecorder = new KnowledgeMetricsRecorder();

        Long userId = 1001L;
        String query = "知识检索失败场景";
        KnowledgeRetrieveResult retrieveResult = gateway.retrieve(userId, query);
        metricsRecorder.record(retrieveResult);
        String mergedPrompt = assembler.merge(query, retrieveResult);

        ArgumentCaptor<KnowledgeRetrieveRequest> requestCaptor = ArgumentCaptor.forClass(KnowledgeRetrieveRequest.class);
        verify(client).retrieve(requestCaptor.capture());
        assertEquals(userId, requestCaptor.getValue().getUserId());
        assertEquals(query, requestCaptor.getValue().getQuery());

        assertFalse(retrieveResult.isHit());
        assertTrue(retrieveResult.isFallback());
        assertEquals(KnowledgeFallbackReason.SEARCH_FAILED, retrieveResult.getReason());
        assertEquals(query, mergedPrompt);

        KnowledgeMetricsRecorder.Snapshot snapshot = metricsRecorder.snapshot();
        assertEquals(0L, snapshot.getSuccessCount());
        assertEquals(1L, snapshot.getFallbackCount());
        assertEquals("SEARCH_FAILED", snapshot.getLastFallbackReason());
    }

    private KnowledgeRetrieveResult mockSuccessResult() {
        KnowledgeRecord record = new KnowledgeRecord();
        record.setSegmentId("seg-001");
        record.setDocumentId("doc-001");
        record.setContent("签到可在活动详情页完成，若异常请刷新重试。");
        record.setScore(0.98D);

        KnowledgeRetrieveResult result = new KnowledgeRetrieveResult();
        result.setHit(true);
        result.setFallback(false);
        result.setRecords(List.of(record));
        return result;
    }
}
