## Context

- **变更**：`h5-mini-program-parity` — 在已有 `sparklink-h5`（Vue 3 + Vite + Vant）上对齐 `sparklink-mini-program`（24 页）。
- **当前 H5**：约 12 个视图已实现（登录、四 Tab、搭子/详情/发布、消息/聊天、AI、资料编辑、设置骨架）。
- **约束**：复用现有后端 `Authorization` + `X-User-Id`；IM 使用 `tim-js-sdk`；部署为 Nginx 静态 + `/api` 反代。
- **详细设计（权威）**：见仓库根目录 [`docs/superpowers/specs/2026-05-23-h5-mini-program-parity-design.md`](../../../docs/superpowers/specs/2026-05-23-h5-mini-program-parity-design.md)（路由对照表、目录结构、数据流、里程碑、UI 要点均以该文档为准）。

## Goals / Non-Goals

**Goals:**

- 功能与交互与小程序等价：Tab 结构、跳转关系、表单字段、API 与本地存储键名（如 `filter_partner`）一致。
- 在现有 `request.js`、`imService.js`、`partnerListMap.js` 等基础上扩展，避免重复造轮子。
- 可分里程碑上线：M1 核心 Tab + 搭子；M2 社交与设置；M3 入驻与微信 OAuth/地图。

**Non-Goals:**

- 不把小程序重写为 Taro/Uni-app。
- 不实现 `onShareAppMessage`、小程序版本更新管理器（H5 可省略或 Web Share 替代）。
- `tasks.md` 已按 M1 Superpowers plan 生成（2026-05-23）；M2/M3 为概要任务，实施前可再写 plan。

## Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 实现基线 | 扩展 `sparklink-h5` | 已有构建、部署与 IM/请求层 |
| 页面迁移 | 每小程序页对应一路由 + 一 View | 与 `app.json` 一一映射，便于走查 |
| 导航 | Vue Router；Tab + 中间发布钮 | 对齐 `custom-tab-bar` |
| 登录 | M1 账号密码 + 协议；M3 微信 OAuth | 生产不依赖 `test-login` |
| 地图 | 腾讯/高德 H5 SDK，降级手动地址 | 替代 `chooseLocation` |
| 实施顺序 | M1 → M2 → M3 | 见 superpowers 设计文档 §10 |

**未采用**：Taro 双端重写（成本高）；纯脚本 wx→web 转换（IM/TabBar 无法可靠映射）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 1:1 工期长 | 严格按 M1/M2/M3；每里程碑可独立验收 |
| IM Web 域名限制 | 腾讯云控制台配置 H5 域名 |
| 线上无种子账号 | 文档说明；本地 `allow-test-login` 仅 DEV |
| 设计双份维护 | OpenSpec 本文档写决策摘要；细节以 superpowers spec 为准 |

## Migration Plan

1. 按 `tasks.md` M1 段执行 → `npm run build` → 本地/UAT。
2. M2、M3 同理；全部通过后 `scripts/deploy-h5.sh` 部署 ECS。
3. 回滚：保留上一版 `dist` 备份；Nginx 切回旧静态目录。

## Open Questions

1. 微信开放平台网站应用与 OAuth 回调域名是否已就绪？（影响 M3）
2. 地图选点使用腾讯还是高德？Key 由谁提供？
3. 生产 H5 域名：`h5.aipick.cloud` 还是主站子路径？

---

**实施计划（plan）**：[`docs/superpowers/plans/2026-05-23-h5-mini-program-parity-m1.md`](../../../docs/superpowers/plans/2026-05-23-h5-mini-program-parity-m1.md)  
**任务清单**：[`tasks.md`](tasks.md)（M1 与 plan Task 1–12 对齐）
