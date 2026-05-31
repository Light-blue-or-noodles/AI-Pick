# 2026-05-31 /api/chat Tools 化设计

## 目标

- `POST /api/chat` 采用 Spring AI Function Calling + 受控工具白名单（A 方案）
- 工具失败或未命中意图时，回落现有关键词规则召回 + DashScope 兼容接口
- `POST /api/ai/chat` 兼容 1 个版本：转发至 `ChatService`，响应头 `Deprecation: true`

## 架构

```
Client -> ChatController -> ChatServiceImpl
  -> ChatToolOrchestrator (intent gate + ChatClient tools)
  -> ChatTools (searchPartners / searchActivities / getUserContext)
  -> fallback: ChatRecommendFallbackService + DashScopeCompatClient
```

## 工具白名单

| 意图 | 允许工具 |
|------|----------|
| PARTNER | searchPartners, getUserContext |
| ACTIVITY | searchActivities, getUserContext |
| MIXED | searchPartners, searchActivities, getUserContext |
| PROFILE | getUserContext |
| GENERAL | 无（走 fallback 话术） |

## 安全约束

- 只读工具，分页上限 6
- 工具参数白名单（type/category/keyword/limit）
- 审计日志：`[ChatTool]` / `[ChatOrchestrator]`
- toolContext 注入 userId，不暴露敏感字段

## 配置

```yaml
app:
  chat:
    tools-enabled: true      # 关闭则始终走 fallback
    tools-timeout-ms: 25000  # 工具编排超时
```

## 前端

- 小程序 `pages/chat/chat.js`、`pages/ai-chat` 均调用 `/api/chat`
- H5 `AiChatView.vue` 调用 `/api/chat`
- 推荐卡片：`type=partner` 跳搭子详情；`type=activity` 活动模块未上线时 toast 提示

## 实现状态（2026-05-31）

- [x] 后端 tools 编排 + fallback
- [x] `/api/ai/chat` 兼容转发
- [x] 前端迁移与推荐跳转
- [x] 单测：`ChatIntentToolGateTest`、`ChatRecommendFallbackServiceTest`
- [x] 实现计划：`docs/superpowers/plans/2026-05-31-api-chat-tools.md`

## 下一版本

- 删除 `AiController#chat` 兼容端点
- 可选：对话历史持久化 recommends 元数据
