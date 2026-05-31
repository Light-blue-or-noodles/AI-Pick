package com.sparklink.memory.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sparklink.memory.config.MemoryLibraryProperties;
import com.sparklink.memory.model.MemoryContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 记忆库客户端：负责构建与调用 Search / Add API。
 *
 * @author AI-Pick
 */
@Component
public class MemoryLibraryClient {

    private final MemoryLibraryProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public MemoryLibraryClient(MemoryLibraryProperties properties) {
        this(properties, new ObjectMapper(), new RestTemplate());
    }

    public MemoryLibraryClient(MemoryLibraryProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, new RestTemplate());
    }

    public MemoryLibraryClient(MemoryLibraryProperties properties, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    public ObjectNode buildSearchPayload(String userId, String query) {
        Assert.hasText(userId, "userId 不能为空");
        Assert.hasText(query, "query 不能为空");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("user_id", userId);
        payload.put("query", query);
        payload.set("knowledgebase_ids", buildKnowledgebaseIdsNode());
        payload.put("max_results", properties.getMaxResults() == null ? 8 : properties.getMaxResults());
        if (properties.getSimilarityThreshold() != null) {
            payload.put("similarity_threshold", properties.getSimilarityThreshold());
        }
        return payload;
    }

    public ObjectNode buildAddPayload(String userId, List<Map<String, String>> messages) {
        Assert.hasText(userId, "userId 不能为空");
        Assert.notNull(messages, "messages 不能为空");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("user_id", userId);
        payload.set("knowledgebase_ids", buildKnowledgebaseIdsNode());

        ArrayNode messageNodes = payload.putArray("messages");
        for (Map<String, String> message : messages) {
            String role = requireMessageField(message, "role");
            String content = requireMessageField(message, "content");
            ObjectNode messageNode = messageNodes.addObject();
            messageNode.put("role", role);
            messageNode.put("content", content);
        }
        return payload;
    }

    public MemoryContext searchMemory(String memoryUserId, String query) {
        ensureEnabled();
        JsonNode dataNode = invokeAndExtractData(buildSearchUrl(), buildSearchPayload(memoryUserId, query));
        List<String> snippets = extractMemorySnippets(dataNode);
        Map<String, String> profile = extractProfileAttributes(dataNode);
        return MemoryContext.of(snippets, profile);
    }

    public void addMemory(String memoryUserId, List<Map<String, String>> messages) {
        ensureEnabled();
        invokeAndExtractData(buildAddUrl(), buildAddPayload(memoryUserId, messages));
    }

    private String requireMessageField(Map<String, String> message, String fieldName) {
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("message 不能为空");
        }
        String fieldValue = message.get(fieldName);
        if (!StringUtils.hasText(fieldValue)) {
            throw new IllegalArgumentException("message." + fieldName + " 不能为空");
        }
        return fieldValue;
    }

    private ArrayNode buildKnowledgebaseIdsNode() {
        Assert.notNull(properties, "memoryLibraryProperties 不能为空");
        String knowledgebaseId = properties.getKnowledgebaseId();
        Assert.isTrue(StringUtils.hasText(knowledgebaseId), "knowledgebaseId 不能为空");

        ArrayNode knowledgebaseIds = objectMapper.createArrayNode();
        knowledgebaseIds.add(knowledgebaseId);
        return knowledgebaseIds;
    }

    private JsonNode invokeAndExtractData(String url, ObjectNode payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(readApiKey());
        HttpEntity<String> entity = new HttpEntity<>(payload.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("memory api call failed, status=" + response.getStatusCode().value());
        }
        String body = response.getBody();
        if (!StringUtils.hasText(body)) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.path("data");
            if (!data.isMissingNode() && !data.isNull()) {
                return data;
            }
            return root;
        } catch (Exception ex) {
            throw new IllegalStateException("memory api response parse failed", ex);
        }
    }

    private List<String> extractMemorySnippets(JsonNode dataNode) {
        ArrayNode snippets = objectMapper.createArrayNode();
        appendStringField(dataNode.path("memory_nodes"), snippets, "content");
        appendStringField(dataNode.path("memory_nodes"), snippets, "custom_content");
        appendStringField(dataNode.path("memory_detail_list"), snippets, "memory_value");
        appendStringField(dataNode.path("memory_detail_list"), snippets, "content");
        appendStringField(dataNode.path("memories"), snippets, "content");
        return objectMapper.convertValue(snippets, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private Map<String, String> extractProfileAttributes(JsonNode dataNode) {
        Map<String, String> profile = new LinkedHashMap<>();
        collectProfileFromNode(dataNode.path("profile"), profile);
        collectProfileFromNode(dataNode.path("user_profile"), profile);
        JsonNode profileDetailList = dataNode.path("profile_detail_list");
        if (profileDetailList.isArray()) {
            for (JsonNode item : profileDetailList) {
                String key = readFirstText(item, "name", "field_name", "attribute_name");
                String value = readFirstText(item, "value", "field_value", "attribute_value");
                if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                    profile.put(key.trim(), value.trim());
                }
            }
        }
        return profile;
    }

    private void collectProfileFromNode(JsonNode profileNode, Map<String, String> profile) {
        if (profileNode == null || profileNode.isMissingNode() || profileNode.isNull()) {
            return;
        }
        JsonNode attributes = profileNode.path("attributes");
        if (!attributes.isArray()) {
            return;
        }
        for (JsonNode item : attributes) {
            String name = readFirstText(item, "name");
            String value = readFirstText(item, "value");
            if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
                profile.put(name.trim(), value.trim());
            }
        }
    }

    private void appendStringField(JsonNode arrayNode, ArrayNode snippets, String fieldName) {
        if (!arrayNode.isArray()) {
            return;
        }
        for (JsonNode item : arrayNode) {
            String value = item.path(fieldName).asText(null);
            if (StringUtils.hasText(value)) {
                snippets.add(value.trim());
            }
        }
    }

    private String readFirstText(JsonNode node, String... candidateFields) {
        for (String field : candidateFields) {
            String value = node.path(field).asText(null);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String buildSearchUrl() {
        return normalizeBaseUrl() + "/search";
    }

    private String buildAddUrl() {
        return normalizeBaseUrl() + "/add";
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        Assert.isTrue(StringUtils.hasText(baseUrl), "memory.library.base-url 不能为空");
        String trimmed = baseUrl.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String readApiKey() {
        String apiKey = properties.getApiKey();
        Assert.isTrue(StringUtils.hasText(apiKey), "memory.library.api-key 不能为空");
        return apiKey.trim();
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("memory.library.enabled=false");
        }
        Objects.requireNonNull(properties, "memory library properties must not be null");
    }
}
