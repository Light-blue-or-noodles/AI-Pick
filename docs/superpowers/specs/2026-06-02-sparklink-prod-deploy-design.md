# SparkLink 生产环境部署设计（前后端新功能）

> 日期：2026-06-02  
> 状态：待用户审阅  
> 目标：将近期新开发的前后端功能部署到现有生产 ECS（59.110.0.107 / aipick.cloud）

---

## 1. 背景与范围

### 1.1 部署目标

| 项 | 值 |
|----|-----|
| 环境 | 生产（直接覆盖线上） |
| 服务器 | 阿里云 ECS `59.110.0.107` |
| 域名 | `www.aipick.cloud`、`h5.aipick.cloud` |
| 分支 | `feature/pick-0.2.1`（含本地未提交 `LoginView.vue` 测试账号提示） |

### 1.2 本次上线功能

**后端（已 commit）**

- Dify Cloud 知识库检索（`KnowledgeRetrieveGateway`、`DifyKnowledgeClient`）
- 百炼 Web Search 自动触发（`BailianWebSearchClient`、`WebSearchPolicyService`）
- 记忆库集成与 AI 对话链路优化

**前端**

- H5 全量静态资源更新
- 登录页临时测试账号提示：`测试账号：test，密码：123456`

### 1.3 不在本次范围

- 数据库结构变更（新功能无 DDL 需求）
- 小程序发版
- CI/CD 流水线改造
- 新建统一部署脚本（沿用现有脚本，方案 A）

---

## 2. 方案选择

采用 **方案 A：本机脚本直推**。

理由：

1. 项目已有成熟脚本：`部署/deploy.sh`（后端）、`scripts/deploy-h5.sh`（H5）
2. 本地未提交改动需一并上线，本机 build 最直接
3. 比 ECS 上 git pull + Maven 构建更快、更可控

---

## 3. 架构与数据流（部署后）

```
用户浏览器 (H5)
    │ HTTPS
    ▼
Nginx (www.aipick.cloud)
    ├── /          → /var/www/sparklink-h5 (静态)
    └── /api/      → 127.0.0.1:8080 (Spring Boot)
                          ├── MySQL sparklink
                          ├── Redis
                          ├── Dify API (知识库)
                          ├── DashScope (对话 + Web Search + 记忆库)
                          └── 腾讯云 IM
```

---

## 4. 后端部署细节

### 4.1 部署路径

| 资源 | 路径 |
|------|------|
| JAR | `/opt/sparklink/app.jar` |
| JAR 备份 | `/opt/sparklink/app.jar.backup.YYYYMMDD_HHMMSS` |
| 环境变量 | `/opt/sparklink/.env`（或 systemd `EnvironmentFile`） |
| 日志 | `/var/log/sparklink.log` 或 `/var/log/sparklink/backend.log` |
| systemd 服务名 | `sparklink` |

### 4.2 环境变量（新增/确认）

写入服务器 `.env`，**禁止提交到 Git**：

```bash
# 新增 — Dify 知识库
DIFY_API_KEY=<用户提供的 dataset API key>
DIFY_DATASET_ID=eb75bc7c-51e9-490c-88ac-2e73ef6763bb
DIFY_BASE_URL=https://api.dify.ai/v1

# 确认/更新 — 百炼（对话 + Web Search + 记忆库共用）
DASHSCOPE_API_KEY=<用户提供的 sk-...>

# 建议显式开启
CHAT_TOOLS_ENABLED=true
CHAT_WEB_SEARCH_ENABLED=true
MEMORY_LIBRARY_ENVIRONMENT_TAG=prod
```

生产 profile（`application-prod.yml`）已设置：

- `app.allow-test-login: false`（后端测试登录接口关闭）
- `memory.library.environment-tag: prod`

其余已有变量（`DB_PASSWORD`、`JWT_SECRET`、`TENCENT_IM_*`、`WECHAT_SECRET`）保持不变，部署前 SSH 核对是否存在即可。

### 4.3 部署命令

```bash
# 1. 本地 commit（含 LoginView 提示，便于版本追溯）
git add sparklink-h5/src/views/LoginView.vue
git commit -m "feat(h5): add temporary test account hint on login page"

# 2. SSH 更新 .env（在服务器上编辑，追加 DIFY 等变量；参考 部署/sparklink.env.example）
ssh root@59.110.0.107
# vi /opt/sparklink/.env

# 3. 本机一键全量部署（后端 + H5 + Nginx + systemd sparklink）
cd /Users/yanleishi/AI/Project/OpenClaw
./scripts/deploy-prod.sh prod
# 或: ./deploy.sh prod

# 可选参数:
#   --backend-only   仅后端
#   --h5-only        仅 H5 + Nginx
#   --skip-nginx     跳过 Nginx 同步

# 4. 健康检查（脚本末尾也会执行）
curl -s http://59.110.0.107:8080/api/health
curl -s https://www.aipick.cloud/api/health
curl -sI https://www.aipick.cloud/
```

### 4.4 systemd 与 legacy 迁移

`scripts/deploy-prod.sh` 会：

1. 安装 `部署/sparklink.service` → `/etc/systemd/system/sparklink.service`
2. 使用 `EnvironmentFile=/opt/sparklink/.env`（首次可从 `/opt/aipick/.env` 自动迁移）
3. 停止并 disable legacy 服务 `aipick`，避免 8080 端口冲突
4. `systemctl restart sparklink`

部署前确认 `/opt/sparklink/.env` 已包含 DIFY 变量（参考 `部署/sparklink.env.example`）。

### 4.5 后端错误处理

| 场景 | 行为 | 应对 |
|------|------|------|
| DIFY_API_KEY 缺失 | 知识库检索跳过，聊天仍可用 | 检查 .env + 重启 |
| DASHSCOPE_API_KEY 缺失 | AI 对话失败；Web Search 跳过 | 检查 .env |
| Dify 超时（6s） | 降级为无知识库上下文 | 查 Dify 控制台与网络 |
| Web Search 超时（5s） | 降级为无联网引用 | 查百炼 MCP 配额 |
| jar 启动失败 | 健康检查不通过 | 还原 backup jar |

---

## 5. 前端部署细节

### 5.1 部署路径

| 资源 | 路径 |
|------|------|
| 静态文件 | `/var/www/sparklink-h5/` |
| Nginx 配置 | `/etc/nginx/conf.d/sparklink-h5.conf`（参考 `部署/nginx/sparklink-h5.conf`） |
| 生产 API 基址 | `sparklink-h5/.env.production` → `VITE_API_BASE_URL=https://www.aipick.cloud` |

### 5.2 部署命令

全量部署已包含 H5 环节（推荐）：

```bash
./scripts/deploy-prod.sh prod
```

仅 H5 时：

```bash
./scripts/deploy-h5.sh root@59.110.0.107 /var/www/sparklink-h5
```

脚本流程：`npm ci` → `npm run build` → `rsync -avz --delete dist/` 到服务器。

Nginx 由 `deploy-prod.sh` 同步 `部署/nginx/sparklink-h5.conf` → `/etc/nginx/conf.d/sparklink.conf`，并禁用 legacy `aipick.conf`。

### 5.3 前端注意事项

- 无需新增 `VITE_*` 环境变量
- 登录页测试账号提示会出现在**生产登录页**（用户已确认包含）
- 静态资源带 hash，rsync `--delete` 会清理旧 chunk；部署后建议硬刷新或无痕窗口验证

---

## 6. 验收清单（P0 冒烟）

参考 `部署/H5-UAT-CHECKLIST.md`，本次增量重点：

| # | 检查项 | 预期 |
|---|--------|------|
| 1 | `GET /api/health` | 返回 UP |
| 2 | H5 首页加载 | 200，无 Mixed Content |
| 3 | 登录页测试账号提示 | 可见「测试账号：test，密码：123456」 |
| 4 | 账号密码登录 | `test` / `123456` 登录成功 |
| 5 | AI 对话 | 发送消息收到回复 |
| 6 | 知识库检索 | 问产品/文档相关问题，回答含知识库信息或后端日志有 retrieve 成功 |
| 7 | Web Search | 问时效性问题（如「今天天气」），响应含搜索引用或 `searchMeta` |
| 8 | IM 发消息 | 搭子详情 → 联系 TA 可收发 |

---

## 7. 回滚方案

### 7.1 后端

```bash
ssh root@59.110.0.107
ls -lt /opt/sparklink/app.jar.backup.*
cp /opt/sparklink/app.jar.backup.<timestamp> /opt/sparklink/app.jar
systemctl restart sparklink
curl -s http://127.0.0.1:8080/api/health
```

### 7.2 前端

若无 dist 备份：本地 `git checkout HEAD~1 -- sparklink-h5`，重新 `deploy-h5.sh`。

### 7.3 环境变量

回滚 jar 不影响 .env；若新变量导致异常，从 `.env` 移除 DIFY 相关行并重启。

---

## 8. 安全要求

1. API Key 仅存在于服务器 `.env`，不得写入仓库、设计文档或 commit message
2. 生产 `allow-test-login=false`；测试账号提示仅为 UI 文案，需确保 `test` 账号为受控测试账号
3. 部署完成后不在聊天/日志中打印完整密钥
4. 用户在对话中提供的密钥，实施时通过 SSH 直接写入服务器，不本地持久化到文件

---

## 9. 风险与缓解

| 风险 | 缓解 |
|------|------|
| systemd 未加载 .env | 部署前 `systemctl cat sparklink` 验证 |
| 直接覆盖生产 | 部署前自动备份 jar；低峰期操作 |
| Dify/百炼配额或网络 | 冒烟测试 + 日志观察；失败可降级不影响主链路 |
| H5 缓存 | 无痕窗口 + `curl -I` 验证 index.html |

---

## 10. 实施顺序摘要

1. 本地 commit `LoginView.vue`
2. SSH 更新 `/opt/sparklink/.env`（DIFY + DASHSCOPE）
3. 验证 systemd `EnvironmentFile`
4. `./部署/deploy.sh prod`
5. `./scripts/deploy-h5.sh`
6. P0 冒烟验收
7. 失败则按第 7 节回滚

---

## 11. 后续可选优化（非本次）

- 统一 `deploy-all.sh` 串联 env + 后端 + H5 + 验收
- 将 DIFY/CHAT 变量补入 `application-prod.yml` 文档注释
- H5 dist 部署前自动备份上一版
- GitHub Actions 自动部署
