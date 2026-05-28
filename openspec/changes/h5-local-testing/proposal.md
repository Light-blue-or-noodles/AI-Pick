## Why

`sparklink-h5` 已完成首版实现与生产构建，但在部署到阿里云前需要在本地验证核心用户路径（登录、搭子、IM、AI 聊天、资料编辑）是否与现有后端 API 正常联调。缺少系统化的本地测试方案会导致线上才发现代理、鉴权或 IM 凭证等问题。

## What Changes

- 建立本地 H5 开发与测试标准流程（`npm run dev` + Vite 代理 / 可选本地后端）
- 整理测试账号准备、环境变量与常见问题排查步骤
- 将 [`部署/H5-UAT-CHECKLIST.md`](../../../部署/H5-UAT-CHECKLIST.md) 中的功能项映射为可执行的本地验收步骤
- 记录本地测试通过标准，作为 ECS 部署前的门禁
- 可选：补充 `sparklink-h5` 开发文档中的「本地联调」章节

## Capabilities

### New Capabilities

- `h5-local-dev-test`: 在开发者本机启动 H5、对接 API、执行功能验收与问题记录的能力规范

### Modified Capabilities

（无：主规格库尚无既有 capability）

## Impact

- **代码**：`sparklink-h5/`（主要为文档与可选 `.env.local.example`）；一般不修改业务逻辑除非测试暴露缺陷
- **后端**：依赖 `https://www.aipick.cloud` 或本机 `localhost:8080` 的 `/api/**` 接口；需有效用户名密码账号
- **IM**：本地 H5 需能拉取 `/api/im/usersig` 并登录腾讯云 IM Web SDK
- **部署**：本地测试通过后才执行 `scripts/deploy-h5.sh`
