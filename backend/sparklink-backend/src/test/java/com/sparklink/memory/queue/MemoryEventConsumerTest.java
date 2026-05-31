package com.sparklink.memory.queue;

import com.sparklink.memory.model.MemoryEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
