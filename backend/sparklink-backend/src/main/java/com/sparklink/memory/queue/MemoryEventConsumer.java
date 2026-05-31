package com.sparklink.memory.queue;

import com.sparklink.memory.client.MemoryLibraryClient;
import com.sparklink.memory.config.MemoryLibraryProperties;
import com.sparklink.memory.metrics.MemoryMetricsRecorder;
import com.sparklink.memory.model.MemoryEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

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
    private final MemoryLibraryProperties memoryLibraryProperties;

    public MemoryEventConsumer(StringRedisTemplate stringRedisTemplate, MemoryLibraryClient memoryLibraryClient) {
        this(stringRedisTemplate, memoryLibraryClient::addMemory, new MemoryMetricsRecorder(), new MemoryLibraryProperties());
    }

    @Autowired
    public MemoryEventConsumer(StringRedisTemplate stringRedisTemplate,
                               MemoryLibraryClient memoryLibraryClient,
                               MemoryMetricsRecorder memoryMetricsRecorder,
                               MemoryLibraryProperties memoryLibraryProperties) {
        this(stringRedisTemplate, memoryLibraryClient::addMemory, memoryMetricsRecorder, memoryLibraryProperties);
    }

    MemoryEventConsumer(StringRedisTemplate stringRedisTemplate, MemoryAddExecutor memoryAddExecutor) {
        this(stringRedisTemplate, memoryAddExecutor, new MemoryMetricsRecorder(), new MemoryLibraryProperties());
    }

    MemoryEventConsumer(StringRedisTemplate stringRedisTemplate,
                        MemoryAddExecutor memoryAddExecutor,
                        MemoryMetricsRecorder memoryMetricsRecorder) {
        this(stringRedisTemplate, memoryAddExecutor, memoryMetricsRecorder, new MemoryLibraryProperties());
    }

    MemoryEventConsumer(StringRedisTemplate stringRedisTemplate,
                        MemoryAddExecutor memoryAddExecutor,
                        MemoryMetricsRecorder memoryMetricsRecorder,
                        MemoryLibraryProperties memoryLibraryProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.memoryAddExecutor = memoryAddExecutor;
        this.memoryMetricsRecorder = memoryMetricsRecorder;
        this.memoryLibraryProperties = memoryLibraryProperties;
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

    /**
     * 轮询主流与重试流，提供可运行的消费入口。
     */
    @Scheduled(fixedDelayString = "${memory.library.consumer-poll-interval-ms:2000}")
    public void pollStreams() {
        if (memoryLibraryProperties == null
                || !memoryLibraryProperties.isEnabled()
                || !memoryLibraryProperties.isConsumerEnabled()) {
            return;
        }
        drainStream(MemoryEventProducer.MAIN_STREAM_KEY, false);
        drainStream(RETRY_STREAM_KEY, true);
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

    private void drainStream(String streamKey, boolean retryStream) {
        int batchSize = memoryLibraryProperties.getConsumerBatchSize() == null
                ? 50
                : Math.max(1, memoryLibraryProperties.getConsumerBatchSize());
        List<MapRecord<String, Object, Object>> records =
                stringRedisTemplate.opsForStream().range(streamKey, org.springframework.data.domain.Range.unbounded());
        if (records == null || records.isEmpty()) {
            return;
        }
        int processed = 0;
        for (MapRecord<String, Object, Object> record : records) {
            if (processed >= batchSize) {
                break;
            }
            if (retryStream && !isDueRetryRecord(record)) {
                continue;
            }
            consumeRecord(record);
            stringRedisTemplate.opsForStream().delete(streamKey, record.getId());
            processed++;
        }
    }

    private boolean isDueRetryRecord(MapRecord<String, Object, Object> record) {
        Object value = record.getValue().get(MemoryEvent.FIELD_NEXT_RETRY_AT);
        if (value == null) {
            return true;
        }
        try {
            Instant nextRetryAt = Instant.parse(String.valueOf(value));
            return !nextRetryAt.isAfter(Instant.now());
        } catch (Exception ex) {
            return true;
        }
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

}
