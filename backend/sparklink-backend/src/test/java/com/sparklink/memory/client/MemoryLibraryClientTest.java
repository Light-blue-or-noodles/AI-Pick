package com.sparklink.memory.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sparklink.memory.config.MemoryLibraryProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryLibraryClientTest {

    @Test
    void buildSearchPayload_withDefaultProperties_containsFixedKnowledgebaseId() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);

        ObjectNode payload = client.buildSearchPayload("sparklink:user:10086", "测试查询");

        assertEquals("sparklink:user:10086", payload.path("user_id").asText());
        assertEquals("5eb944ee565c400d8fb71e6ab119dfe3", payload.path("memory_library_id").asText());
        assertEquals(1, payload.path("messages").size());
        assertEquals("user", payload.path("messages").get(0).path("role").asText());
        assertEquals("测试查询", payload.path("messages").get(0).path("content").asText());
        assertEquals(8, payload.path("top_k").asInt());
    }

    @Test
    void buildSearchPayload_withConfiguredKnowledgebaseId_prefersConfigValue() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        properties.setKnowledgebaseId("kb-from-config");
        MemoryLibraryClient client = new MemoryLibraryClient(properties);

        ObjectNode payload = client.buildSearchPayload("sparklink:user:9527", "配置优先");

        assertEquals("kb-from-config", payload.path("memory_library_id").asText());
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
        assertEquals("5eb944ee565c400d8fb71e6ab119dfe3", payload.path("memory_library_id").asText());
        assertEquals(2, payload.path("messages").size());
        assertEquals("user", payload.path("messages").get(0).path("role").asText());
        assertEquals("你好", payload.path("messages").get(0).path("content").asText());
        assertEquals("assistant", payload.path("messages").get(1).path("role").asText());
        assertEquals("你好，我在", payload.path("messages").get(1).path("content").asText());
    }

    @Test
    void buildAddPayload_withNullMessage_throwsIllegalArgumentException() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", "ok"));
        messages.add(null);

        assertThrows(IllegalArgumentException.class,
                () -> client.buildAddPayload("sparklink:user:10010", messages));
    }

    @Test
    void buildAddPayload_withEmptyMessage_throwsIllegalArgumentException() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", "ok"),
                Map.of()
        );

        assertThrows(IllegalArgumentException.class,
                () -> client.buildAddPayload("sparklink:user:10010", messages));
    }

    @Test
    void buildAddPayload_withMissingRole_throwsIllegalArgumentException() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);
        List<Map<String, String>> messages = List.of(
                Map.of("content", "only-content")
        );

        assertThrows(IllegalArgumentException.class,
                () -> client.buildAddPayload("sparklink:user:10010", messages));
    }

    @Test
    void buildAddPayload_withMissingContent_throwsIllegalArgumentException() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user")
        );

        assertThrows(IllegalArgumentException.class,
                () -> client.buildAddPayload("sparklink:user:10010", messages));
    }

    @Test
    void buildAddPayload_withBlankRole_throwsIllegalArgumentException() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);
        List<Map<String, String>> messages = List.of(
                Map.of("role", " ", "content", "hello")
        );

        assertThrows(IllegalArgumentException.class,
                () -> client.buildAddPayload("sparklink:user:10010", messages));
    }

    @Test
    void buildAddPayload_withBlankContent_throwsIllegalArgumentException() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        MemoryLibraryClient client = new MemoryLibraryClient(properties);
        List<Map<String, String>> messages = List.of(
                Map.of("role", "user", "content", " ")
        );

        assertThrows(IllegalArgumentException.class,
                () -> client.buildAddPayload("sparklink:user:10010", messages));
    }

    @Test
    void searchMemory_usesMemoryNodesSearchEndpointAndMessagesPayload() {
        MemoryLibraryProperties properties = new MemoryLibraryProperties();
        properties.setApiKey("mock-key");
        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.postForEntity(eq("https://dashscope.aliyuncs.com/api/v2/apps/memory/memory_nodes/search"),
                any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"memory_nodes\":[]}", HttpStatus.OK));

        MemoryLibraryClient client = new MemoryLibraryClient(properties, new com.fasterxml.jackson.databind.ObjectMapper(), restTemplate);
        client.searchMemory("sparklink:user:10010", "今天要做什么");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://dashscope.aliyuncs.com/api/v2/apps/memory/memory_nodes/search"),
                entityCaptor.capture(), eq(String.class));
        String body = String.valueOf(entityCaptor.getValue().getBody());
        assertTrue(body.contains("\"memory_library_id\""));
        assertTrue(body.contains("\"messages\""));
        assertTrue(body.contains("\"今天要做什么\""));
    }
}
