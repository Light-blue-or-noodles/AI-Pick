# Spark Link H5

基于 Vue 3 + Vite + Vant 的 H5 前端，对接现有 Spark Link 后端 API，可部署至阿里云 ECS（Nginx 静态托管 + `/api` 反代）。

## 本地开发

```bash
cd sparklink-h5
npm install
npm run dev
```

开发服务器默认 `http://localhost:5173`。

### API 联调模式

| 模式 | 配置 | 说明 |
|------|------|------|
| 线上 API（默认） | 不建 `.env.development.local` | Vite 将 `/api` 代理到 `https://www.aipick.cloud` |
| 本地后端 | 复制 `.env.development.local.example` 为 `.env.development.local`，设置 `VITE_API_BASE_URL=http://127.0.0.1:8080` | 需本机 `mvn spring-boot:run` 且 `/api/health` 可用 |

### 本地测试（OpenSpec `h5-local-testing`）

```bash
# 1. API 冒烟（不启浏览器）
./scripts/smoke-api.sh                          # 线上
./scripts/smoke-api.sh http://127.0.0.1:8080   # 本地后端

# 2. 启动 H5
npm run dev
```

- 本地后端：登录页在开发模式下显示 **「开发环境：测试账号登录」**（需 `application.yml` 中 `allow-test-login: true`）。
- 线上 API：使用已注册用户名密码；生产环境 `test-login` 已关闭。
- 完整清单见 [`部署/H5-UAT-CHECKLIST.md`](../部署/H5-UAT-CHECKLIST.md) 与 `openspec/changes/h5-local-testing/TEST-RESULTS.md`。

## 生产构建

```bash
npm run build
```

产物在 `dist/`，上传至 ECS `/var/www/sparklink-h5` 或由 `scripts/deploy-h5.sh` 自动部署。

## 登录说明

H5 使用 **用户名 + 密码** 登录（`POST /api/user/login`）。需在后端已有注册用户；小程序微信登录仅在小程序端使用。

## 小程序 1:1 对齐（OpenSpec `h5-mini-program-parity`）

已实现 M1–M3 主要页面与路由，对照 `sparklink-mini-program`：

| 路由 | 说明 |
|------|------|
| `/home` | AI 首页 |
| `/partner`、`/partner/filter`、`/partner/:id`、`/partner-publish` | 搭子 |
| `/message`、`/chat`、`/ai-chat` | 消息与 AI |
| `/profile`、`/profile-edit`、`/settings/*`、`/about` | 个人与设置 |
| `/my/partners`、`/my/following`、`/my/followers` | 社交列表 |
| `/user/:id` | 他人主页 |
| `/join/company`、`/join/school` | 入驻 |
| `/agreement/user`、`/agreement/privacy` | 协议 |

可选环境变量：`VITE_WECHAT_APP_ID`（微信网站应用 OAuth）、`VITE_MAP_KEY`（地图选点，未配置则手动输入地址）。

实施计划：`docs/superpowers/plans/2026-05-23-h5-mini-program-parity-m*.md`

## 环境变量

| 文件 | 说明 |
|------|------|
| `.env.development` | 开发：`VITE_API_BASE_URL` 留空则走代理 |
| `.env.production` | 生产：默认 `https://www.aipick.cloud` |

## 部署

见仓库根目录：

- `scripts/deploy-h5.sh` — 本地构建并 rsync 到 ECS
- `部署/nginx/sparklink-h5.conf` — Nginx 静态站 + API 反代示例
- `部署/H5-UAT-CHECKLIST.md` — 上线验收清单
