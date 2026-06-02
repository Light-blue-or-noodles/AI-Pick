package com.sparklink.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 百炼 WebSearch MCP 客户端：通过 MCP JSON-RPC 协议调用联网搜索工具。
 */
@Component
public class BailianWebSearchClient {

    private static final Logger log = LoggerFactory.getLogger(BailianWebSearchClient.class);

    private final ObjectMapper objectMapper;
    private WebSearchProperties webSearchProperties;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${app.chat.web-search.endpoint:https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp}")
    private String endpoint;

    public BailianWebSearchClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Autowired(required = false)
    void setWebSearchProperties(WebSearchProperties webSearchProperties) {
        this.webSearchProperties = webSearchProperties;
    }

    /**
     * 联网检索调用入口。
     */
    public List<RawResult> search(String query, int count) {
        validateEndpoint();
        if (!StringUtils.hasText(apiKey)) {
            log.warn("Web search skipped: api-key not configured");
            return Collections.emptyList();
        }
        String payload = buildMcpCallPayload(query, count);
        RestTemplate timeoutRestTemplate = createRestTemplate(resolveTimeoutMs());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.replaceFirst("^Bearer\\s+", ""));
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = timeoutRestTemplate.postForEntity(endpoint, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                log.warn("MCP web search response invalid: status={}", response.getStatusCode());
                return Collections.emptyList();
            }
            return parseResults(response.getBody());
        } catch (RestClientException ex) {
            throw new IllegalStateException("MCP web search request failed", ex);
        }
    }

    public String buildMcpCallPayload(String query, int count) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("jsonrpc", "2.0");
        root.put("id", 1);
        root.put("method", "tools/call");

        ObjectNode params = root.putObject("params");
        params.put("name", "bailian_web_search");
        ObjectNode arguments = params.putObject("arguments");
        arguments.put("query", query == null ? "" : query);
        arguments.put("count", Math.max(1, count));
        try {
            return objectMapper.writeValueAsString(root);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build MCP web search payload", ex);
        }
    }

    /**
     * 兼容旧测试方法名，内部转发到 MCP 调用体构建。
     */
    public String buildPayload(String query, int count) {
        return buildMcpCallPayload(query, count);
    }

    public String buildMcpInitializePayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("jsonrpc", "2.0");
        payload.put("id", 1);
        payload.put("method", "initialize");
        ObjectNode params = payload.putObject("params");
        params.put("protocolVersion", "2024-11-05");
        params.putObject("capabilities");
        ObjectNode clientInfo = params.putObject("clientInfo");
        clientInfo.put("name", "sparklink-backend");
        clientInfo.put("version", "1.0.0");
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build MCP initialize payload", ex);
        }
    }

    public List<RawResult> parseResults(String body) {
        if (body == null || body.isBlank()) {
            log.warn("Web search response has no results: body is null or blank");
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            boolean isError = root.path("isError").asBoolean(false);
            JsonNode contentNode = root.path("result").path("content");
            if (!contentNode.isArray() || contentNode.isEmpty()) {
                log.warn("MCP response has no content array");
                return Collections.emptyList();
            }
            String textPayload = contentNode.get(0).path("text").asText("");
            if (!StringUtils.hasText(textPayload)) {
                log.warn("MCP content text is empty");
                return Collections.emptyList();
            }
            JsonNode toolResult = objectMapper.readTree(textPayload);
            if (isError || toolResult.path("status").asInt(0) != 0) {
                int status = toolResult.path("status").asInt(-1);
                log.warn("MCP tool call returned error status={}", status);
                throw new IllegalStateException("MCP tool call failed with status=" + status);
            }
            JsonNode resultsNode = toolResult.path("pages");
            if (!resultsNode.isArray()) {
                log.warn("MCP tool payload has no pages array");
                return Collections.emptyList();
            }
            List<RawResult> rows = new ArrayList<>();
            for (JsonNode item : resultsNode) {
                rows.add(new RawResult(
                        item.path("title").asText(""),
                        item.path("url").asText(""),
                        item.path("snippet").asText(""),
                        item.path("publishedAt").asText("")
                ));
            }
            return rows;
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Failed to parse web search response body", ex);
            return Collections.emptyList();
        }
    }

    private void validateEndpoint() {
        if (!StringUtils.hasText(endpoint) || !endpoint.startsWith("https://")) {
            throw new IllegalStateException("Web search endpoint must use https");
        }
    }

    private long resolveTimeoutMs() {
        long defaultTimeoutMs = 5_000L;
        if (webSearchProperties == null || webSearchProperties.getTimeoutMs() <= 0) {
            return defaultTimeoutMs;
        }
        return webSearchProperties.getTimeoutMs();
    }

    private RestTemplate createRestTemplate(long timeoutMs) {
        int safeTimeout = timeoutMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) timeoutMs;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(safeTimeout);
        requestFactory.setReadTimeout(safeTimeout);
        return new RestTemplate(requestFactory);
    }

    public String buildToolsListPayload() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("jsonrpc", "2.0");
        payload.put("id", 1);
        payload.put("method", "tools/list");
        payload.set("params", objectMapper.createObjectNode());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build MCP tools/list payload", ex);
        }
    }

    public record RawResult(String title, String url, String snippet, String publishedAt) {
    }
}
