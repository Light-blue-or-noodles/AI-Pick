package com.sparklink.ai.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BailianWebSearchClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BailianWebSearchClient client = new BailianWebSearchClient(objectMapper);

    @Test
    void shouldBuildSearchPayloadWithJsonStructure() throws Exception {
        String payload = client.buildPayload("北京今天天气", 5);
        JsonNode root = objectMapper.readTree(payload);
        assertEquals("2.0", root.path("jsonrpc").asText());
        assertEquals("tools/call", root.path("method").asText());
        assertEquals("bailian_web_search", root.path("params").path("name").asText());
        assertEquals("北京今天天气", root.path("params").path("arguments").path("query").asText());
        assertEquals(5, root.path("params").path("arguments").path("count").asInt());
    }

    @Test
    void shouldNormalizeCountToAtLeastOne() throws Exception {
        String payload = client.buildPayload("北京今天天气", 0);
        JsonNode root = objectMapper.readTree(payload);
        assertEquals(1, root.path("params").path("arguments").path("count").asInt());
    }

    @Test
    void shouldParseResultListFromResponse() {
        String body = """
                {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"{\\"request_id\\":\\"x\\",\\"status\\":0,\\"pages\\":[{\\"title\\":\\"A\\",\\"url\\":\\"https://a.com\\",\\"snippet\\":\\"x\\"}]}"}]},"isError":false}
                """;
        List<BailianWebSearchClient.RawResult> rows = client.parseResults(body);
        assertEquals(1, rows.size());
        assertEquals("A", rows.get(0).title());
        assertEquals("https://a.com", rows.get(0).url());
        assertEquals("x", rows.get(0).snippet());
        assertEquals("", rows.get(0).publishedAt());
    }

    @Test
    void shouldThrowWhenToolReturnsErrorStatus() {
        String body = """
                {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"{\\"request_id\\":\\"x\\",\\"status\\":429}"}]},"isError":true}
                """;
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> client.parseResults(body));
        assertTrue(exception.getMessage().contains("status=429"));
    }

    @Test
    void shouldReturnEmptyListWhenResultsMissing() {
        List<BailianWebSearchClient.RawResult> rows = client.parseResults("{\"ok\":true}");
        assertTrue(rows.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenBodyIsNull() {
        List<BailianWebSearchClient.RawResult> rows = client.parseResults(null);
        assertTrue(rows.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenBodyIsBlank() {
        List<BailianWebSearchClient.RawResult> rows = client.parseResults("   ");
        assertTrue(rows.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenBodyIsInvalidJson() {
        List<BailianWebSearchClient.RawResult> rows = client.parseResults("{invalid}");
        assertTrue(rows.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenResultsIsNotArray() {
        String body = """
                {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"{\\"request_id\\":\\"x\\",\\"status\\":0,\\"pages\\":{}}"}]},"isError":false}
                """;
        List<BailianWebSearchClient.RawResult> rows = client.parseResults(body);
        assertTrue(rows.isEmpty());
    }

    @Test
    void shouldDefaultMissingFieldsToEmptyString() {
        String body = """
                {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"{\\"request_id\\":\\"x\\",\\"status\\":0,\\"pages\\":[{\\"title\\":\\"A\\"}]}"}]},"isError":false}
                """;
        List<BailianWebSearchClient.RawResult> rows = client.parseResults(body);
        assertEquals(1, rows.size());
        assertEquals("A", rows.get(0).title());
        assertEquals("", rows.get(0).url());
        assertEquals("", rows.get(0).snippet());
        assertEquals("", rows.get(0).publishedAt());
    }

    @Test
    void shouldRejectNonHttpsEndpointBeforeCallingSearch() {
        ReflectionTestUtils.setField(client, "endpoint", "http://example.com/search");
        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> client.search("今天新闻", 3));
        assertTrue(exception.getMessage().contains("https"));
    }

    @Test
    void shouldBuildToolsListPayload() throws Exception {
        String payload = client.buildToolsListPayload();
        JsonNode root = objectMapper.readTree(payload);
        assertEquals("tools/list", root.path("method").asText());
    }

    @Test
    void shouldBuildInitializePayload() throws Exception {
        String payload = client.buildMcpInitializePayload();
        JsonNode root = objectMapper.readTree(payload);
        assertEquals("initialize", root.path("method").asText());
        assertEquals("sparklink-backend", root.path("params").path("clientInfo").path("name").asText());
    }
}
