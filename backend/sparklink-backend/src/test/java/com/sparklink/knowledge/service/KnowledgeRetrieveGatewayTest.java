package com.sparklink.knowledge.service;

import com.sparklink.knowledge.client.DifyKnowledgeClient;
import com.sparklink.knowledge.client.KnowledgeClientException;
import com.sparklink.knowledge.metrics.KnowledgeMetricsRecorder;
import com.sparklink.knowledge.model.KnowledgeRetrieveRequest;
import com.sparklink.knowledge.model.KnowledgeFallbackReason;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeRetrieveGatewayTest {

    @Mock
    private DifyKnowledgeClient difyKnowledgeClient;

    @Mock
    private KnowledgeMetricsRecorder metricsRecorder;

    @Test
    void shouldFallbackWhenDisabled() {
        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(difyKnowledgeClient, false);
        gateway.setKnowledgeMetricsRecorder(metricsRecorder);

        KnowledgeRetrieveResult result = gateway.retrieve(1001L, "查询内容");

        assertTrue(result.isFallback());
        assertSame(KnowledgeFallbackReason.NOT_TRIGGERED, result.getReason());
        assertFalse(result.isHit());
        verify(difyKnowledgeClient, never()).retrieve(any());
        verify(metricsRecorder).recordFallback(KnowledgeFallbackReason.NOT_TRIGGERED);
    }

    @Test
    void shouldFallbackWhenBlankQuery() {
        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(difyKnowledgeClient, true);
        gateway.setKnowledgeMetricsRecorder(metricsRecorder);

        KnowledgeRetrieveResult result = gateway.retrieve(1001L, "   ");

        assertTrue(result.isFallback());
        assertSame(KnowledgeFallbackReason.NOT_TRIGGERED, result.getReason());
        assertFalse(result.isHit());
        verify(difyKnowledgeClient, never()).retrieve(any());
        verify(metricsRecorder).recordFallback(KnowledgeFallbackReason.NOT_TRIGGERED);
    }

    @Test
    void shouldReturnClientResultWhenSuccess() {
        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(difyKnowledgeClient, true);
        gateway.setKnowledgeMetricsRecorder(metricsRecorder);
        gateway.setApiKey("mock-api-key");
        gateway.setDatasetId("mock-dataset-id");
        KnowledgeRetrieveResult expected = new KnowledgeRetrieveResult();
        expected.setHit(true);
        when(difyKnowledgeClient.retrieve(any())).thenReturn(expected);

        Long userId = 1001L;
        String query = "有效查询";
        KnowledgeRetrieveResult result = gateway.retrieve(userId, query);

        assertSame(expected, result);
        ArgumentCaptor<KnowledgeRetrieveRequest> requestCaptor = ArgumentCaptor.forClass(KnowledgeRetrieveRequest.class);
        verify(difyKnowledgeClient).retrieve(requestCaptor.capture());
        assertEquals(userId, requestCaptor.getValue().getUserId());
        assertEquals(query, requestCaptor.getValue().getQuery());
        verify(metricsRecorder).recordSuccess();
    }

    @Test
    void shouldFallbackWhenClientThrows() {
        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(difyKnowledgeClient, true);
        gateway.setKnowledgeMetricsRecorder(metricsRecorder);
        gateway.setApiKey("mock-api-key");
        gateway.setDatasetId("mock-dataset-id");
        when(difyKnowledgeClient.retrieve(any())).thenThrow(
                new KnowledgeClientException(KnowledgeClientException.ErrorType.HTTP_ERROR, "mock error", null));

        KnowledgeRetrieveResult result = gateway.retrieve(1001L, "有效查询");

        assertTrue(result.isFallback());
        assertSame(KnowledgeFallbackReason.SEARCH_FAILED, result.getReason());
        assertFalse(result.isHit());
        verify(difyKnowledgeClient).retrieve(any());
        verify(metricsRecorder).recordFallback(KnowledgeFallbackReason.SEARCH_FAILED);
    }

    @Test
    void shouldFallbackWhenClientReturnsNull() {
        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(difyKnowledgeClient, true);
        gateway.setKnowledgeMetricsRecorder(metricsRecorder);
        gateway.setApiKey("mock-api-key");
        gateway.setDatasetId("mock-dataset-id");
        when(difyKnowledgeClient.retrieve(any())).thenReturn(null);

        KnowledgeRetrieveResult result = gateway.retrieve(1001L, "null响应");

        assertTrue(result.isFallback());
        assertSame(KnowledgeFallbackReason.SEARCH_FAILED, result.getReason());
        assertFalse(result.isHit());
        verify(difyKnowledgeClient).retrieve(any());
        verify(metricsRecorder).recordFallback(KnowledgeFallbackReason.SEARCH_FAILED);
    }

    @Test
    void shouldFallbackWhenRuntimeException() {
        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(difyKnowledgeClient, true);
        gateway.setKnowledgeMetricsRecorder(metricsRecorder);
        gateway.setApiKey("mock-api-key");
        gateway.setDatasetId("mock-dataset-id");
        when(difyKnowledgeClient.retrieve(any())).thenThrow(new RuntimeException("runtime boom"));

        KnowledgeRetrieveResult result = gateway.retrieve(1001L, "有效查询");

        assertTrue(result.isFallback());
        assertSame(KnowledgeFallbackReason.SEARCH_FAILED, result.getReason());
        assertFalse(result.isHit());
        verify(difyKnowledgeClient).retrieve(any());
        verify(metricsRecorder).recordFallback(KnowledgeFallbackReason.SEARCH_FAILED);
    }

    @Test
    void shouldFallbackWhenCredentialMissingAndNotCallClient() {
        KnowledgeRetrieveGateway gateway = new KnowledgeRetrieveGateway(difyKnowledgeClient, true);
        gateway.setKnowledgeMetricsRecorder(metricsRecorder);
        gateway.setApiKey("");
        gateway.setDatasetId("dataset-id");

        KnowledgeRetrieveResult result = gateway.retrieve(1001L, "有效查询");

        assertTrue(result.isFallback());
        assertSame(KnowledgeFallbackReason.SEARCH_FAILED, result.getReason());
        assertFalse(result.isHit());
        verify(difyKnowledgeClient, never()).retrieve(any());
        verify(metricsRecorder).recordFallback(KnowledgeFallbackReason.SEARCH_FAILED);
    }
}
