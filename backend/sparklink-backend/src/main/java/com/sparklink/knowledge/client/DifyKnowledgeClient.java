package com.sparklink.knowledge.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sparklink.knowledge.config.DifyKnowledgeProperties;
import com.sparklink.knowledge.model.KnowledgeRecord;
import com.sparklink.knowledge.model.KnowledgeRetrieveRequest;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Dify 知识检索客户端。
 */
@Component
public class DifyKnowledgeClient {

    private static final int MAX_QUERY_LENGTH = 250;

    private final RestTemplate restTemplate;
    private final DifyKnowledgeProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    public DifyKnowledgeClient(RestTemplateBuilder restTemplateBuilder,
                               DifyKnowledgeProperties properties,
                               ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    DifyKnowledgeClient(RestTemplate restTemplate, DifyKnowledgeProperties properties) {
        this(restTemplate, properties, new ObjectMapper());
    }

    DifyKnowledgeClient(RestTemplate restTemplate,
                        DifyKnowledgeProperties properties,
                        ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 Dify 数据集检索接口并映射为领域结果。
     */
    public KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
        String payload = buildPayload(request == null ? null : request.getQuery());
        String url = buildRetrieveUrl();
        HttpEntity<String> entity = new HttpEntity<>(payload, buildHeaders());
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return parseResponse(response.getBody());
        } catch (KnowledgeClientException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new KnowledgeClientException(
                    KnowledgeClientException.ErrorType.HTTP_ERROR,
                    "Dify retrieve request failed",
                    ex);
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(normalizeApiKey(properties.getApiKey()));
        return headers;
    }

    private String normalizeApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        return apiKey.replaceFirst("^Bearer\\s+", "").trim();
    }

    private String buildRetrieveUrl() {
        String baseUrl = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBase + "/datasets/" + properties.getDatasetId() + "/retrieve";
    }

    private String buildPayload(String query) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("query", truncateQuery(query));

        DifyKnowledgeProperties.Retrieve retrieve = properties.getRetrieve();
        ObjectNode retrievalModel = root.putObject("retrieval_model");
        retrievalModel.put("top_k", retrieve.getTopK());
        retrievalModel.put("score_threshold_enabled", retrieve.isThresholdEnabled());
        retrievalModel.put("score_threshold", retrieve.getScoreThreshold());
        retrievalModel.put("search_method", retrieve.getSearchMethod());
        retrievalModel.put("reranking_enable", retrieve.isRerankingEnable());

        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new KnowledgeClientException(
                    KnowledgeClientException.ErrorType.SERIALIZE_ERROR,
                    "Failed to serialize dify retrieve payload",
                    ex);
        }
    }

    private String truncateQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }
        String normalized = query.trim();
        if (normalized.length() <= MAX_QUERY_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_QUERY_LENGTH);
    }

    private KnowledgeRetrieveResult parseResponse(String responseBody) {
        KnowledgeRetrieveResult result = new KnowledgeRetrieveResult();
        if (!StringUtils.hasText(responseBody)) {
            result.setHit(false);
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode recordsNode = root.path("records");
            if (!recordsNode.isArray()) {
                throw new KnowledgeClientException(
                        KnowledgeClientException.ErrorType.PARSE_ERROR,
                        "Dify response records is not an array",
                        null);
            }

            List<KnowledgeRecord> records = new ArrayList<>();
            for (JsonNode itemNode : recordsNode) {
                JsonNode segmentNode = itemNode.path("segment");
                if (!segmentNode.isObject()) {
                    continue;
                }
                String content = segmentNode.path("content").asText("");
                if (!StringUtils.hasText(content)) {
                    continue;
                }
                KnowledgeRecord record = new KnowledgeRecord();
                record.setSegmentId(segmentNode.path("id").asText(""));
                record.setDocumentId(segmentNode.path("document_id").asText(""));
                record.setContent(content);
                record.setScore(itemNode.path("score").isNumber() ? itemNode.path("score").asDouble() : null);
                records.add(record);
            }
            result.setRecords(records);
            result.setHit(!records.isEmpty());
            return result;
        } catch (KnowledgeClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new KnowledgeClientException(
                    KnowledgeClientException.ErrorType.PARSE_ERROR,
                    "Failed to parse dify retrieve response",
                    ex);
        }
    }
}
