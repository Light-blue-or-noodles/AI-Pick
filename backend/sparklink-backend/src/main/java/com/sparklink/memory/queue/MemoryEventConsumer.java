package com.sparklink.memory.queue;

import com.sparklink.memory.client.MemoryLibraryClient;
import com.sparklink.memory.metrics.MemoryMetricsRecorder;
import com.sparklink.memory.model.MemoryEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * MemoryEvent 消费者（幂等 + 重试 + DLQ）。
 *
 * @author AI-Pick
 */
@Component
public class MemoryEventConsumer {

    public static final String RETRY_STREAM_KEY = "memory:events:retry";
    public static final String DLQ_STREAM_KEY = "memory:events:dlq";
    public static final String IDEMPOTENCY_PREFIX = "memory:event:idempotent:";
    public static final Duration IDEMPOTENCY_TTL = Duration.ofDays(7);
    public static final int MAX_RETRY_COUNT = 6;
    public static final int MAX_ERROR_LENGTH = 512;

    private static final long[] RETRY_BACKOFF_SECONDS = {5, 30, 120, 600, 1800, 7200};
    private static final Pattern SENSITIVE_KV_PATTERN =
            Pattern.compile("(?i)(authorization|token|secret|password|passwd|api[-_]?key)\\s*[=:]\\s*\\S+");
    private static final Pattern SENSITIVE_JSON_DQ_PATTERN =
            Pattern.compile("(?i)(\"(?:authorization|token|secret|password|passwd|api[-_]?key)\"\\s*:\\s*\")([^\"]*)(\")");
    private static final Pattern SENSITIVE_JSON_SQ_PATTERN =
            Pattern.compile("(?i)('(?:authorization|token|secret|password|passwd|api[-_]?key)'\\s*:\\s*')([^']*)(')");
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._\\-+/=]+");

    private final StringRedisTemplate stringRedisTemplate;
    private final MemoryAddExecutor memoryAddExecutor;
    private final MemoryMetricsRecorder memoryMetricsRecorder;

    public MemoryEventConsumer(StringRedisTemplate stringRedisTemplate, MemoryLibraryClient memoryLibraryClient) {
        this(stringRedisTemplate, new ReflectiveMemoryAddExecutor(memoryLibraryClient), new MemoryMetricsRecorder());
    }

    @Autowired
    public MemoryEventConsumer(StringRedisTemplate stringRedisTemplate,
                               MemoryLibraryClient memoryLibraryClient,
                               MemoryMetricsRecorder memoryMetricsRecorder) {
        this(stringRedisTemplate, new ReflectiveMemoryAddExecutor(memoryLibraryClient), memoryMetricsRecorder);
    }

    MemoryEventConsumer(StringRedisTemplate stringRedisTemplate, MemoryAddExecutor memoryAddExecutor) {
        this(stringRedisTemplate, memoryAddExecutor, new MemoryMetricsRecorder());
    }

    MemoryEventConsumer(StringRedisTemplate stringRedisTemplate,
                        MemoryAddExecutor memoryAddExecutor,
                        MemoryMetricsRecorder memoryMetricsRecorder) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.memoryAddExecutor = memoryAddExecutor;
        this.memoryMetricsRecorder = memoryMetricsRecorder;
    }

    public void consume(MemoryEvent event) {
        Assert.notNull(event, "event 不能为空");
        if (!acquireIdempotency(event.getIdempotencyKey())) {
            return;
        }

        try {
            memoryAddExecutor.addMemory(event.getMemoryUserId(), event.getMessages());
            memoryMetricsRecorder.recordAddSuccess();
        } catch (Exception ex) {
            handleFailure(event, ex);
        }
    }

    public void consumeRecord(MapRecord<String, Object, Object> record) {
        Assert.notNull(record, "record 不能为空");
        try {
            Map<Object, Object> body = new LinkedHashMap<>(record.getValue());
            MemoryEvent event = MemoryEvent.fromStreamBody(body);
            consume(event);
        } catch (Exception ex) {
            moveRawRecordToDlq(record, buildErrorMessage(ex));
        }
    }

    private boolean acquireIdempotency(String idempotencyKey) {
        String key = IDEMPOTENCY_PREFIX + idempotencyKey;
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENCY_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    private void handleFailure(MemoryEvent event, Exception ex) {
        String errorMessage = buildErrorMessage(ex);
        releaseIdempotency(event.getIdempotencyKey());

        if (isRetryable(ex) && event.getRetryCount() < MAX_RETRY_COUNT) {
            retryLater(event, errorMessage);
            return;
        }
        moveToDlq(event, errorMessage);
    }

    private void retryLater(MemoryEvent event, String errorMessage) {
        int currentRetryCount = event.getRetryCount();
        int nextRetryCount = currentRetryCount + 1;
        long backoffSeconds = RETRY_BACKOFF_SECONDS[currentRetryCount];
        MemoryEvent retryEvent = event.withRetry(nextRetryCount, Instant.now().plusSeconds(backoffSeconds), sanitizeAndTruncate(errorMessage));
        stringRedisTemplate.opsForStream().add(RETRY_STREAM_KEY, retryEvent.toStreamBody());
        memoryMetricsRecorder.recordRetry();
    }

    private void moveToDlq(MemoryEvent event, String errorMessage) {
        String sanitizedReason = sanitizeAndTruncate(errorMessage);
        Map<String, Object> body = new LinkedHashMap<>(event.toStreamBody());
        body.put(MemoryEvent.FIELD_FAILURE_REASON, sanitizedReason);
        stringRedisTemplate.opsForStream().add(DLQ_STREAM_KEY, body);
        memoryMetricsRecorder.recordDlq(sanitizedReason);
    }

    private void moveRawRecordToDlq(MapRecord<String, Object, Object> record, String errorMessage) {
        String sanitizedReason = sanitizeAndTruncate(errorMessage);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source_stream", record.getStream());
        body.put("source_record_id", String.valueOf(record.getId()));
        body.put(MemoryEvent.FIELD_FAILURE_REASON, sanitizedReason);
        body.put("raw_summary", summarizeRecordBody(record.getValue()));
        body.put(MemoryEvent.FIELD_CREATED_AT, Instant.now().toString());
        stringRedisTemplate.opsForStream().add(DLQ_STREAM_KEY, body);
        memoryMetricsRecorder.recordDlq(sanitizedReason);
    }

    private void releaseIdempotency(String idempotencyKey) {
        stringRedisTemplate.delete(IDEMPOTENCY_PREFIX + idempotencyKey);
    }

    private boolean isRetryable(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof SocketTimeoutException || throwable instanceof TimeoutException) {
            return true;
        }
        if (throwable instanceof ResourceAccessException) {
            return true;
        }
        if (throwable instanceof RestClientResponseException responseException) {
            int statusCode = responseException.getRawStatusCode();
            return statusCode == 429 || statusCode >= 500;
        }
        if (StringUtils.hasText(throwable.getMessage()) && throwable.getMessage().toLowerCase().contains("timeout")) {
            return true;
        }
        return isRetryable(throwable.getCause());
    }

    private String buildErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        if (StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    private String summarizeRecordBody(Map<Object, Object> recordValue) {
        Map<Object, Object> safeMap = recordValue == null ? Collections.emptyMap() : recordValue;
        return sanitizeAndTruncate(String.valueOf(safeMap));
    }

    private String sanitizeAndTruncate(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return "unknown error";
        }
        String sanitized = rawValue
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll("\\p{Cntrl}", " ");
        boolean hasSensitiveKv = SENSITIVE_KV_PATTERN.matcher(sanitized).find();
        sanitized = SENSITIVE_KV_PATTERN.matcher(sanitized).replaceAll("$1=[REDACTED]");
        boolean hasSensitiveJsonDq = SENSITIVE_JSON_DQ_PATTERN.matcher(sanitized).find();
        sanitized = SENSITIVE_JSON_DQ_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED]$3");
        boolean hasSensitiveJsonSq = SENSITIVE_JSON_SQ_PATTERN.matcher(sanitized).find();
        sanitized = SENSITIVE_JSON_SQ_PATTERN.matcher(sanitized).replaceAll("$1[REDACTED]$3");
        boolean hasBearerToken = BEARER_PATTERN.matcher(sanitized).find();
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED]");
        if (hasSensitiveKv || hasSensitiveJsonDq || hasSensitiveJsonSq || hasBearerToken) {
            memoryMetricsRecorder.recordSensitiveFieldBlock();
        }
        sanitized = sanitized.replaceAll("\\s{2,}", " ").trim();
        if (sanitized.length() > MAX_ERROR_LENGTH) {
            return sanitized.substring(0, MAX_ERROR_LENGTH);
        }
        return sanitized;
    }

    @FunctionalInterface
    public interface MemoryAddExecutor {
        void addMemory(String memoryUserId, List<Map<String, String>> messages);
    }

    private static final class ReflectiveMemoryAddExecutor implements MemoryAddExecutor {
        private final MemoryLibraryClient memoryLibraryClient;
        private final Method addMemoryMethod;

        private ReflectiveMemoryAddExecutor(MemoryLibraryClient memoryLibraryClient) {
            this.memoryLibraryClient = memoryLibraryClient;
            this.addMemoryMethod = resolveAddMemoryMethod(memoryLibraryClient);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void addMemory(String memoryUserId, List<Map<String, String>> messages) {
            try {
                addMemoryMethod.invoke(memoryLibraryClient, memoryUserId, messages);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("调用 MemoryLibraryClient.addMemory 失败", ex);
            } catch (InvocationTargetException ex) {
                Throwable target = ex.getTargetException();
                if (target instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("MemoryLibraryClient.addMemory 执行失败", target);
            }
        }

        private static Method resolveAddMemoryMethod(MemoryLibraryClient memoryLibraryClient) {
            Assert.notNull(memoryLibraryClient, "memoryLibraryClient 不能为空");
            try {
                return memoryLibraryClient.getClass().getMethod("addMemory", String.class, List.class);
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("MemoryLibraryClient.addMemory(String, List) 未实现", ex);
            }
        }
    }
}
