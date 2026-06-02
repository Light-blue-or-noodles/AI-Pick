# Backend Dify Cloud Knowledge Retrieval Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `sparklink-backend` 中将知识库能力切换为 **Dify Cloud Knowledge API**，通过后端代理统一接入 `POST /datasets/{dataset_id}/retrieve`，确保前端无感、密钥安全、失败可降级。

**Architecture:** 业务请求进入 `ChatService` 后调用 `KnowledgeRetrieveGateway`；由 `DifyKnowledgeClient` 请求 Dify Cloud 检索分段，抽取白名单字段注入 Prompt；Dify 异常时自动 fallback 到无知识上下文回答，避免中断主链路。

**Tech Stack:** Spring Boot 3.2, Spring Web, Jackson, Resilience4j (or internal circuit-breaker), JUnit 5, Mockito

**设计依据:**
- `docs/superpowers/specs/2026-05-31-backend-agent-memory-library-design.md`
- Dify API: `POST /datasets/{dataset_id}/retrieve`

---

## 文件结构（实施前锁定）

| 路径 | 职责 |
|------|------|
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/config/DifyKnowledgeProperties.java` | Dify 配置（Base URL、API Key、Dataset ID、检索参数） |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/client/DifyKnowledgeClient.java` | Dify 检索 API 调用封装 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveRequest.java` | 内部检索请求模型 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveResult.java` | 内部检索结果模型 |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/KnowledgeRetrieveGateway.java` | 检索统一入口（可切换实现） |
| `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/PromptKnowledgeAssembler.java` | 检索结果注入 Prompt 的截断与格式化 |
| `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java` | 聊天链路接入知识检索 |
| `backend/sparklink-backend/src/main/resources/application.yml` | Dify 配置项 |
| `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/**` | 知识检索模块测试 |

---

### Task 1: 建立 Dify 配置与参数模型

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/config/DifyKnowledgeProperties.java`
- Modify: `backend/sparklink-backend/src/main/resources/application.yml`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/config/DifyKnowledgePropertiesTest.java`

- [ ] **Step 1: 写失败测试（配置默认值与必填项）**
- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=DifyKnowledgePropertiesTest test`  
Expected: FAIL（配置类不存在）

- [ ] **Step 3: 实现配置与默认值**

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

---

### Task 2: 封装 Dify 检索客户端（/datasets/{dataset_id}/retrieve）

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/client/DifyKnowledgeClient.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveRequest.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeRetrieveResult.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/client/DifyKnowledgeClientTest.java`

- [ ] **Step 1: 写失败测试（请求路径、鉴权头、核心参数）**
- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=DifyKnowledgeClientTest test`  
Expected: FAIL（客户端未实现）

- [ ] **Step 3: 实现调用与响应解析**

必须覆盖：
- 请求路径：`/datasets/{dataset_id}/retrieve`
- Header：`Authorization: Bearer {DIFY_API_KEY}`
- Body 至少包含：`query`, `retrieval_model.top_k`, `score_threshold_enabled`, `score_threshold`
- 响应提取：`records[].segment.content`, `records[].segment.id`, `records[].score`, `records[].segment.document_id`

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=DifyKnowledgeClientTest test`  
Expected: PASS

---

### Task 3: 建立网关与 Prompt 注入组装器

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/KnowledgeRetrieveGateway.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/PromptKnowledgeAssembler.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/service/PromptKnowledgeAssemblerTest.java`

- [ ] **Step 1: 写失败测试（按 score 排序、截断、去重）**
- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=PromptKnowledgeAssemblerTest test`  
Expected: FAIL

- [ ] **Step 3: 实现注入规则**

规则要求：
- 按 `score` 降序；
- 最大注入 `max-inject-records` 条；
- 每条 `content` 截断到 `max-inject-chars-per-record`；
- 按 `segment.id` 去重；
- 过滤空内容与异常分值。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=PromptKnowledgeAssemblerTest test`  
Expected: PASS

---

### Task 4: 接入 ChatService 主链路

**Files:**
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/service/impl/ChatServiceKnowledgeIntegrationTest.java`

- [ ] **Step 1: 写失败测试（先检索后生成，且保留原接口）**
- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatServiceKnowledgeIntegrationTest test`  
Expected: FAIL

- [ ] **Step 3: 最小实现接入**

```java
KnowledgeRetrieveResult result = knowledgeRetrieveGateway.retrieve(userId, request.getMessage());
String prompt = promptKnowledgeAssembler.merge(request.getMessage(), result);
String reply = generateReplyWithHistory(prompt, history, matchContext);
```

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=ChatServiceKnowledgeIntegrationTest test`  
Expected: PASS

---

### Task 5: 实现错误码映射、降级与熔断

**Files:**
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/service/KnowledgeRetrieveGateway.java`
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/model/KnowledgeFallbackReason.java`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/service/KnowledgeFallbackTest.java`

- [ ] **Step 1: 写失败测试（dataset_not_initialized / quota_exceeded / 500 降级）**
- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=KnowledgeFallbackTest test`  
Expected: FAIL

- [ ] **Step 3: 实现降级逻辑**

覆盖：
- `dataset_not_initialized`
- `provider_not_initialize`
- `provider_quota_exceeded`
- `403/404/500/timeout`

统一策略：记录原因 + 返回空检索上下文，不中断回答。

- [ ] **Step 4: 运行测试确认通过**

Run: `cd backend/sparklink-backend && mvn -Dtest=KnowledgeFallbackTest test`  
Expected: PASS

---

### Task 6: 观测埋点与 E2E 验收

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/knowledge/metrics/KnowledgeMetricsRecorder.java`
- Create: `backend/sparklink-backend/src/test/java/com/sparklink/knowledge/KnowledgeE2ETest.java`
- Modify: `backend/sparklink-backend/README.md`

- [ ] **Step 1: 写失败测试（成功率、空命中率、fallback 率）**
- [ ] **Step 2: 运行测试确认失败**

Run: `cd backend/sparklink-backend && mvn -Dtest=KnowledgeE2ETest test`  
Expected: FAIL

- [ ] **Step 3: 实现指标与文档补充**

README 新增：

```markdown
### Dify Cloud 知识库配置
- DIFY_API_KEY
- DIFY_DATASET_ID
- app.dify.retrieve.*
```

- [ ] **Step 4: 跑全量测试**

Run: `cd backend/sparklink-backend && mvn test`  
Expected: PASS

- [ ] **Step 5: 编译校验**

Run: `cd backend/sparklink-backend && mvn compile`  
Expected: BUILD SUCCESS

---

## Spec 覆盖自检

| Spec 需求 | 对应 Task |
|----------|-----------|
| 后端代理 Dify，前端不直连 | Task 1, Task 4 |
| 接入 `/datasets/{dataset_id}/retrieve` | Task 2 |
| 检索结果白名单注入 Prompt | Task 3 |
| 错误码处理 + 降级 + 熔断 | Task 5 |
| 观测告警与验收测试 | Task 6 |

---

## Placeholder 扫描结果

- 已检查：无“后续补充/TODO”占位描述。
- 每个任务包含明确文件路径、命令和预期结果。

---

## 类型一致性检查

- 统一使用 `Long userId` 作为业务身份。
- 统一使用 `KnowledgeRetrieveGateway#retrieve(...)` 作为检索入口。
- 统一使用 `KnowledgeRetrieveResult` 作为注入与观测的数据载体。

