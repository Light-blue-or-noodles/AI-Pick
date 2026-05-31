# /api/chat Tools 化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `POST /api/chat` 升级为 Spring AI 受控 Function Calling，保留规则 fallback，并完成 `/api/ai/chat` 一版兼容废弃。

**Architecture:** `ChatIntentToolGate` 判定意图与白名单 → `ChatToolOrchestrator` 通过 `ChatClient` 调用 `ChatTools` → 工具结果写入 `ChatToolExecutionContext` 并生成 `recommends`；失败时 `ChatRecommendFallbackService` + `DashScopeCompatClient`。

**Tech Stack:** Spring Boot 3.2、Spring AI Alibaba DashScope、MyBatis-Plus、小程序/H5

**Spec:** [2026-05-31-api-chat-tools-design.md](../specs/2026-05-31-api-chat-tools-design.md)

---

### Task 1: Chat 工具基础设施

**Files:**
- Create: `backend/sparklink-backend/src/main/java/com/sparklink/ai/chat/*`
- Test: `backend/sparklink-backend/src/test/java/com/sparklink/ai/chat/ChatIntentToolGateTest.java`

- [x] 新增 `ChatIntent`、`ChatIntentToolGate`、`ChatTools`、`ChatToolExecutionContext`
- [x] 新增 `ChatRecommendAssembler`、`ChatRecommendFallbackService`
- [x] 新增 `ChatToolOrchestrator`、`ChatAiConfig`
- [x] `ChatIntentToolGateTest` 通过

### Task 2: ChatServiceImpl 接入与 fallback

**Files:**
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/ChatServiceImpl.java`

- [x] 优先 `ChatToolOrchestrator`，失败回落规则召回 + DashScope
- [x] 工具无 recommends 时补跑 fallback 召回

### Task 3: 旧接口兼容废弃

**Files:**
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/controller/AiController.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/AiService.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/service/impl/AiServiceImpl.java`

- [x] `/ai/chat` 转发 `ChatService`，响应头 `Deprecation: true`
- [x] 移除 `AiService#chat` 规则实现

### Task 4: 前端迁移

**Files:**
- Modify: `sparklink-mini-program/pages/chat/chat.js`
- Modify: `sparklink-mini-program/pages/ai-chat/ai-chat.js`
- Modify: `sparklink-h5/src/views/AiChatView.vue`

- [x] 小程序 IM 内 AI 聊天改调 `/api/chat`
- [x] 推荐卡片按 `type` 跳转（activity 模块未上线时 toast 提示）
- [x] H5 推荐卡片 `:key` 与跳转逻辑对齐

### Task 5: 配置、文档与测试

**Files:**
- Modify: `backend/sparklink-backend/src/main/resources/application.yml`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/common/AiConstants.java`
- Modify: `backend/sparklink-backend/src/main/java/com/sparklink/controller/ChatController.java`
- Test: `ChatRecommendFallbackServiceTest.java`
- Modify: `技术方案/AI-Pick-API接口文档.md`

- [x] `app.chat.tools-timeout-ms` 配置与 orchestrator 超时
- [x] `X-User-Id` 可选（未登录仍可对话，工具无用户画像）
- [x] fallback 单测与 API 文档更新

### Task 6: 下一版本（未做，留待后续 PR）

- [ ] 删除 `AiController#chat`
- [ ] 历史消息持久化 `recommends` 元数据
