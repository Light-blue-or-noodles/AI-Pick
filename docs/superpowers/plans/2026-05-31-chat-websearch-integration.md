# AI Chat WebSearch Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为业务 AI Chat 增加“自动判定 + 手动开关”的联网搜索能力，回答中可返回来源链接，同时保持现有聊天主流程可降级可回退。

**Architecture:** 采用独立 Orchestrator 组件化方案：`WebSearchPolicyService` 负责触发决策，`BailianWebSearchClient` 负责检索，`SearchResultFilterService` 负责黑白名单与去重，`CitationBuilder` 负责引用结构，`ChatServiceImpl` 负责编排与降级。网络检索失败时自动回落到现有站内聊天逻辑，不中断主流程。

**Tech Stack:** Spring Boot 3.2, Spring Web, Jackson, Spring AI (existing), JUnit 5, Mockito

**实施前提（工作目录）:**
- 建议在独立 worktree 执行，避免与当前并行改动冲突。

---

## 文件结构（实施前锁定）

| 路径 | 职责 |
|------|------|
| `backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatRequest.java` | 增加 `webSearchMode` 入参（`auto/on/off`） |
| `backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatResponse.java` | 增加 `citations` 与 `searchMeta` 输出字段 |
| `backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatCitationItem.java` | 联网来源引用项 DTO |
| `backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatSearchMeta.java` | 联网触发元数据 DTO |
| `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchMode.java` | 联网模式枚举与解析 |
| `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchProperties.java` | 联网配置（开关、超时、黑白名单、条数） |
| `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchPolicyService.java` | `auto/on/off` 判定 + 关键词触发 |
| `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/BailianWebSearchClient.java` | 百炼 WebSearch API 调用封装 |
| `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/SearchResultFilterService.java` | 域名过滤、去重、摘要裁剪 |
| `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/CitationBuilder.java` | 过滤结果转 `ChatCitationItem` |
| `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java` | 接入联网检索编排与降级 |
| `backend/sparklink-backend/src/main/resources/application.yml` | 增加 `app.chat.web-search.*` 配置 |
| `backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/WebSearchPolicyServiceTest.java` | 触发策略单测 |
| `backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/SearchResultFilterServiceTest.java` | 域名过滤与去重单测 |
| `backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/BailianWebSearchClientTest.java` | 请求体构造与响应解析单测 |
| `backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceWebSearchIntegrationTest.java` | `ChatServiceImpl` 联网路径集成测试 |

---

### Task 1: 扩展对话请求/响应契约（向后兼容）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatCitationItem.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatSearchMeta.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatRequest.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatResponse.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/ChatContractWebSearchTest.java`

- [ ] **Step 1: 写失败测试（默认兼容 + 新字段可读写）**

```java
@Test
void chatResponseShouldKeepBackwardCompatibility() {
    ChatResponse response = new ChatResponse("s1", "ok");
    assertNotNull(response.getRecommends());
    assertTrue(response.getCitations().isEmpty());
    assertNotNull(response.getSearchMeta());
    assertFalse(response.getSearchMeta().isTriggered());
}

@Test
void chatRequestShouldParseWebSearchMode() {
    ChatRequest req = new ChatRequest();
    req.setMessage("今天北京天气");
    req.setWebSearchMode("on");
    assertEquals("on", req.getWebSearchMode());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatContractWebSearchTest test`  
Expected: FAIL（`getCitations/getSearchMeta/getWebSearchMode` 不存在）

- [ ] **Step 3: 最小实现 DTO 与默认值**

```java
public class ChatRequest {
    @NotBlank
    @Size(max = 2000)
    private String message;
    private String sessionId;
    /** auto/on/off */
    private String webSearchMode = "auto";
}
```

```java
public class ChatResponse {
    private List<ChatRecommendItem> recommends;
    private List<ChatCitationItem> citations = Collections.emptyList();
    private ChatSearchMeta searchMeta = ChatSearchMeta.notTriggered("not_evaluated", 0);
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatContractWebSearchTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatRequest.java \
  backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatResponse.java \
  backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatCitationItem.java \
  backend/sparklink-backend/src/main/java/com/sparklink/dto/ChatSearchMeta.java \
  backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/ChatContractWebSearchTest.java
git commit -m "feat: extend chat contract for web search citations"
```

---

### Task 2: 新增联网触发策略与配置模型

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchMode.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchProperties.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchPolicyService.java`
- Modify: `backend/sparklink-backend/src/main/resources/application.yml`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/WebSearchPolicyServiceTest.java`

- [ ] **Step 1: 写失败测试（auto/on/off 与时效词触发）**

```java
@Test
void shouldForceOnWhenModeOn() {
    assertTrue(policy.shouldSearch("on", "你好").triggered());
}

@Test
void shouldDisableWhenModeOff() {
    assertFalse(policy.shouldSearch("off", "今天新闻").triggered());
}

@Test
void shouldTriggerByRecencyKeywordInAuto() {
    assertTrue(policy.shouldSearch("auto", "今天北京天气怎么样").triggered());
}

@Test
void shouldNotTriggerForSmallTalkInAuto() {
    assertFalse(policy.shouldSearch("auto", "你好呀").triggered());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=WebSearchPolicyServiceTest test`  
Expected: FAIL（策略类与判定结果类型不存在）

- [ ] **Step 3: 最小实现模式解析与判定**

```java
public enum WebSearchMode {
    AUTO, ON, OFF;
    public static WebSearchMode from(String raw) {
        if (raw == null) return AUTO;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "on" -> ON;
            case "off" -> OFF;
            default -> AUTO;
        };
    }
}
```

```java
public Decision shouldSearch(String rawMode, String message) {
    WebSearchMode mode = WebSearchMode.from(rawMode);
    if (mode == WebSearchMode.ON) return Decision.triggered("mode_on");
    if (mode == WebSearchMode.OFF) return Decision.notTriggered("mode_off");
    boolean hit = RECENCY_KEYWORDS.stream().anyMatch(message::contains);
    return hit ? Decision.triggered("auto_keyword") : Decision.notTriggered("auto_skip");
}
```

```yaml
app:
  chat:
    web-search:
      enabled: true
      timeout-ms: 5000
      max-results: 5
      domain-whitelist: []
      domain-blacklist: []
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=WebSearchPolicyServiceTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchMode.java \
  backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchProperties.java \
  backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/WebSearchPolicyService.java \
  backend/sparklink-backend/src/main/resources/application.yml \
  backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/WebSearchPolicyServiceTest.java
git commit -m "feat: add web search policy and config model"
```

---

### Task 3: 实现百炼 WebSearch 客户端封装

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/BailianWebSearchClient.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/BailianWebSearchClientTest.java`

- [ ] **Step 1: 写失败测试（请求参数与响应解析）**

```java
@Test
void shouldBuildSearchPayloadWithQueryAndCount() {
    String payload = client.buildPayload("北京今天天气", 5);
    assertTrue(payload.contains("\"query\":\"北京今天天气\""));
    assertTrue(payload.contains("\"count\":5"));
}

@Test
void shouldParseResultListFromResponse() {
    String body = """
      {"results":[{"title":"A","url":"https://a.com","snippet":"x"}]}
      """;
    List<BailianWebSearchClient.RawResult> rows = client.parseResults(body);
    assertEquals(1, rows.size());
    assertEquals("https://a.com", rows.get(0).url());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=BailianWebSearchClientTest test`  
Expected: FAIL（客户端类或 `buildPayload/parseResults` 不存在）

- [ ] **Step 3: 最小实现客户端**

```java
public String buildPayload(String query, int count) {
    ObjectNode node = objectMapper.createObjectNode();
    node.put("query", query);
    node.put("count", Math.max(1, count));
    return objectMapper.writeValueAsString(node);
}
```

```java
public List<RawResult> parseResults(String body) {
    JsonNode root = objectMapper.readTree(body);
    JsonNode arr = root.path("results");
    List<RawResult> out = new ArrayList<>();
    if (arr.isArray()) {
        for (JsonNode n : arr) {
            out.add(new RawResult(n.path("title").asText(""),
                    n.path("url").asText(""),
                    n.path("snippet").asText(""),
                    n.path("publishedAt").asText("")));
        }
    }
    return out;
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=BailianWebSearchClientTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/BailianWebSearchClient.java \
  backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/BailianWebSearchClientTest.java
git commit -m "feat: add bailian web search client"
```

---

### Task 4: 实现结果过滤与引用构建（黑白名单 + 去重）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/SearchResultFilterService.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/CitationBuilder.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/SearchResultFilterServiceTest.java`

- [ ] **Step 1: 写失败测试（黑名单剔除 + URL 去重 + 摘要裁剪）**

```java
@Test
void shouldFilterByBlacklistAndDedupe() {
    List<RawResult> in = List.of(
      new RawResult("A", "https://news.bad.com/a", "x", ""),
      new RawResult("B", "https://ok.com/1", "first", ""),
      new RawResult("B2", "https://ok.com/1", "dup", "")
    );
    FilteredResult out = service.filter(in);
    assertEquals(1, out.items().size());
    assertEquals("https://ok.com/1", out.items().get(0).url());
}

@Test
void shouldBuildCitationItems() {
    List<ChatCitationItem> citations = builder.build(List.of(
      new SearchResultFilterService.FilteredItem("标题", "https://ok.com/a", "ok.com", "摘要", "")
    ));
    assertEquals(1, citations.size());
    assertEquals("ok.com", citations.get(0).getDomain());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=SearchResultFilterServiceTest test`  
Expected: FAIL（过滤器或构建器未实现）

- [ ] **Step 3: 最小实现过滤器与引用构建**

```java
boolean allowed(String domain) {
    if (!blacklist.isEmpty() && blacklist.contains(domain)) return false;
    return whitelist.isEmpty() || whitelist.contains(domain);
}
```

```java
if (seenUrl.add(url) && allowed(domain)) {
    out.add(new FilteredItem(title, url, domain, truncate(snippet, 180), publishedAt));
}
```

```java
public List<ChatCitationItem> build(List<FilteredItem> items) {
    return items.stream().map(i -> new ChatCitationItem(i.title(), i.url(), i.domain(),
            i.snippet(), i.publishedAt())).toList();
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=SearchResultFilterServiceTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/SearchResultFilterService.java \
  backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/CitationBuilder.java \
  backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/SearchResultFilterServiceTest.java
git commit -m "feat: add web search filtering and citation builder"
```

---

### Task 5: 在 ChatServiceImpl 接入联网编排与降级

**Files:**
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceWebSearchIntegrationTest.java`

- [ ] **Step 1: 写失败测试（on/off/auto 三模式与降级）**

```java
@Test
void shouldUseWebSearchWhenModeOn() {
    ChatRequest req = new ChatRequest();
    req.setMessage("今天北京天气");
    req.setWebSearchMode("on");
    chatService.chat(1L, req);
    verify(webSearchClient).search(anyString(), anyInt());
}

@Test
void shouldSkipWebSearchWhenModeOff() {
    ChatRequest req = new ChatRequest();
    req.setMessage("今天北京天气");
    req.setWebSearchMode("off");
    chatService.chat(1L, req);
    verify(webSearchClient, never()).search(anyString(), anyInt());
}

@Test
void shouldFallbackToNormalReplyWhenSearchThrows() {
    when(webSearchClient.search(anyString(), anyInt())).thenThrow(new RuntimeException("timeout"));
    ChatResponse resp = chatService.chat(1L, reqOn);
    assertNotNull(resp.getReply());
    assertEquals("search_failed_fallback", resp.getSearchMeta().getReason());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatServiceWebSearchIntegrationTest test`  
Expected: FAIL（`ChatServiceImpl` 未注入联网组件）

- [ ] **Step 3: 最小实现编排路径**

```java
Decision decision = webSearchPolicyService.shouldSearch(request.getWebSearchMode(), request.getMessage());
List<ChatCitationItem> citations = List.of();
ChatSearchMeta searchMeta = ChatSearchMeta.notTriggered(decision.reason(), 0);
String webContext = "";

if (decision.triggered()) {
    try {
        List<RawResult> raw = bailianWebSearchClient.search(request.getMessage(), webSearchProperties.getMaxResults());
        FilteredResult filtered = searchResultFilterService.filter(raw);
        citations = citationBuilder.build(filtered.items());
        webContext = searchResultFilterService.toPromptContext(filtered.items());
        searchMeta = ChatSearchMeta.triggered(decision.reason(), filtered.filteredCount());
    } catch (Exception ex) {
        searchMeta = ChatSearchMeta.notTriggered("search_failed_fallback", 0);
    }
}
```

```java
String reply = generateReplyWithHistory(request.getMessage(), history, matchContext, webContext);
ChatResponse response = new ChatResponse(sessionId, reply, recommends);
response.setCitations(citations);
response.setSearchMeta(searchMeta);
```

并新增重载：

```java
private String generateReplyWithHistory(String currentMessage, List<ChatMessage> history,
                                        String matchContext, String webContext) {
    // webContext 非空时作为 system message 注入
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatServiceWebSearchIntegrationTest test`  
Expected: PASS

- [ ] **Step 5: 运行 chat 相关回归**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatIntentToolGateTest,ChatRecommendFallbackServiceTest,ChatServiceWebSearchIntegrationTest test`  
Expected: PASS（旧能力不回归）

- [ ] **Step 6: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java \
  backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceWebSearchIntegrationTest.java
git commit -m "feat: integrate web search orchestration into chat service"
```

---

### Task 6: 端到端验证与文档补充

**Files:**
- Modify: `backend/sparklink-backend/src/main/resources/application.yml`
- Modify: `backend/sparklink-backend/README.md`

- [ ] **Step 1: 补充配置说明文档**

```markdown
### AI Chat 联网搜索配置
- app.chat.web-search.enabled=true|false
- app.chat.web-search.timeout-ms=5000
- app.chat.web-search.max-results=5
- app.chat.web-search.domain-whitelist=[]
- app.chat.web-search.domain-blacklist=[]
- ChatRequest.webSearchMode: auto/on/off
```

- [ ] **Step 2: 本地编译验证**

Run: `cd backend/sparklink-backend && mvn compile`  
Expected: BUILD SUCCESS

- [ ] **Step 3: 运行最小回归测试集**

Run: `cd backend/sparklink-backend && mvn -Dtest=WebSearchPolicyServiceTest,BailianWebSearchClientTest,SearchResultFilterServiceTest,ChatServiceWebSearchIntegrationTest test`  
Expected: PASS

- [ ] **Step 4: 完整测试（可选，发布前）**

Run: `cd backend/sparklink-backend && mvn test`  
Expected: 全量 PASS（时间较长）

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/resources/application.yml \
  backend/sparklink-backend/README.md
git commit -m "docs: add chat web search configuration and rollout notes"
```

---

## Spec 覆盖自检

| 需求 | 对应 Task |
|------|-----------|
| 混合触发（auto + 手动 on/off） | Task 2, Task 5 |
| 全网检索 + 黑白名单过滤 | Task 2, Task 4 |
| 返回来源链接（citation） | Task 1, Task 4, Task 5 |
| 失败自动降级不影响主流程 | Task 5 |
| 向后兼容旧客户端 | Task 1 |

---

## Placeholder 扫描结果

- 已检查：无 `TODO/TBD/后续补充` 占位符。
- 每个任务包含：文件路径、测试代码、运行命令、预期结果、提交命令。

---

## 类型一致性检查

- `ChatRequest.webSearchMode` 统一字符串输入，内部统一经 `WebSearchMode.from(...)` 解析。
- `ChatResponse.citations` 统一 `List<ChatCitationItem>`。
- `ChatResponse.searchMeta` 统一 `ChatSearchMeta`，字段固定 `triggered/reason/filteredCount`。

---

Plan complete and saved to `docs/superpowers/plans/2026-05-31-chat-websearch-integration.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
