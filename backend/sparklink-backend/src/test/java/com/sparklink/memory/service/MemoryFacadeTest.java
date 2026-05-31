package com.sparklink.memory.service;

import com.sparklink.memory.client.MemoryLibraryClient;
import com.sparklink.memory.metrics.MemoryMetricsRecorder;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.queue.MemoryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryFacadeTest {

    @Mock
    private MemoryEventProducer memoryEventProducer;

    @Mock
    private MemoryMetricsRecorder memoryMetricsRecorder;

    @Mock
    private MemoryLibraryClient memoryLibraryClient;

    private MemoryFacade memoryFacade;

    @BeforeEach
    void setUp() {
        memoryFacade = new MemoryFacade(memoryEventProducer, memoryLibraryClient, memoryMetricsRecorder);
    }

    @Test
    void recallForPrompt_whenSearchReturnsData_shouldFilterSensitiveAndCountSuccess() {
        when(memoryLibraryClient.searchMemory(eq("sparklink:user:10086"), eq("我喜欢打羽毛球")))
                .thenReturn(MemoryContext.of(
                        List.of("用户偏好羽毛球活动"),
                        Map.of("年龄", "28", "爱好", "羽毛球")
                ));

        MemoryContext context = memoryFacade.recallForPrompt(10086L, "我喜欢打羽毛球");
        assertNotNull(context);
        assertFalse(context.isEmpty());
        assertTrue(context.getMemorySnippets().contains("用户偏好羽毛球活动"));
        assertFalse(context.getProfileAttributes().containsKey("年龄"));
        assertEquals("羽毛球", context.getProfileAttributes().get("爱好"));
        verify(memoryMetricsRecorder).recordSearchSuccess();
        verify(memoryMetricsRecorder, never()).recordSearchFailure();
    }

    @Test
    void recallForPrompt_whenSearchThrows_shouldReturnEmptyAndCountFailure() {
        when(memoryLibraryClient.searchMemory(any(), any())).thenThrow(new RuntimeException("search failed"));
        MemoryContext context = memoryFacade.recallForPrompt(10086L, "测试");
        assertNotNull(context);
        assertTrue(context.isEmpty());
        verify(memoryMetricsRecorder).recordSearchFailure();
    }

    @Test
    void mergePrompt_whenContextIsNull_returnsRawPrompt() {
        String merged = memoryFacade.mergePrompt("推荐同城活动", null);
        assertEquals("推荐同城活动", merged);
    }

    @Test
    void enqueueConversation_shouldPublishMessagesWithResolvedMemoryUserId() {
        memoryFacade.enqueueConversation(10086L, "你好", "你好，我在");

        ArgumentCaptor<List<Map<String, String>>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> idempotencyCaptor = ArgumentCaptor.forClass(String.class);
        verify(memoryEventProducer).publish(eq("sparklink:user:10086"), messagesCaptor.capture(), idempotencyCaptor.capture());

        List<Map<String, String>> messages = messagesCaptor.getValue();
        assertEquals(2, messages.size());
        assertEquals(Map.of("role", "user", "content", "你好"), messages.get(0));
        assertEquals(Map.of("role", "assistant", "content", "你好，我在"), messages.get(1));
        assertNotNull(idempotencyCaptor.getValue());
        verify(memoryMetricsRecorder, never()).recordAddSuccess();
        verify(memoryMetricsRecorder, never()).recordDlq(any());
        verify(memoryMetricsRecorder).recordEnqueueSuccess();
        verify(memoryMetricsRecorder, never()).recordEnqueueFailure();
    }

    @Test
    void enqueueConversation_whenPublishFails_shouldNotCountDlqOrAddSuccess() {
        when(memoryEventProducer.publish(any(), any(), any())).thenThrow(new RuntimeException("publish failed"));

        memoryFacade.enqueueConversation(10086L, "你好", "你好，我在");

        verify(memoryMetricsRecorder, never()).recordAddSuccess();
        verify(memoryMetricsRecorder, never()).recordDlq(any());
        verify(memoryMetricsRecorder, never()).recordEnqueueSuccess();
        verify(memoryMetricsRecorder).recordEnqueueFailure();
    }

    @Test
    void enqueueConversation_whenInputInvalid_shouldSkipWithoutMetrics() {
        memoryFacade.enqueueConversation(null, "你好", "你好，我在");

        verifyNoInteractions(memoryEventProducer);
        verifyNoInteractions(memoryMetricsRecorder);
    }
}
