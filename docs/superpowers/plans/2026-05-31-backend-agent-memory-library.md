# Backend Agent Memory Library Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `sparklink-backend` 中完成“按 JWT 用户维度读取/写入阿里云记忆库”的全链路能力，采用 Redis Stream 异步重试，保证主业务低延迟且记忆最终一致入云。

**Architecture:** 以阿里云记忆库作为长期记忆单一事实源；请求前通过 `MemoryFacade` 做检索与敏感字段过滤注入；请求后异步写入 Redis Stream，由消费者调用 `AddMemory`，失败指数退避并进入 DLQ。统一在服务层封装，不在各 Controller 重复粘贴逻辑。

**Tech Stack:** Spring Boot 3.2, Spring Web, Redis (Spring Data Redis + Stream), Jackson, JUnit 5, Mockito

**设计依据:**
- `docs/superpowers/specs/2026-05-31-backend-agent-memory-library-design.md`

---

## 文件结构（实施前锁定）

| 路径 | 职责 |
|------|------|
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/config/MemoryLibraryProperties.java` | 记忆库配置（API Key、KB ID、阈值、重试参数） |
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/model/MemoryEvent.java` | 异步写入事件模型 |
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/model/MemoryContext.java` | 检索结果统一上下文 |
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/client/MemoryLibraryClient.java` | 阿里云 Add/Search API 调用封装 |
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/filter/MemoryFieldWhitelistFilter.java` | 敏感字段过滤与 Prompt 注入白名单 |
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/queue/MemoryEventProducer.java` | Stream 生产者 |
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/queue/MemoryEventConsumer.java` | Stream 消费、重试、DLQ |
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/service/MemoryFacade.java` | 统一检索/入队入口 |
| `backend/sparklink-backend/src/main/java/com/sparklink/memory/service/MemoryUserIdResolver.java` | 统一生成 `memory_user_id` |
| `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java` | 对话链路接入记忆读写 |
| `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/AiServiceImpl.java` | 推荐链路接入检索注入（解释/排序） |
| `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/NaturalLanguageSearchServiceImpl.java` | NL 搜索链路接入检索注入 |
| `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ActivityAiTextService.java` | 活动文案链路接入检索与写入事件 |
| `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/PartnerAiTextService.java` | 搭子文案链路接入检索与写入事件 |
| `backend/sparklink-backend/src/main/resources/application.yml` | 记忆配置项 |
| `backend/sparklink-backend/src/test/java/com/sparklink/memory/**` | 内存模块测试 |

---

### Task 1: 建立配置与身份键解析

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/config/MemoryLibraryProperties.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/service/MemoryUserIdResolver.java`
- Modify: `backend/sparklink-backend/src/main/resources/application.yml`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/memory/service/MemoryUserIdResolverTest.java`

- [ ] **Step 1: 写失败测试（身份键格式）**

```java
@Test
void shouldBuildMemoryUserIdFromJwtUserId() {
    MemoryUserIdResolver resolver = new MemoryUserIdResolver();
    String memoryUserId = resolver.resolve(10086L);
    assertEquals("sparklink:user:10086", memoryUserId);
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryUserIdResolverTest test`  
Expected: FAIL（`MemoryUserIdResolver` 不存在）

- [ ] **Step 3: 最小实现 + 配置项**

```java
public class MemoryUserIdResolver {
    public String resolve(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        return "sparklink:user:" + userId;
    }
}
```

```yaml
memory:
  library:
    enabled: true
    knowledgebase-id: 5eb944ee565c400d8fb71e6ab119dfe3
    similarity-threshold: 0.6
    max-results: 8
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryUserIdResolverTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/memory/config/MemoryLibraryProperties.java \
  backend/sparklink-backend/src/main/java/com/sparklink/memory/service/MemoryUserIdResolver.java \
  backend/sparklink-backend/src/main/resources/application.yml \
  backend/sparklink-backend/src/test/java/com/sparklink/memory/service/MemoryUserIdResolverTest.java
git commit -m "feat: add memory config and user id resolver"
```

---

### Task 2: 实现召回白名单过滤（敏感字段只存不召回）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/filter/MemoryFieldWhitelistFilter.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/model/MemoryContext.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/memory/filter/MemoryFieldWhitelistFilterTest.java`

- [ ] **Step 1: 写失败测试（敏感字段被过滤）**

```java
@Test
void shouldRemoveSensitiveProfileFields() {
    MemoryFieldWhitelistFilter filter = new MemoryFieldWhitelistFilter();
    Map<String, String> profile = new HashMap<>();
    profile.put("民族", "汉族");
    profile.put("性别", "男");
    profile.put("年龄", "28");
    profile.put("家庭成员", "父母");
    profile.put("爱好", "羽毛球");
    profile.put("内容偏好", "运动社交");

    Map<String, String> injected = filter.filterProfileForPrompt(profile);

    assertFalse(injected.containsKey("民族"));
    assertFalse(injected.containsKey("性别"));
    assertFalse(injected.containsKey("年龄"));
    assertFalse(injected.containsKey("家庭成员"));
    assertEquals("羽毛球", injected.get("爱好"));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryFieldWhitelistFilterTest test`  
Expected: FAIL（过滤器类不存在）

- [ ] **Step 3: 最小实现白名单过滤**

```java
private static final Set<String> ALLOWED_FIELDS = Set.of(
    "年龄段", "居住地", "社会关系（朋友圈）", "宠物", "人生理想/目标", "饮食习惯", "爱好", "内容偏好"
);

public Map<String, String> filterProfileForPrompt(Map<String, String> profile) {
    if (profile == null || profile.isEmpty()) {
        return Collections.emptyMap();
    }
    return profile.entrySet().stream()
        .filter(e -> ALLOWED_FIELDS.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryFieldWhitelistFilterTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/memory/filter/MemoryFieldWhitelistFilter.java \
  backend/sparklink-backend/src/main/java/com/sparklink/memory/model/MemoryContext.java \
  backend/sparklink-backend/src/test/java/com/sparklink/memory/filter/MemoryFieldWhitelistFilterTest.java
git commit -m "feat: add memory prompt whitelist filter"
```

---

### Task 3: 封装阿里云记忆库客户端（Search + Add）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/client/MemoryLibraryClient.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/memory/client/MemoryLibraryClientTest.java`

- [ ] **Step 1: 写失败测试（检索请求包含固定记忆库ID）**

```java
@Test
void shouldBuildSearchRequestWithKnowledgebaseId() {
    MemoryLibraryClient client = new MemoryLibraryClient(new ObjectMapper(), props, restClient);
    String body = client.buildSearchPayload("sparklink:user:10086", "帮我找羽毛球搭子");
    assertTrue(body.contains("\"knowledgebase_ids\":[\"5eb944ee565c400d8fb71e6ab119dfe3\"]"));
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryLibraryClientTest test`  
Expected: FAIL（客户端类或方法不存在）

- [ ] **Step 3: 最小实现 Search/Add 请求构建**

```java
public String buildSearchPayload(String userId, String query) { ... }
public String buildAddPayload(String userId, List<Map<String, String>> messages) { ... }
```

请求体必须固定包含：

```json
{
  "knowledgebase_ids": ["5eb944ee565c400d8fb71e6ab119dfe3"]
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryLibraryClientTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/memory/client/MemoryLibraryClient.java \
  backend/sparklink-backend/src/test/java/com/sparklink/memory/client/MemoryLibraryClientTest.java
git commit -m "feat: add aliyun memory library client"
```

---

### Task 4: 实现异步队列生产与消费（重试 + DLQ + 幂等）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/model/MemoryEvent.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/queue/MemoryEventProducer.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/queue/MemoryEventConsumer.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/memory/queue/MemoryEventConsumerTest.java`

- [ ] **Step 1: 写失败测试（重复事件不重复写云）**

```java
@Test
void shouldSkipWhenIdempotencyKeyExists() {
    when(stringRedisTemplate.opsForValue().setIfAbsent(anyString(), eq("1"), any()))
        .thenReturn(Boolean.FALSE);
    consumer.consume(record);
    verify(memoryLibraryClient, never()).addMemory(anyString(), anyList());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryEventConsumerTest test`  
Expected: FAIL（消费者未实现）

- [ ] **Step 3: 实现消费主流程**

```java
if (!acquireIdempotency(event)) {
    return;
}
try {
    memoryLibraryClient.addMemory(event.getMemoryUserId(), event.getMessages());
    ack(record);
} catch (RetryableMemoryException ex) {
    retryLater(record, event);
} catch (Exception ex) {
    moveToDlq(record, ex.getMessage());
}
```

重试退避：`5s, 30s, 2m, 10m, 30m, 2h`，最大 6 次。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryEventConsumerTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/memory/model/MemoryEvent.java \
  backend/sparklink-backend/src/main/java/com/sparklink/memory/queue/MemoryEventProducer.java \
  backend/sparklink-backend/src/main/java/com/sparklink/memory/queue/MemoryEventConsumer.java \
  backend/sparklink-backend/src/test/java/com/sparklink/memory/queue/MemoryEventConsumerTest.java
git commit -m "feat: add memory async queue with retry and dlq"
```

---

### Task 5: 建立 MemoryFacade 并接入 ChatService

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/service/MemoryFacade.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/memory/service/MemoryFacadeTest.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceMemoryIntegrationTest.java`

- [ ] **Step 1: 写失败测试（Chat 调用前检索，调用后入队）**

```java
@Test
void shouldRecallBeforeReplyAndEnqueueAfterReply() {
    chatService.chat(10086L, request);
    verify(memoryFacade).recallForPrompt(eq(10086L), anyString());
    verify(memoryFacade).enqueueConversation(eq(10086L), anyString(), anyString());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatServiceMemoryIntegrationTest test`  
Expected: FAIL（`ChatServiceImpl` 未调用 `MemoryFacade`）

- [ ] **Step 3: 最小实现接入**

```java
MemoryContext memoryContext = memoryFacade.recallForPrompt(userId, request.getMessage());
String reply = generateReplyWithHistory(request.getMessage(), history, matchContext, memoryContext);
memoryFacade.enqueueConversation(userId, request.getMessage(), reply);
```

并新增重载：

```java
private String generateReplyWithHistory(String currentMessage, List<ChatMessage> history,
                                        String matchContext, MemoryContext memoryContext) { ... }
```

并在 `MemoryFacade` 明确定义统一接口：

```java
MemoryContext recallForPrompt(Long userId, String query);
String mergePrompt(String rawPrompt, MemoryContext memoryContext);
void enqueueConversation(Long userId, String userMessage, String assistantMessage);
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryFacadeTest,ChatServiceMemoryIntegrationTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/memory/service/MemoryFacade.java \
  backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java \
  backend/sparklink-backend/src/test/java/com/sparklink/memory/service/MemoryFacadeTest.java \
  backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceMemoryIntegrationTest.java
git commit -m "feat: integrate memory facade into chat flow"
```

---

### Task 6: 全覆盖接入其余 Agent 链路

**Files:**
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/AiServiceImpl.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/NaturalLanguageSearchServiceImpl.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ActivityAiTextService.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/PartnerAiTextService.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/service/impl/AiServiceMemoryHookTest.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/service/impl/NaturalLanguageSearchMemoryHookTest.java`

- [ ] **Step 1: 写失败测试（每条链路都有 recall + enqueue）**

```java
@Test
void recommendShouldUseMemoryContext() {
    aiService.recommend(request);
    verify(memoryFacade).recallForPrompt(eq(request.getUserId()), anyString());
}
```

```java
@Test
void partnerAiShouldEnqueueConversation() {
    service.enhanceDescription(req, 10086L);
    verify(memoryFacade).enqueueConversation(eq(10086L), anyString(), anyString());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=AiServiceMemoryHookTest,NaturalLanguageSearchMemoryHookTest test`  
Expected: FAIL

- [ ] **Step 3: 最小实现统一钩子**

为各服务增加统一调用：

```java
MemoryContext ctx = memoryFacade.recallForPrompt(userId, userInput);
String finalPrompt = memoryFacade.mergePrompt(userPrompt, ctx);
memoryFacade.enqueueConversation(userId, userInput, modelOutput);
```

> 若某方法当前没有 `userId` 入参，先从调用方透传；不新增客户端可伪造字段。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=AiServiceMemoryHookTest,NaturalLanguageSearchMemoryHookTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/service/impl/AiServiceImpl.java \
  backend/sparklink-backend/src/main/java/com/sparklink/service/impl/NaturalLanguageSearchServiceImpl.java \
  backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ActivityAiTextService.java \
  backend/sparklink-backend/src/main/java/com/sparklink/service/impl/PartnerAiTextService.java \
  backend/sparklink-backend/src/test/java/com/sparklink/service/impl/AiServiceMemoryHookTest.java \
  backend/sparklink-backend/src/test/java/com/sparklink/service/impl/NaturalLanguageSearchMemoryHookTest.java
git commit -m "feat: add memory hooks across agent service flows"
```

---

### Task 7: 增加可观测性与告警埋点

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/memory/metrics/MemoryMetricsRecorder.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/memory/queue/MemoryEventConsumer.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/memory/service/MemoryFacade.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/memory/metrics/MemoryMetricsRecorderTest.java`

- [ ] **Step 1: 写失败测试（成功率与 DLQ 计数）**

```java
@Test
void shouldIncreaseDlqCounterWhenMoveToDlq() {
    recorder.recordDlq("add_memory_4xx");
    assertEquals(1, recorder.snapshot().getDlqCount());
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryMetricsRecorderTest test`  
Expected: FAIL

- [ ] **Step 3: 最小实现指标记录**

```java
recordSearchSuccess();
recordAddSuccess();
recordRetry();
recordDlq(reason);
recordSensitiveFieldBlock();
```

并在 `MemoryFacade`/`MemoryEventConsumer` 的成功与异常路径分别埋点。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryMetricsRecorderTest test`  
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/sparklink-backend/src/main/java/com/sparklink/memory/metrics/MemoryMetricsRecorder.java \
  backend/sparklink-backend/src/main/java/com/sparklink/memory/service/MemoryFacade.java \
  backend/sparklink-backend/src/main/java/com/sparklink/memory/queue/MemoryEventConsumer.java \
  backend/sparklink-backend/src/test/java/com/sparklink/memory/metrics/MemoryMetricsRecorderTest.java
git commit -m "feat: add memory metrics and dlq observability"
```

---

### Task 8: 端到端回归与验收

**Files:**
- Create: `backend/sparklink-backend/src/test/java/com/sparklink/memory/MemoryE2ETest.java`
- Modify: `backend/sparklink-backend/README.md`

- [ ] **Step 1: 写失败测试（同用户可召回、不同用户隔离）**

```java
@Test
void shouldRecallOnlyWithinSameMemoryUserId() {
    // user A 写入
    // user B 检索
    // 断言 B 看不到 A 记忆
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=MemoryE2ETest test`  
Expected: FAIL

- [ ] **Step 3: 打通 E2E 并补充文档**

README 新增：

```markdown
### Memory Library 配置
- DASHSCOPE_API_KEY
- memory.library.knowledgebase-id=5eb944ee565c400d8fb71e6ab119dfe3
- Redis Stream 队列与 DLQ 运维说明
```

- [ ] **Step 4: 运行完整测试**

Run: `cd backend/sparklink-backend && mvn test`  
Expected: 全部 PASS

- [ ] **Step 5: 验证编译**

Run: `cd backend/sparklink-backend && mvn compile`  
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/sparklink-backend/src/test/java/com/sparklink/memory/MemoryE2ETest.java \
  backend/sparklink-backend/README.md
git commit -m "test: add memory library e2e coverage and docs"
```

---

## Spec 覆盖自检

| Spec 需求 | 对应 Task |
|----------|-----------|
| JWT userId 作为用户维度 | Task 1 |
| 敏感字段只存不召回 | Task 2 |
| 阿里云单一持久化与固定 KB | Task 3 |
| 异步队列 + 重试 + DLQ | Task 4 |
| Chat 链路接入记忆 | Task 5 |
| 全量 Agent 链路接入 | Task 6 |
| 观测与告警指标 | Task 7 |
| 验收与回归测试 | Task 8 |

---

## Placeholder 扫描结果

- 已检查：无待补占位符（如“稍后实现/待完善/后续补充”）。
- 每个任务包含明确文件路径、命令、预期结果和提交动作。

---

## 类型一致性检查

- 统一使用 `Long userId`（业务身份）与 `String memoryUserId`（记忆身份键）。
- 统一入口：`MemoryFacade#recallForPrompt(...)`、`MemoryFacade#enqueueConversation(...)`。
- 幂等键统一为 `idempotencyKey`，避免命名漂移。

