package com.sparklink.memory.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 记忆异步写入事件。
 *
 * @author AI-Pick
 */
public class MemoryEvent {

    public static final String FIELD_EVENT_ID = "event_id";
    public static final String FIELD_MEMORY_USER_ID = "memory_user_id";
    public static final String FIELD_MESSAGES = "messages";
    public static final String FIELD_IDEMPOTENCY_KEY = "idempotency_key";
    public static final String FIELD_RETRY_COUNT = "retry_count";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_NEXT_RETRY_AT = "next_retry_at";
    public static final String FIELD_LAST_ERROR = "last_error";
    public static final String FIELD_FAILURE_REASON = "failure_reason";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, String>>> MESSAGES_TYPE = new TypeReference<>() {
    };

    private final String eventId;
    private final String memoryUserId;
    private final List<Map<String, String>> messages;
    private final String idempotencyKey;
    private final int retryCount;
    private final String createdAt;
    private final String nextRetryAt;
    private final String lastError;

    private MemoryEvent(Builder builder) {
        this.eventId = builder.eventId;
        this.memoryUserId = builder.memoryUserId;
        this.messages = builder.messages;
        this.idempotencyKey = builder.idempotencyKey;
        this.retryCount = builder.retryCount;
        this.createdAt = builder.createdAt;
        this.nextRetryAt = builder.nextRetryAt;
        this.lastError = builder.lastError;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MemoryEvent create(String memoryUserId, List<Map<String, String>> messages, String idempotencyKey) {
        return builder()
                .eventId(UUID.randomUUID().toString())
                .memoryUserId(memoryUserId)
                .messages(messages)
                .idempotencyKey(idempotencyKey)
                .retryCount(0)
                .createdAt(Instant.now().toString())
                .build();
    }

    public MemoryEvent withRetry(int newRetryCount, Instant nextRetryAtTime, String errorMessage) {
        Assert.notNull(nextRetryAtTime, "nextRetryAtTime 不能为空");
        return MemoryEvent.builder()
                .eventId(this.eventId)
                .memoryUserId(this.memoryUserId)
                .messages(this.messages)
                .idempotencyKey(this.idempotencyKey)
                .retryCount(newRetryCount)
                .createdAt(this.createdAt)
                .nextRetryAt(nextRetryAtTime.toString())
                .lastError(errorMessage)
                .build();
    }

    public Map<String, Object> toStreamBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(FIELD_EVENT_ID, eventId);
        body.put(FIELD_MEMORY_USER_ID, memoryUserId);
        body.put(FIELD_MESSAGES, toMessagesJson(messages));
        body.put(FIELD_IDEMPOTENCY_KEY, idempotencyKey);
        body.put(FIELD_RETRY_COUNT, String.valueOf(retryCount));
        body.put(FIELD_CREATED_AT, createdAt);
        if (StringUtils.hasText(nextRetryAt)) {
            body.put(FIELD_NEXT_RETRY_AT, nextRetryAt);
        }
        if (StringUtils.hasText(lastError)) {
            body.put(FIELD_LAST_ERROR, lastError);
        }
        return body;
    }

    public static MemoryEvent fromStreamBody(Map<Object, Object> body) {
        Assert.notNull(body, "body 不能为空");
        return MemoryEvent.builder()
                .eventId(requireText(body, FIELD_EVENT_ID))
                .memoryUserId(requireText(body, FIELD_MEMORY_USER_ID))
                .messages(parseMessages(requireText(body, FIELD_MESSAGES)))
                .idempotencyKey(requireText(body, FIELD_IDEMPOTENCY_KEY))
                .retryCount(parseRetryCount(body.get(FIELD_RETRY_COUNT)))
                .createdAt(requireText(body, FIELD_CREATED_AT))
                .nextRetryAt(readText(body.get(FIELD_NEXT_RETRY_AT)))
                .lastError(readText(body.get(FIELD_LAST_ERROR)))
                .build();
    }

    public String getEventId() {
        return eventId;
    }

    public String getMemoryUserId() {
        return memoryUserId;
    }

    public List<Map<String, String>> getMessages() {
        return messages;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getNextRetryAt() {
        return nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    private static int parseRetryCount(Object value) {
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static String requireText(Map<Object, Object> body, String key) {
        String value = readText(body.get(key));
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(key + " 不能为空");
        }
        return value;
    }

    private static String readText(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    private static String toMessagesJson(List<Map<String, String>> messages) {
        Assert.notNull(messages, "messages 不能为空");
        try {
            return OBJECT_MAPPER.writeValueAsString(messages);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("messages 序列化失败", ex);
        }
    }

    private static List<Map<String, String>> parseMessages(String messagesJson) {
        try {
            return OBJECT_MAPPER.readValue(messagesJson, MESSAGES_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("messages 反序列化失败", ex);
        }
    }

    public static final class Builder {
        private String eventId;
        private String memoryUserId;
        private List<Map<String, String>> messages;
        private String idempotencyKey;
        private int retryCount;
        private String createdAt;
        private String nextRetryAt;
        private String lastError;

        private Builder() {
        }

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder memoryUserId(String memoryUserId) {
            this.memoryUserId = memoryUserId;
            return this;
        }

        public Builder messages(List<Map<String, String>> messages) {
            this.messages = messages;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder nextRetryAt(String nextRetryAt) {
            this.nextRetryAt = nextRetryAt;
            return this;
        }

        public Builder lastError(String lastError) {
            this.lastError = lastError;
            return this;
        }

        public MemoryEvent build() {
            Assert.hasText(eventId, "eventId 不能为空");
            Assert.hasText(memoryUserId, "memoryUserId 不能为空");
            Assert.notNull(messages, "messages 不能为空");
            Assert.hasText(idempotencyKey, "idempotencyKey 不能为空");
            if (!StringUtils.hasText(createdAt)) {
                createdAt = Instant.now().toString();
            }
            if (retryCount < 0) {
                throw new IllegalArgumentException("retryCount 不能小于 0");
            }
            return new MemoryEvent(this);
        }
    }
}
