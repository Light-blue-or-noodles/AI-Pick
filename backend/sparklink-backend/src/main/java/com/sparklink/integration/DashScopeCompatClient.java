package com.sparklink.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 阿里云百炼（DashScope）<b>OpenAI 兼容</b>对话接口。
 * <p>
 * 旧版 {@code /api/v1/services/aigc/text-generation/generation} 对部分新模型会返回
 * {@code InvalidParameter: url error}，需改走 {@code /compatible-mode/v1/chat/completions}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashScopeCompatClient {

    private final ObjectMapper objectMapper;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${spring.ai.dashscope.chat-completions-url:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String chatCompletionsUrl;

    @Value("${spring.ai.dashscope.model:qwen-max}")
    private String defaultModel;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 是否已配置百炼 API Key（未配置时 {@link #completeUserOnly} 等会直接返回 null）。
     */
    public boolean isApiKeyConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * 单条 user 消息，使用配置的默认模型（与首页 AI 对话一致）。
     */
    public String completeUserOnly(String userText) {
        if (!StringUtils.hasText(apiKey)) {
            log.warn("[DashScope] 未配置 api-key");
            return null;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", defaultModel);
            ArrayNode messages = body.putArray("messages");
            ObjectNode u = messages.addObject();
            u.put("role", "user");
            u.put("content", userText == null ? "" : userText);
            return postAndParse(body);
        } catch (Exception e) {
            log.error("[DashScope] completeUserOnly 失败", e);
            return null;
        }
    }

    /**
     * 多轮 / 多角色消息，可指定模型（为 null 时用默认）。
     */
    public String completeMessages(ArrayNode messages, String model) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", StringUtils.hasText(model) ? model : defaultModel);
            body.set("messages", messages);
            return postAndParse(body);
        } catch (Exception e) {
            log.error("[DashScope] completeMessages 失败", e);
            return null;
        }
    }

    private String postAndParse(ObjectNode requestBody) throws Exception {
        String bodyStr = objectMapper.writeValueAsString(requestBody);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey.replaceFirst("^Bearer\\s+", ""));

        HttpEntity<String> entity = new HttpEntity<>(bodyStr, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    chatCompletionsUrl, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("[DashScope] HTTP 非 2xx: status={} body前200字={}",
                        response.getStatusCode(), truncate(response.getBody(), 200));
                return null;
            }
            return extractAssistantText(response.getBody());
        } catch (HttpClientErrorException e) {
            log.error("[DashScope] HTTP {}: {}", e.getStatusCode(), truncate(e.getResponseBodyAsString(), 1000));
            return null;
        } catch (RestClientException e) {
            log.error("[DashScope] 请求失败", e);
            return null;
        }
    }

    /**
     * 解析 OpenAI 兼容响应：根节点 {@code choices[0].message.content}；兼容部分旧式嵌套在 {@code output} 下。
     */
    public String extractAssistantText(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode err = root.get("error");
            if (err != null && !err.isNull()) {
                log.warn("[DashScope] 响应含 error: {}", truncate(err.toString(), 500));
                return null;
            }
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                String c = choices.get(0).path("message").path("content").asText("");
                if (!c.isEmpty()) {
                    return c;
                }
            }
            JsonNode out = root.path("output");
            if (!out.isMissingNode() && !out.isNull()) {
                String t = out.path("text").asText("");
                if (!t.isEmpty()) {
                    return t;
                }
                choices = out.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText("");
                }
            }
            log.warn("[DashScope] 无法解析助手正文，前500字: {}", truncate(responseBody, 500));
        } catch (Exception e) {
            log.error("[DashScope] 解析响应 JSON 失败: {}", e.getMessage());
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...(截断)";
    }
}
