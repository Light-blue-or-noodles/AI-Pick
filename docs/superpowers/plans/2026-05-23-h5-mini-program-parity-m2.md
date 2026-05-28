# H5 小程序对齐 — M2 实施计划（概要）

**Goal:** 消息/IM、个人中心、关注链、设置树与小程序对齐。

**参考:** `openspec/changes/h5-mini-program-parity/tasks.md` M2 段、`sparklink-mini-program/pages/profile|message|settings/*`

**关键 API:**
- `GET /api/user/following`、`GET /api/user/followers`
- `GET /api/user/follow/stats`、`GET /api/user/stats`
- `GET /api/partner/my`
- `POST/DELETE /api/user/{id}/follow`

**验收:** 详情联系→聊天发消息；Profile 统计跳转；设置子页可打开；`npm run build` 通过。
