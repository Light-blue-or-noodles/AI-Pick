## Why

微信小程序上线 AI 等功能受企业资质等限制，需要将 `sparklink-mini-program` 的能力迁移为可部署在阿里云的 H5 应用。现有 `sparklink-h5` 仅覆盖约一半页面与交互，无法作为小程序的完整替代入口。

## What Changes

- 在 `sparklink-h5/` 上扩展至与小程序 **24 个页面** 功能与交互尽量 1:1 对齐（Tab、路由、表单、API 调用一致）。
- 补齐缺失页面：协议页、搭子筛选、我的搭子/关注/粉丝、他人主页、设置子页、公司/学校入驻等。
- 增强已有页面：登录（协议、多种登录方式）、首页、搭子发布（偏好/可见范围/地图选点）、个人中心菜单与统计、TabBar 与 IM 角标。
- 微信专属能力用 H5 等价方案：OAuth/账号密码、文件上传、地图选点 SDK、`tim-js-sdk`。
- 分 **M1 / M2 / M3** 里程碑交付；`tasks.md` 与 `specs/**` 已生成（M1 对齐 Superpowers M1 plan）。

## Capabilities

### New Capabilities

- `h5-auth`: H5 登录、协议、会话与登出（含开发环境 test-login、生产账号密码，M3 可选微信 OAuth）
- `h5-partner`: 搭子列表（三 Tab）、筛选、详情、发布与完整表单
- `h5-social`: 消息列表、IM 单聊、关注/粉丝、他人主页、我的搭子
- `h5-profile-settings`: 个人中心、资料编辑、设置/账号安全/隐私/关于
- `h5-org-join`: 加入公司、加入学校（M3）
- `h5-ai-chat`: AI 助手对话与推荐卡片跳转

### Modified Capabilities

（`openspec/specs/` 尚无既有 capability，无修改项。）

## Impact

- **代码**：`sparklink-h5/`（views、components、services、router、stores、utils）
- **参考实现**：`sparklink-mini-program/`（页面与 `utils/` 映射逻辑）
- **后端**：现有 `sparklink-backend` REST 与 `/api/im/*`，无破坏性 API 变更预期
- **部署**：`scripts/deploy-h5.sh`、`部署/nginx/sparklink-h5.conf`、`部署/H5-UAT-CHECKLIST.md`
- **文档**：`docs/superpowers/specs/2026-05-23-h5-mini-program-parity-design.md`（详细设计源稿）
