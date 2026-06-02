package com.sparklink.knowledge.service;

import com.sparklink.knowledge.client.DifyKnowledgeClient;
import com.sparklink.knowledge.metrics.KnowledgeMetricsRecorder;
import com.sparklink.knowledge.model.KnowledgeFallbackReason;
import com.sparklink.knowledge.model.KnowledgeRetrieveRequest;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 知识检索网关：封装开关控制与异常降级。
 */
@Component
public class KnowledgeRetrieveGateway {

    private final DifyKnowledgeClient difyKnowledgeClient;
    private final boolean enabled;
    private KnowledgeMetricsRecorder knowledgeMetricsRecorder;
    private String apiKey = "";
    private String datasetId = "";

    public KnowledgeRetrieveGateway(
            DifyKnowledgeClient difyKnowledgeClient,
            @Value("${app.dify.enabled:true}") boolean enabled) {
        this.difyKnowledgeClient = Objects.requireNonNull(difyKnowledgeClient, "difyKnowledgeClient must not be null");
        this.enabled = enabled;
    }

    /**
     * 执行知识检索并在必要时返回降级结果。
     */
    public KnowledgeRetrieveResult retrieve(Long userId, String query) {
        if (!enabled || !StringUtils.hasText(query)) {
            return fallback(KnowledgeFallbackReason.NOT_TRIGGERED);
        }
        if (!hasCredentials()) {
            return fallback(KnowledgeFallbackReason.SEARCH_FAILED);
        }

        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setUserId(userId);
        request.setQuery(query);
        try {
            KnowledgeRetrieveResult result = difyKnowledgeClient.retrieve(request);
            if (result == null) {
                return fallback(KnowledgeFallbackReason.SEARCH_FAILED);
            }
            recordSuccess(result);
            return result;
        } catch (RuntimeException ex) {
            return fallback(KnowledgeFallbackReason.SEARCH_FAILED);
        }
    }

    @Autowired(required = false)
    public void setKnowledgeMetricsRecorder(KnowledgeMetricsRecorder knowledgeMetricsRecorder) {
        this.knowledgeMetricsRecorder = knowledgeMetricsRecorder;
    }

    @Value("${app.dify.api-key:}")
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Value("${app.dify.dataset-id:}")
    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId == null ? "" : datasetId.trim();
    }

    private boolean hasCredentials() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(datasetId);
    }

    private KnowledgeRetrieveResult fallback(KnowledgeFallbackReason reason) {
        if (knowledgeMetricsRecorder != null) {
            knowledgeMetricsRecorder.recordFallback(reason);
        }
        KnowledgeRetrieveResult result = new KnowledgeRetrieveResult();
        result.setHit(false);
        result.setFallback(true);
        result.setReason(reason);
        return result;
    }

    private void recordSuccess(KnowledgeRetrieveResult result) {
        if (knowledgeMetricsRecorder == null) {
            return;
        }
        if (result.isFallback()) {
            knowledgeMetricsRecorder.recordFallback(result.getReason());
            return;
        }
        knowledgeMetricsRecorder.recordSuccess();
    }
}
