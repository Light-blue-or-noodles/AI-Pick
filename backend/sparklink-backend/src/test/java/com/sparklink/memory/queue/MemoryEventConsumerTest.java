package com.sparklink.memory.queue;

import com.sparklink.memory.model.MemoryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryEventConsumerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private MemoryEventConsumer.MemoryAddExecutor memoryAddExecutor;

    private MemoryEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new MemoryEventConsumer(stringRedisTemplate, memoryAddExecutor);
    }

    @Test
    void shouldSkipWhenIdempotencyKeyExists() {
        MemoryEvent event = buildEvent("idem-hit", 0);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("memory:event:idempotent:idem-hit"), eq("1"), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        consumer.consume(event);

        verify(memoryAddExecutor, never()).addMemory(any(), any());
        verify(streamOperations, never()).add(eq("memory:events:retry"), any(Map.class));
        verify(streamOperations, never()).add(eq("memory:events:dlq"), any(Map.class));
    }

    @Test
    void shouldRequeueToRetryStreamWhenRetryableError() {
        MemoryEvent event = buildEvent("idem-retry", 0);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);
        when(valueOperations.setIfAbsent(eq("memory:event:idempotent:idem-retry"), eq("1"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        HttpClientErrorException retryable = HttpClientErrorException.create(
                "too many requests",
                HttpStatus.TOO_MANY_REQUESTS,
                "too many requests",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );
        org.mockito.Mockito.doThrow(retryable).when(memoryAddExecutor).addMemory(any(), any());

        consumer.consume(event);

        verify(stringRedisTemplate).delete("memory:event:idempotent:idem-retry");
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq("memory:events:retry"), captor.capture());
        Map<String, Object> body = captor.getValue();
        assertEquals("idem-retry", body.get("idempotency_key"));
        assertEquals("1", body.get("retry_count"));
    }

    @Test
    void shouldMoveToDlqWhenNonRetryableError() {
        MemoryEvent event = buildEvent("idem-dlq", 2);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);
        when(valueOperations.setIfAbsent(eq("memory:event:idempotent:idem-dlq"), eq("1"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("bad payload"))
                .when(memoryAddExecutor).addMemory(any(), any());

        consumer.consume(event);

        verify(stringRedisTemplate).delete("memory:event:idempotent:idem-dlq");
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq("memory:events:dlq"), captor.capture());
        Map<String, Object> body = captor.getValue();
        assertEquals("bad payload", body.get("failure_reason"));
    }

    @Test
    void shouldMoveToDlqWhenRetryCountReachesUpperBound() {
        MemoryEvent event = buildEvent("idem-max-retry", MemoryEventConsumer.MAX_RETRY_COUNT);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);
        when(valueOperations.setIfAbsent(eq("memory:event:idempotent:idem-max-retry"), eq("1"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);
        org.mockito.Mockito.doThrow(new RuntimeException("timeout token=abc123"))
                .when(memoryAddExecutor).addMemory(any(), any());

        consumer.consume(event);

        verify(streamOperations, never()).add(eq("memory:events:retry"), any(Map.class));
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq("memory:events:dlq"), captor.capture());
        String failureReason = String.valueOf(captor.getValue().get("failure_reason"));
        assertTrue(failureReason.contains("token=[REDACTED]"));
    }

    @Test
    void shouldProcessSuccessfullyWithoutWritingRetryOrDlq() {
        MemoryEvent event = buildEvent("idem-success", 0);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("memory:event:idempotent:idem-success"), eq("1"), any(Duration.class)))
                .thenReturn(Boolean.TRUE);

        consumer.consume(event);

        verify(memoryAddExecutor).addMemory(eq("sparklink:user:10086"), any());
        verify(streamOperations, never()).add(eq("memory:events:retry"), any(Map.class));
        verify(streamOperations, never()).add(eq("memory:events:dlq"), any(Map.class));
    }

    @Test
    void shouldMoveMalformedRecordToDlqWhenConsumeRecordFailsToParse() {
        when(stringRedisTemplate.opsForStream()).thenReturn(streamOperations);
        @SuppressWarnings("unchecked")
        MapRecord<String, Object, Object> record = org.mockito.Mockito.mock(MapRecord.class);
        when(record.getStream()).thenReturn("memory:events:main");
        when(record.getId()).thenReturn(RecordId.of("1-0"));
        when(record.getValue()).thenReturn(Map.of("token", "abc123"));

        consumer.consumeRecord(record);

        verify(memoryAddExecutor, never()).addMemory(any(), any());
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(streamOperations).add(eq("memory:events:dlq"), captor.capture());
        Map<String, Object> body = captor.getValue();
        assertEquals("memory:events:main", body.get("source_stream"));
        assertEquals("1-0", body.get("source_record_id"));
        String rawSummary = String.valueOf(body.get("raw_summary"));
        assertTrue(rawSummary.contains("token=[REDACTED]"));
        assertFalse(rawSummary.contains("abc123"));
    }

    private MemoryEvent buildEvent(String idempotencyKey, int retryCount) {
        return MemoryEvent.builder()
                .eventId("evt-" + idempotencyKey)
                .memoryUserId("sparklink:user:10086")
                .messages(List.of(
                        Map.of("role", "user", "content", "你好"),
                        Map.of("role", "assistant", "content", "你好，我在")
                ))
                .idempotencyKey(idempotencyKey)
                .retryCount(retryCount)
                .build();
    }
}
