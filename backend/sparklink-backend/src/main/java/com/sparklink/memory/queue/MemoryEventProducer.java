package com.sparklink.memory.queue;

import com.sparklink.memory.model.MemoryEvent;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * MemoryEvent 生产者。
 *
 * @author AI-Pick
 */
@Component
public class MemoryEventProducer {

    public static final String MAIN_STREAM_KEY = "memory:events:main";

    private final StringRedisTemplate stringRedisTemplate;

    public MemoryEventProducer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public RecordId publish(MemoryEvent event) {
        Assert.notNull(event, "event 不能为空");
        return stringRedisTemplate.opsForStream().add(MAIN_STREAM_KEY, event.toStreamBody());
    }

    public RecordId publish(String memoryUserId, List<Map<String, String>> messages, String idempotencyKey) {
        MemoryEvent event = MemoryEvent.create(memoryUserId, messages, idempotencyKey);
        return publish(event);
    }
}
