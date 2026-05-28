# H5 本地测试结果

| 项 | 值 |
|----|-----|
| 日期 | 2026-05-23 |
| 测试人 | Agent（自动化 + 浏览器抽查） |
| API 环境 | 本地 `http://127.0.0.1:8080`（`mvn spring-boot:run`） |
| H5 | `npm run dev` → `http://localhost:5173`，`.env.development.local` 指向本地后端 |

## 自动化

| 检查 | 结果 |
|------|------|
| `npm install` | 通过 |
| `npm run build` | 通过（`dist/`） |
| `GET /api/health`（线上） | HTTP 200 |
| `GET /api/health`（本地） | HTTP 200 |
| `GET /api/health`（经 Vite 代理，默认线上配置） | HTTP 200 |
| `./scripts/smoke-api.sh http://127.0.0.1:8080` + test-login | 登录需 `test-login`；种子账号 `testuser/123456` 与库中哈希不一致 |
| `POST /api/chat`（test-login 后） | 通过，有 `reply` |

## 浏览器抽查（Chrome）

| 用例 | 结果 | 备注 |
|------|------|------|
| 登录页加载 | 通过 | |
| 开发测试账号登录 | 通过 | 跳转 `/home` |
| 搭子页 Tab | 通过 | `/partner` 三 Tab 可见 |
| AI 聊天页 | 部分 | 页面可开；发送按钮未在自动化中确认 UI 回显（API 正常） |
| 消息页 | 部分 | 页面可开；IM 会话列表需人工确认 |
| 个人中心 | 待人工 | 本次 snapshot 未抓到单元格文案 |

## 已知问题 / 待人工

1. **线上 API**：`testuser/123456` 登录失败；`test-login` 生产已关闭——需真实账号。
2. **本地种子密码**：`seed-data` 中 `123456` 与当前库 bcrypt 可能不一致，本地联调请用登录页 **「开发环境：测试账号登录」**。
3. **Safari 真机**（任务 4.2）：请人工抽查登录 + 搭子列表。
4. **IM Web**：本地需腾讯云 IM 配置；消息/聊天用例请登录后人工走一遍。
5. **401 跳转**：未在本轮浏览器中验证，可按 tasks 3.2 手动清 `localStorage.token` 后刷新。

## 结论

本地环境与核心链路（健康检查、test-login、首页/搭子/AI API）**可继续联调**；P0 全量 UI 验收建议测试人按 `tasks.md` 第 3–4 节补测后勾选 `部署/H5-UAT-CHECKLIST.md`。
