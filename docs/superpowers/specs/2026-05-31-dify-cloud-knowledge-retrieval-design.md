# 后端 Agent 接入 Dify Cloud 知识库 — 设计说明

| 项 | 内容 |
|----|------|
| 日期 | 2026-05-31 |
| 状态 | 待评审 |
| 范围 | 后端 Agent / Chat 链路接入 Dify Cloud 知识库检索 |
| 检索服务 | Dify Cloud Knowledge API |
| 核心接口 | `POST /datasets/{dataset_id}/retrieve` |
| 参考文档 | https://docs.dify.ai/api-reference/知识库/从知识库检索分段-测试检索 |

---

## 1. 目标与非目标

### 1.1 目标

- 采用 Dify Cloud 作为知识库中台，后端通过 API 统一代理检索能力。
- 前端（小程序/H5）不直连 Dify，避免 API Key 泄漏。
- 保持现有聊天接口不变（`/api/chat`），在服务端注入检索上下文。
- 支持可配置的 `top_k`、检索方法、阈值过滤和元数据过滤。
- 检索失败时主链路可降级，不阻断回答。

### 1.2 非目标

- 本阶段不自建向量数据库（Milvus/pgvector）。
- 本阶段不实现复杂多租户知识库自动路由（先固定单 dataset）。
- 本阶段不做文档解析管道自研（文档管理先依赖 Dify Cloud 控制台）。

---

## 2. 关键决策（已确认）

1. **架构边界**：Dify Cloud 负责知识库管理与检索；业务后端负责鉴权、编排、审计与降级。
2. **访问路径**：`Client -> Spring Boot -> Dify Cloud`，禁止前端持有 Dify API Key。
3. **接口选型**：统一使用 Dify 检索接口 `POST /datasets/{dataset_id}/retrieve`。
4. **检索策略**：MVP 默认 `hybrid_search + top_k=6 + score_threshold=0.55`。
5. **响应利用**：仅抽取 `records[].segment.content` 等白名单字段注入 Prompt。
6. **可迁移性**：通过 `KnowledgeRetrieveGateway` 抽象，后续可平滑替换为自建服务。

---

## 3. 总体架构

### 3.1 逻辑流程

1. 前端调用现有 `POST /api/chat`。
2. 后端完成 JWT 鉴权与用户身份解析。
3. `KnowledgeRetrieveGateway` 调用 Dify：
   - `POST https://api.dify.ai/v1/datasets/{dataset_id}/retrieve`
   - `Authorization: Bearer {DIFY_API_KEY}`
4. 后端对检索结果执行安全过滤、截断与格式化。
5. 将检索上下文拼接到 Chat Prompt，调用现有大模型链路生成回答。
6. 返回回答，同时记录检索耗时、命中数、降级标记。

### 3.2 架构图

```
MiniProgram/H5
   -> /api/chat
   -> ChatController
   -> ChatServiceImpl
      -> KnowledgeRetrieveGateway (DifyKnowledgeClient)
         -> Dify Cloud /datasets/{dataset_id}/retrieve
      -> Prompt Assembler (with retrieval context)
      -> LLM Client
```

---

## 4. Dify API 契约映射

### 4.1 请求映射

- Path：
  - `dataset_id`：固定来自服务端配置，不接受前端传入。
- Body（MVP）：
  - `query`：当前用户问题（最大 250 字符，超长后端截断）。
  - `retrieval_model.search_method`：`hybrid_search`
  - `retrieval_model.top_k`：`6`
  - `retrieval_model.score_threshold_enabled`：`true`
  - `retrieval_model.score_threshold`：`0.55`
  - `retrieval_model.reranking_enable`：`false`（MVP 默认关闭，后续可灰度）
  - `retrieval_model.metadata_filtering_conditions`：可选（按业务标签过滤）

### 4.2 响应映射

- 主用字段：
  - `records[].segment.content`
  - `records[].segment.document.name`
  - `records[].score`
  - `records[].segment.document_id`
  - `records[].segment.id`
- 注入 Prompt 前处理：
  - 按 `score` 降序；
  - 截断每段长度（如 300~500 字符）；
  - 最多注入 `N` 条（默认 4）；
  - 去重（基于 `segment.id`）。

---

## 5. 配置设计

```yaml
app:
  dify:
    base-url: https://api.dify.ai/v1
    api-key: ${DIFY_API_KEY}
    dataset-id: ${DIFY_DATASET_ID}
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

---

## 6. 安全与合规

- API Key 仅保存在服务端配置（环境变量/密钥管理），前端不可见。
- 后端统一校验用户登录态与业务权限，防止未授权检索。
- 检索上下文仅保留白名单字段，禁止传递敏感系统字段。
- 审计日志不落原始敏感文本，默认记录摘要与 ID。
- 对上传到 Dify 的文档执行脱敏策略（手机号、身份证等）。

---

## 7. 错误处理与降级

### 7.1 Dify 错误码策略

- `dataset_not_initialized`：返回“知识库初始化中”并走无知识兜底回答。
- `provider_not_initialize` / `provider_quota_exceeded`：标记配置错误告警，走兜底回答。
- `invalid_param`：记录参数错误并触发开发告警。
- `403/404/500`：统一按“检索失败可降级”处理。

### 7.2 降级原则

- 知识检索失败不阻塞主回答。
- 当连续失败率超过阈值，自动短路 Dify 调用一段时间（熔断）。
- 熔断期间定时半开探测恢复。

---

## 8. 观测与告警

### 8.1 核心指标

- `dify_retrieve_qps`
- `dify_retrieve_latency_p95`
- `dify_retrieve_success_rate`
- `dify_retrieve_empty_hit_rate`
- `dify_retrieve_fallback_rate`
- `dify_retrieve_circuit_open_count`

### 8.2 告警阈值（初始）

- `dify_retrieve_success_rate < 95%`（5 分钟窗口）
- `dify_retrieve_latency_p95 > 3000ms`（10 分钟窗口）
- `fallback_rate > 20%`（10 分钟窗口）

---

## 9. 验收标准

- 前端无改造或仅最小改造即可使用知识库增强回答。
- 后端成功通过 Dify 返回分段并参与回答生成。
- Dify 异常场景下主链路可用（有可解释兜底）。
- API Key 未出现在前端包体、日志明文或客户端存储中。
- 命中结果可追踪（可定位到 `document_id/segment_id`）。

---

## 10. 测试计划

### 10.1 单元测试

- Dify 请求体组装（参数默认值与覆盖）。
- 响应提取与截断、去重逻辑。
- 错误码映射与降级分支。

### 10.2 集成测试

- `/api/chat -> Dify 检索 -> 回答` 闭环验证。
- 空命中、低分命中、超时、403/404/500 场景。
- 元数据过滤条件生效验证。

### 10.3 回归测试

- 不启用知识库时行为与旧逻辑一致。
- 开启知识库后推荐卡片与主回答格式不回归。

---

## 11. 实施边界与演进

### 11.1 V1 实施边界

- 固定单 `dataset_id`。
- 仅接入 Dify 检索 API，不接入外部知识库 API。
- 文档导入/切分策略由 Dify 控制台维护。

### 11.2 后续演进

- 支持按业务域或租户路由多个 `dataset_id`。
- 启用 `reranking_enable` 与自定义权重策略。
- 引入 `external_retrieval_model`，接入自建检索服务。
- 抽象通用 `KnowledgeProvider`，支持 Dify/RagFlow 切换。

---

## 12. 风险与缓解

| 风险 | 缓解措施 |
|------|----------|
| Dify 免费额度不足导致检索失败 | 启用 BYOK + 熔断降级 + 配额监控 |
| 命中噪声影响回答质量 | 阈值过滤 + TopK 调优 + 注入长度控制 |
| 前端直连导致 Key 泄漏 | 强制后端代理 + 网关规则禁止直连 |
| 供应商锁定 | 网关抽象 + 请求/响应模型内部标准化 |

---

## 13. 实施前置条件

- Dify Cloud Workspace 已创建，知识库已导入文档并完成索引。
- 已获取可用 `DIFY_API_KEY` 与 `DIFY_DATASET_ID`。
- 后端配置中心/环境变量支持密钥注入。
- 已配置基础监控与告警通道。

---

*本设计已更新为 Dify Cloud 知识库方案，可直接用于下一步实施计划拆解。*

