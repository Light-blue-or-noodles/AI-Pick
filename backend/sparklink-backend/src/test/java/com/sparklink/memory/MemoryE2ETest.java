package com.sparklink.memory;

import com.sparklink.memory.model.MemoryEvent;
import com.sparklink.memory.queue.MemoryEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 记忆写入到召回的端到端语义回归（采用 Redis/记忆库 stub）。
 *
 * @author AI-Pick
 */
@ExtendWith(MockitoExtension.class)
class MemoryE2ETest {

    @Mock
    private StringRedisTemplate producerRedisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> producerStreamOperations;

    private final List<MapRecord<String, Object, Object>> mainStreamRecords = new ArrayList<>();
    private final InMemoryMemoryLibraryStub memoryLibraryStub = new InMemoryMemoryLibraryStub();

    private MemoryEventProducer memoryEventProducer;

    @BeforeEach
    void setUp() {
        when(producerRedisTemplate.opsForStream()).thenReturn(producerStreamOperations);
        when(producerStreamOperations.add(eq(MemoryEventProducer.MAIN_STREAM_KEY), any(Map.class)))
                .thenAnswer(invocation -> {
                    Map<String, Object> eventBody = invocation.getArgument(1);
                    MapRecord<String, Object, Object> record = StreamRecords.newRecord()
                            .in(MemoryEventProducer.MAIN_STREAM_KEY)
                            .ofMap(new LinkedHashMap<>(eventBody));
                    mainStreamRecords.add(record);
                    return RecordId.autoGenerate();
                });

        memoryEventProducer = new MemoryEventProducer(producerRedisTemplate);
    }

    @Test
    void shouldRecallConversationForSameUser() {
        String userA = "sparklink:user:10086";

        memoryEventProducer.publish(userA, conversation("我想找羽毛球搭子", "我帮你筛了 3 个羽毛球活动"), "idem-u10086-1");
        consumeMainStream();

        List<String> recalled = memoryLibraryStub.searchContents(userA, "羽毛球");
        assertFalse(recalled.isEmpty(), "同一用户写入后应可召回相关记忆");
        assertTrue(recalled.stream().anyMatch(content -> content.contains("羽毛球")));
    }

    @Test
    void shouldIsolateMemoriesAcrossDifferentUsers() {
        String userA = "sparklink:user:10086";
        String userB = "sparklink:user:10010";

        memoryEventProducer.publish(userA, conversation("周末去露营吗", "可以推荐近郊营地"), "idem-u10086-2");
        consumeMainStream();

        List<String> userARecall = memoryLibraryStub.searchContents(userA, "露营");
        List<String> userBRecall = memoryLibraryStub.searchContents(userB, "露营");

        assertFalse(userARecall.isEmpty(), "写入用户应召回到自己的记忆");
        assertTrue(userBRecall.isEmpty(), "不同用户不应召回他人记忆");
    }

    private void consumeMainStream() {
        for (MapRecord<String, Object, Object> record : mainStreamRecords) {
            MemoryEvent event = MemoryEvent.fromStreamBody(new LinkedHashMap<>(record.getValue()));
            memoryLibraryStub.addMemory(event.getMemoryUserId(), event.getMessages());
        }
        mainStreamRecords.clear();
    }

    private List<Map<String, String>> conversation(String userMessage, String assistantMessage) {
        return List.of(
                Map.of("role", "user", "content", userMessage),
                Map.of("role", "assistant", "content", assistantMessage)
        );
    }

    /**
     * 端到端语义 stub：按 memoryUserId 存储并按 query 过滤召回。
     */
    private static final class InMemoryMemoryLibraryStub {
        private final Map<String, List<Map<String, String>>> store = new ConcurrentHashMap<>();

        private void addMemory(String memoryUserId, List<Map<String, String>> messages) {
            store.computeIfAbsent(memoryUserId, key -> new ArrayList<>())
                    .addAll(messages);
        }

        private List<String> searchContents(String memoryUserId, String query) {
            return store.getOrDefault(memoryUserId, List.of())
                    .stream()
                    .map(message -> message.getOrDefault("content", ""))
                    .filter(content -> content.contains(query))
                    .collect(Collectors.toList());
        }
    }
}
