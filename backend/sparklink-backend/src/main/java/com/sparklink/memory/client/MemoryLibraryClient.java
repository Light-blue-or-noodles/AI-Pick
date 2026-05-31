package com.sparklink.memory.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sparklink.memory.config.MemoryLibraryProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 记忆库客户端（仅负责构建 Search / Add 请求载荷）。
 *
 * @author AI-Pick
 */
public class MemoryLibraryClient {

    private final MemoryLibraryProperties properties;
    private final ObjectMapper objectMapper;

    public MemoryLibraryClient(MemoryLibraryProperties properties) {
        this(properties, new ObjectMapper());
    }

    public MemoryLibraryClient(MemoryLibraryProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public ObjectNode buildSearchPayload(String userId, String query) {
        Assert.hasText(userId, "userId 不能为空");
        Assert.hasText(query, "query 不能为空");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("user_id", userId);
        payload.put("query", query);
        payload.set("knowledgebase_ids", buildKnowledgebaseIdsNode());
        return payload;
    }

    public ObjectNode buildAddPayload(String userId, List<Map<String, String>> messages) {
        Assert.hasText(userId, "userId 不能为空");
        Assert.notNull(messages, "messages 不能为空");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("user_id", userId);
        payload.set("knowledgebase_ids", buildKnowledgebaseIdsNode());

        ArrayNode messageNodes = payload.putArray("messages");
        for (Map<String, String> message : messages) {
            ObjectNode messageNode = messageNodes.addObject();
            if (message == null || message.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, String> entry : message.entrySet()) {
                if (entry.getValue() != null) {
                    messageNode.put(entry.getKey(), entry.getValue());
                } else {
                    messageNode.putNull(entry.getKey());
                }
            }
        }
        return payload;
    }

    private ArrayNode buildKnowledgebaseIdsNode() {
        Assert.notNull(properties, "memoryLibraryProperties 不能为空");
        String knowledgebaseId = properties.getKnowledgebaseId();
        Assert.isTrue(StringUtils.hasText(knowledgebaseId), "knowledgebaseId 不能为空");

        ArrayNode knowledgebaseIds = objectMapper.createArrayNode();
        knowledgebaseIds.add(knowledgebaseId);
        return knowledgebaseIds;
    }
}
