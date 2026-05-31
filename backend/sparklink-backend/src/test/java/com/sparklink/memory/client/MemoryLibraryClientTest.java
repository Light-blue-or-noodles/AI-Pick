package com.sparklink.memory.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sparklink.memory.config.MemoryLibraryProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryLibraryClientTest {

    @Test
    void buildSearchPayload_withDefaultProperties_containsFixedKnowledgebaseId() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);

        ObjectNode payload = client.buildSearchPayload("sparklink:user:10086", "测试查询");

        assertEquals("sparklink:user:10086", payload.path("user_id").asText());
        assertEquals("测试查询", payload.path("query").asText());
        assertEquals(1, payload.path("knowledgebase_ids").size());
        assertEquals("5eb944ee565c400d8fb71e6ab119dfe3", payload.path("knowledgebase_ids").get(0).asText());
    }

    @Test
    void buildSearchPayload_withConfiguredKnowledgebaseId_prefersConfigValue() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        properties.setKnowledgebaseId("kb-from-config");
        MemoryLibraryClient client = new MemoryLibraryClient(properties);

        ObjectNode payload = client.buildSearchPayload("sparklink:user:9527", "配置优先");

        assertEquals("kb-from-config", payload.path("knowledgebase_ids").get(0).asText());
    }

    @Test
    void buildAddPayload_containsKnowledgebaseIdAndMessages() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", "你好"),
                Map.of("role", "assistant", "content", "你好，我在")
        );

        ObjectNode payload = client.buildAddPayload("sparklink:user:10010", messages);

        assertEquals("sparklink:user:10010", payload.path("user_id").asText());
        assertEquals(1, payload.path("knowledgebase_ids").size());
        assertEquals("5eb944ee565c400d8fb71e6ab119dfe3", payload.path("knowledgebase_ids").get(0).asText());
        assertEquals(2, payload.path("messages").size());
        assertEquals("user", payload.path("messages").get(0).path("role").asText());
        assertEquals("你好", payload.path("messages").get(0).path("content").asText());
        assertEquals("assistant", payload.path("messages").get(1).path("role").asText());
        assertEquals("你好，我在", payload.path("messages").get(1).path("content").asText());
    }
}
