# Dify Cloud Knowledge Retrieval V2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在保持现有 `/chat` 接口不变的前提下，为后端接入 Dify Cloud 知识库检索能力（`/datasets/{dataset_id}/retrieve`），并具备可降级、可观测、可回归验证的生产能力。

**Architecture:** 新增 `knowledge` 子模块（config/client/model/service/metrics），由 `ChatServiceImpl` 在 fallback 分支调用 `KnowledgeRetrieveGateway` 检索上下文，再通过 `PromptKnowledgeAssembler` 安全注入 Prompt。Dify 失败时统一降级为空检索结果，不阻断主回答。所有密钥仅服务端保存，前端继续只调 `/chat`。

**Tech Stack:** Spring Boot 3.2, Jackson, RestClient, Resilience4j（可选）, JUnit5, Mockito

---

## 文件结构（实施前锁定）

| 路径 | 职责 |
|------|------|
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/config/DifyKnowledgeProperties.java` | Dify 连接与检索参数配置 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveRequest.java` | 内部检索请求模型 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRecord.java` | 单条检索结果模型 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveResult.java` | 检索结果聚合（命中、fallback、原因） |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeFallbackReason.java` | 降级原因枚举 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/client/DifyKnowledgeClient.java` | 调用 Dify 检索 API |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/PromptKnowledgeAssembler.java` | 检索上下文截断与注入 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/KnowledgeRetrieveGateway.java` | 统一检索入口与降级逻辑 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/metrics/KnowledgeMetricsRecorder.java` | 检索指标记录 |
| `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java` | 接入知识检索调用 |
| `backend/sparklink-backend/src/main/resources/application.yml` | Dify 配置项 |
| `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/**` | 知识模块单测/集测 |
| `backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceKnowledgeIntegrationTest.java` | Chat 链路回归测试 |

---

### Task 1: 建立 Dify 配置与领域模型

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/config/DifyKnowledgeProperties.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveRequest.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRecord.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveResult.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeFallbackReason.java`
- Modify: `backend/sparklink-backend/src/main/resources/application.yml`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/config/DifyKnowledgePropertiesTest.java`

- [ ] **Step 1: 写失败测试（配置默认值）**

```java
@Test
void shouldLoadDefaultRetrieveConfig() {
    DifyKnowledgeProperties props = new DifyKnowledgeProperties();
    assertEquals("https://api.dify.ai/v1", props.getBaseUrl());
    assertEquals(6, props.getRetrieve().getTopK());
    assertTrue(props.getRetrieve().isThresholdEnabled());
    assertEquals(0.55d, props.getRetrieve().getScoreThreshold());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=DifyKnowledgePropertiesTest test`  
Expected: FAIL with `cannot find symbol DifyKnowledgeProperties`

- [ ] **Step 3: 写最小实现**

```java
@ConfigurationProperties(prefix = "app.dify")
public class DifyKnowledgeProperties {
    private String baseUrl = "https://api.dify.ai/v1";
    private String apiKey;
    private String datasetId;
    private int timeoutMs = 6000;
    private final Retrieve retrieve = new Retrieve();

    public static class Retrieve {
        private int topK = 6;
        private boolean thresholdEnabled = true;
        private double scoreThreshold = 0.55d;
        private String searchMethod = "hybrid_search";
        private boolean rerankingEnable = false;
        private int maxInjectRecords = 4;
        private int maxInjectCharsPerRecord = 400;
        // getters/setters
    }
    // getters/setters
}
```

```yaml
app:
  dify:
    base-url: https://api.dify.ai/v1
    api-key: ${DIFY_API_KEY:}
    dataset-id: ${DIFY_DATASET_ID:}
    timeout-ms: 6000
    retrieve:
      top-k: 6
      threshold-enabled: true
      score-threshold: 0.55
      search-method: hybrid_search
      reranking-enable: false
      max-inject-records: 4
      max-inject-chars-per-record: 400
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=DifyKnowledgePropertiesTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/knowledge/config/DifyKnowledgeProperties.java \
  backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveRequest.java \
  backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRecord.java \
  backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveResult.java \
  backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeFallbackReason.java \
  backend/sparklink-backend/src/main/resources/application.yml \
  backend/sparklink-backend/src/test/java/com/sparklink/knowledge/config/DifyKnowledgePropertiesTest.java
git commit -m "feat: add dify knowledge config and domain models"
```

---

### Task 2: 实现 DifyKnowledgeClient（请求/响应契约）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/client/DifyKnowledgeClient.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/client/DifyKnowledgeClientTest.java`

- [ ] **Step 1: 写失败测试（路径、鉴权头、query 参数）**

```java
@Test
void shouldCallRetrieveEndpointWithBearerToken() {
    KnowledgeRetrieveRequest req = new KnowledgeRetrieveRequest(10086L, "羽毛球搭子怎么找");
    when(restClient.post(anyString(), anyMap(), anyMap(), anyString())).thenReturn("""
      {"query":{"content":"羽毛球搭子怎么找"},"records":[]}
      """);

    client.retrieve(req);

    verify(restClient).post(
        contains("/datasets/ds_test/retrieve"),
        argThat(h -> ("Bearer test-key").equals(h.get("Authorization"))),
        anyMap(),
        anyString()
    );
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=DifyKnowledgeClientTest test`  
Expected: FAIL with `cannot find symbol DifyKnowledgeClient`

- [ ] **Step 3: 写最小实现**

```java
public class DifyKnowledgeClient {
    public KnowledgeRetrieveResult retrieve(KnowledgeRetrieveRequest request) {
        String path = properties.getBaseUrl() + "/datasets/" + properties.getDatasetId() + "/retrieve";
        Map<String, String> headers = Map.of("Authorization", "Bearer " + properties.getApiKey());
        Map<String, Object> body = Map.of(
            "query", truncate(request.getQuery(), 250),
            "retrieval_model", Map.of(
                "search_method", properties.getRetrieve().getSearchMethod(),
                "top_k", properties.getRetrieve().getTopK(),
                "score_threshold_enabled", properties.getRetrieve().isThresholdEnabled(),
                "score_threshold", properties.getRetrieve().getScoreThreshold(),
                "reranking_enable", properties.getRetrieve().isRerankingEnable()
            )
        );
        String raw = simpleHttpClient.post(path, headers, objectMapper.writeValueAsString(body));
        return parseResult(raw);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=DifyKnowledgeClientTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/knowledge/client/DifyKnowledgeClient.java \
  backend/sparklink-backend/src/test/java/com/sparklink/knowledge/client/DifyKnowledgeClientTest.java
git commit -m "feat: implement dify knowledge retrieve client"
```

---

### Task 3: 实现 PromptKnowledgeAssembler（排序/截断/去重）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/PromptKnowledgeAssembler.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/service/PromptKnowledgeAssemblerTest.java`

- [ ] **Step 1: 写失败测试（按 score 排序并限制条数）**

```java
@Test
void shouldSortByScoreAndLimitRecords() {
    PromptKnowledgeAssembler assembler = new PromptKnowledgeAssembler(2, 20);
    KnowledgeRetrieveResult result = KnowledgeRetrieveResult.hit(List.of(
        new KnowledgeRecord("s1", "d1", "content-1", 0.61d),
        new KnowledgeRecord("s2", "d1", "content-2", 0.95d),
        new KnowledgeRecord("s3", "d1", "content-3", 0.72d)
    ));
    String merged = assembler.merge("原问题", result);
    assertTrue(merged.contains("content-2"));
    assertTrue(merged.contains("content-3"));
    assertFalse(merged.contains("content-1"));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=PromptKnowledgeAssemblerTest test`  
Expected: FAIL with `cannot find symbol PromptKnowledgeAssembler`

- [ ] **Step 3: 写最小实现**

```java
public String merge(String userMessage, KnowledgeRetrieveResult result) {
    if (result == null || result.records().isEmpty()) {
        return userMessage;
    }
    List<KnowledgeRecord> records = result.records().stream()
        .filter(r -> r.content() != null && !r.content().isBlank())
        .collect(Collectors.toMap(KnowledgeRecord::segmentId, Function.identity(), (a, b) -> a))
        .values().stream()
        .sorted(Comparator.comparing(KnowledgeRecord::score).reversed())
        .limit(maxInjectRecords)
        .toList();
    StringBuilder sb = new StringBuilder("【知识库参考】\n");
    for (int i = 0; i < records.size(); i++) {
        String content = truncate(records.get(i).content(), maxInjectChars);
        sb.append(i + 1).append(". ").append(content).append('\n');
    }
    sb.append("【用户问题】").append(userMessage);
    return sb.toString();
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=PromptKnowledgeAssemblerTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/PromptKnowledgeAssembler.java \
  backend/sparklink-backend/src/test/java/com/sparklink/knowledge/service/PromptKnowledgeAssemblerTest.java
git commit -m "feat: add knowledge prompt assembler"
```

---

### Task 4: 实现 KnowledgeRetrieveGateway（降级 + 熔断）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/KnowledgeRetrieveGateway.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/service/KnowledgeRetrieveGatewayTest.java`

- [ ] **Step 1: 写失败测试（Dify 抛错时返回 fallback）**

```java
@Test
void shouldFallbackWhenDifyThrowsException() {
    when(client.retrieve(any())).thenThrow(new RuntimeException("timeout"));
    KnowledgeRetrieveResult result = gateway.retrieve(10086L, "今天有什么活动");
    assertTrue(result.fallback());
    assertEquals(KnowledgeFallbackReason.SEARCH_FAILED, result.reason());
    assertTrue(result.records().isEmpty());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=KnowledgeRetrieveGatewayTest test`  
Expected: FAIL

- [ ] **Step 3: 写最小实现**

```java
public KnowledgeRetrieveResult retrieve(Long userId, String query) {
    if (!enabled || query == null || query.isBlank()) {
        return KnowledgeRetrieveResult.fallback(KnowledgeFallbackReason.NOT_TRIGGERED);
    }
    try {
        return difyKnowledgeClient.retrieve(new KnowledgeRetrieveRequest(userId, query));
    } catch (Exception ex) {
        log.warn("Dify retrieve failed, fallback. userId={}", userId, ex);
        return KnowledgeRetrieveResult.fallback(KnowledgeFallbackReason.SEARCH_FAILED);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=KnowledgeRetrieveGatewayTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/KnowledgeRetrieveGateway.java \
  backend/sparklink-backend/src/test/java/com/sparklink/knowledge/service/KnowledgeRetrieveGatewayTest.java
git commit -m "feat: add knowledge retrieve gateway with fallback"
```

---

### Task 5: 接入 ChatServiceImpl 主链路

**Files:**
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceKnowledgeIntegrationTest.java`

- [ ] **Step 1: 写失败测试（fallback 分支调用检索与注入）**

```java
@Test
void shouldRetrieveKnowledgeBeforeFallbackReply() {
    when(orchestrator.orchestrate(anyLong(), anyString(), anyList()))
        .thenReturn(ChatToolOrchestrator.OrchestrationResult.none());
    when(gateway.retrieve(eq(10086L), anyString()))
        .thenReturn(KnowledgeRetrieveResult.hit(List.of(
            new KnowledgeRecord("s1", "d1", "知识片段", 0.9d)
        )));
    service.chat(10086L, request("我想找羽毛球搭子"));
    verify(gateway).retrieve(eq(10086L), eq("我想找羽毛球搭子"));
    verify(assembler).merge(eq("我想找羽毛球搭子"), any(KnowledgeRetrieveResult.class));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatServiceKnowledgeIntegrationTest test`  
Expected: FAIL

- [ ] **Step 3: 写最小实现**

```java
KnowledgeRetrieveResult knowledgeResult = knowledgeRetrieveGateway.retrieve(userId, message);
String matchContext = ChatRecommendAssembler.buildMatchContextString(recommends);
String memoryMergedPrompt = memoryFacade.mergePrompt(matchContext, memoryContext);
String finalPrompt = promptKnowledgeAssembler.merge(memoryMergedPrompt, knowledgeResult);
reply = generateReplyWithHistory(message, history, finalPrompt, webSearchRuntime.webContext());
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatServiceKnowledgeIntegrationTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java \
  backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceKnowledgeIntegrationTest.java
git commit -m "feat: integrate dify knowledge retrieval into chat fallback flow"
```

---

### Task 6: 指标、文档与端到端验收

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/metrics/KnowledgeMetricsRecorder.java`
- Create: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/KnowledgeE2ETest.java`
- Modify: `backend/sparklink-backend/README.md`

- [ ] **Step 1: 写失败测试（指标计数）**

```java
@Test
void shouldRecordFallbackAndSuccess() {
    KnowledgeMetricsRecorder recorder = new KnowledgeMetricsRecorder();
    recorder.recordSuccess(120);
    recorder.recordFallback(KnowledgeFallbackReason.SEARCH_FAILED);
    assertEquals(1, recorder.snapshot().successCount());
    assertEquals(1, recorder.snapshot().fallbackCount());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=KnowledgeE2ETest test`  
Expected: FAIL

- [ ] **Step 3: 写最小实现 + README 配置说明**

```markdown
### Dify Cloud Knowledge Retrieval
- DIFY_API_KEY=...
- DIFY_DATASET_ID=...
- app.dify.base-url=https://api.dify.ai/v1
- app.dify.retrieve.top-k=6
```

- [ ] **Step 4: 运行全量测试**

Run: `cd backend/sparklink-backend && mvn test`  
Expected: PASS

- [ ] **Step 5: 编译校验**

Run: `cd backend/sparklink-backend && mvn compile`  
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/knowledge/metrics/KnowledgeMetricsRecorder.java \
  backend/sparklink-backend/src/test/java/com/sparklink/knowledge/KnowledgeE2ETest.java \
  backend/sparklink-backend/README.md
git commit -m "test: add knowledge retrieval metrics and e2e validation"
```

---

## Spec 覆盖自检

| Spec 条目 | 对应任务 |
|---|---|
| 后端代理 Dify、前端不直连 | Task 1, Task 2, Task 5 |
| 检索接口 `/datasets/{dataset_id}/retrieve` | Task 2 |
| 检索结果安全注入 Prompt | Task 3, Task 5 |
| 错误码处理与可降级 | Task 4 |
| 可观测与告警基础 | Task 6 |
| 验收与回归 | Task 5, Task 6 |

## Placeholder 扫描结果

- 未使用 TBD/TODO/后续补充。
- 每个任务均包含文件、测试、命令、预期结果与提交步骤。

## 类型一致性检查

- `KnowledgeRetrieveGateway#retrieve(Long userId, String query)` 为唯一检索入口。
- `KnowledgeRetrieveResult` 在 client/gateway/assembler/chat 中保持同一类型。
- `KnowledgeFallbackReason` 为统一降级原因枚举，避免字符串散落。

