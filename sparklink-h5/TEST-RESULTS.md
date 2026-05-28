# H5 自动化冒烟结果

| 项 | 值 |
|----|-----|
| 日期 | 2026-05-27 |
| 环境 | 本机自动化；API 默认走 dev 代理 → `https://www.aipick.cloud` |
| 执行人 | Cursor Agent |

## 自动化脚本

```bash
cd sparklink-h5
./scripts/smoke-all.sh              # build + preview 路由
./scripts/smoke-all.sh --api-prod   # 外加线上 API（需有效账号）
H5_USE_TEST_LOGIN=1 ./scripts/smoke-all.sh --api-local   # 需 MySQL + 本地后端
```

## 结果摘要

| 类别 | 结果 | 说明 |
|------|------|------|
| `npm run build` | ✅ 通过 | |
| UI 路由（preview :4173） | ✅ 8/8 | `/`、`/login`、协议、home、partner、filter、ai-chat |
| 线上 API health | ✅ HTTP 200 | |
| 线上 API 登录 | ❌ | `testuser/123456` 返回「用户名或密码错误」 |
| 本地 API | ⏭ 跳过 | MySQL 未启动（:3306 拒绝连接），后端无法拉起 |
| 浏览器（dev :5173） | ✅ 部分 | 登录页、协议页、首页 chip/TabBar 可访问 |

## 浏览器抽查（dev）

- `/login`：用户名/密码、协议链接、开发测试登录按钮可见
- `/agreement/user`：协议正文章节可见
- `/home`：「你好！我是 AI 助手」、游戏/运动/AI chip、底部 TabBar + 发布钮

## 未自动化（需账号或本地栈）

- [ ] 登录后搭子列表 / 详情 / 发布
- [ ] IM 消息与聊天发信
- [ ] AI 聊天收发
- [ ] 个人中心 / 设置 / 关注链
- [ ] 401 跳转登录
- [ ] Safari / 真机

## 建议下一步

1. 启动本机 MySQL 后：`H5_USE_TEST_LOGIN=1 ./scripts/smoke-api.sh http://127.0.0.1:8080`
2. 或提供生产可用账号：`H5_TEST_USER=... H5_TEST_PASS=... ./scripts/smoke-all.sh --api-prod`
3. 人工勾选 `部署/H5-UAT-CHECKLIST.md`
