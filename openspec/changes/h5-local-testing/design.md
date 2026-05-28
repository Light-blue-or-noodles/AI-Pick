## Context

- H5 工程路径：`sparklink-h5/`，Vue 3 + Vite 5 + Vant
- 开发默认：`npm run dev` → `http://localhost:5173`，`vite.config.js` 将 `/api` 代理到 `https://www.aipick.cloud`
- 登录方式：用户名 + 密码（`POST /api/user/login`），与小程序微信登录分离
- 已有上线清单：`部署/H5-UAT-CHECKLIST.md`

## Goals / Non-Goals

**Goals:**

- 开发者可在 15 分钟内完成环境启动并完成 P0 功能冒烟
- 明确「远程 API」与「本地后端」两种联调模式及切换方式
- 测试失败时有可复现的排查路径（网络、401、IM、CORS、上传）

**Non-Goals:**

- 不引入自动化 E2E 框架（Playwright/Cypress）作为本变更硬性要求
- 不在本变更中完成 ECS/Nginx 部署
- 不改造后端鉴权模型（如 JWT 与 `X-User-Id` 绑定）——仅记录为已知风险

## Decisions

| 决策 | 选择 | 理由 |
|------|------|------|
| 默认 API 目标 | Vite 代理 → 线上 `www.aipick.cloud` | 无需本机起 Java/MySQL，最快验证 H5 逻辑 |
| 可选 API 目标 | `.env.development.local` 设置 `VITE_API_BASE_URL=http://127.0.0.1:8080` | 联调本地后端时使用；需关闭代理或直连 |
| 测试账号 | 使用已注册用户名密码；禁止依赖生产 `test-login` | 与生产安全策略一致 |
| 浏览器 | Chrome / Safari 移动端模拟（375px 宽） | 贴近 H5 真实场景 |
| 通过标准 | P0 清单全部勾选 | 与 UAT 清单对齐，减少重复劳动 |

**备选方案（未采用）：**

- 仅 `npm run preview` 测构建产物：无法验证 dev 代理，故作为补充而非主路径
- Mock API：不利于发现真实 IM/上传问题

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 代理到线上 API，数据为生产/预发真实数据 | 使用专用测试账号；勿在测试中发布违规内容 |
| IM Web SDK 在 localhost 受限 | 确认腾讯云 IM 控制台 Web 端域名白名单含 `localhost`（若启用） |
| 本地后端未启动却改 env 指向 127.0.0.1 | 文档中强调健康检查 `curl http://127.0.0.1:8080/api/health` |
| `tim-js` 包体积大导致 dev 冷启动慢 | 接受；生产 build 已验证通过 |

## Migration Plan

本变更为流程/文档类，无数据迁移。实施顺序：

1. 补充本地测试文档与 env 示例
2. 执行 tasks.md 中的验收步骤并记录结果
3. 通过后进入 `deploy-h5.sh` 部署阶段

## Open Questions

- 是否已有专用 H5 测试账号（用户名/密码）需写入团队私密文档而非仓库？
- 线上 IM 是否已配置 Web 端安全域名（影响本地消息页）？
