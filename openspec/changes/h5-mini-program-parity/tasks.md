# h5-mini-program-parity — Tasks

**实施计划（M1 详细步骤）**：[`docs/superpowers/plans/2026-05-23-h5-mini-program-parity-m1.md`](../../../docs/superpowers/plans/2026-05-23-h5-mini-program-parity-m1.md)

**设计源稿**：[`docs/superpowers/specs/2026-05-23-h5-mini-program-parity-design.md`](../../../docs/superpowers/specs/2026-05-23-h5-mini-program-parity-design.md)

**Apply 指令示例**：`/opsx:apply h5-mini-program-parity` 并注明「只实现 M1」；须同时引用本文件 M1 段与上述 plan。

---

## M1 — 核心 Tab 与搭子

> 与 Superpowers plan Task 1–12 一一对应。

### M1.1 设计令牌与全局样式（plan Task 1）

- [x] 1.1 扩充 `sparklink-h5/src/styles/variables.css`（主色、Tab 阴影、字号等）
- [x] 1.2 在 `global.css` 增加 `.page--with-tab` 底栏留白
- [x] 1.3 `MainLayout` 子 `router-view` 增加 `page page--with-tab` class
- [x] 1.4 验证：`cd sparklink-h5 && npm run build` 退出码 0

### M1.2 公共组件（plan Task 2）

- [x] 2.1 新建 `src/components/PageNavBar.vue`（van-nav-bar + 返回）
- [x] 2.2 新建 `src/components/NetworkImage.vue`（`resolveMediaUrl` + 默认图）
- [x] 2.3 验证：`npm run build` 通过

### M1.3 协议页与路由（plan Task 3）

- [x] 3.1 新建 `AgreementUserView.vue`（正文来自小程序 `user-agreement.wxml`）
- [x] 3.2 新建 `AgreementPrivacyView.vue`（正文来自 `privacy-policy.wxml`）
- [x] 3.3 在 `router/index.js` 注册 `/agreement/user`、`/agreement/privacy`（`meta.public: true`）
- [x] 3.4 手动验证：两页可滚动阅读

### M1.4 登录页协议链接（plan Task 4）

- [x] 4.1 `LoginView.vue` 协议链接改为 `router-link` 至协议页
- [x] 4.2 保持未勾选协议禁止登录
- [x] 4.3 手动验证：未勾选 Toast；勾选后登录进 `/home`

### M1.5 TabBar 对齐（plan Task 5）

- [x] 5.1 `AppTabBar.vue` 对齐 `custom-tab-bar/index.wxss`（圆角悬浮、激活态、发布钮）
- [x] 5.2 home/partner Tab 重复点击触发列表刷新（reselect）
- [x] 5.3 手动验证：四 Tab + 发布钮 + 消息角标

### M1.6 首页对齐（plan Task 6）

- [x] 6.1 复制 `home-ai-mascot.png` 至 `public/images/`
- [x] 6.2 `HomeView.vue` 对齐吉祥物、文案、三个 chip
- [x] 6.3 手动验证：chip → `/ai-chat?quick=...` 自动首条

### M1.7 搭子筛选（plan Task 7）

- [x] 7.1 新建 `composables/usePartnerFilter.js`（键名 `filter_partner`）
- [x] 7.2 新建 `PartnerFilterView.vue`（重置/确定）
- [x] 7.3 `PartnerView.vue` 增加筛选入口；`onActivated` 应用筛选
- [x] 7.4 注册路由 `/partner/filter`
- [x] 7.5 手动验证：筛选往返列表生效

### M1.8 搭子列表与卡片（plan Task 8）

- [x] 8.1 scope-tabs 样式对齐 `partner.wxss`
- [x] 8.2 `PartnerCard` 使用 `NetworkImage`；默认封面 `public/images/partner-banner.jpg`
- [x] 8.3 确认下拉刷新调用 `loadPartners`
- [x] 8.4 手动验证：三 Tab + 进详情

### M1.9 搭子详情（plan Task 9）

- [x] 9.1 补全展示字段（标题、类型、人数、描述、地址、发布者、状态）
- [x] 9.2 联系 TA：`prep-peer` + 跳转 chat
- [x] 9.3 若后端支持：申请加入按钮与状态
- [x] 9.4 手动验证：有效 id 页无 401

### M1.10 搭子发布完整表单（plan Task 10）

- [x] 10.1 15 类类型 + 偏好标签最多 5 个
- [x] 10.2 可见范围 public/colleague/alumni（位值 1/2/4，无组织则禁用）
- [x] 10.3 计划日期、时间、人数
- [x] 10.4 M1 手动输入地点（地图 M3）
- [x] 10.5 封面上传 + `POST /api/partner` body 对齐小程序
- [x] 10.6 手动验证：发布后在列表可见

### M1.11 AI 聊天对齐（plan Task 11）

- [x] 11.1 气泡左右对齐 + loading
- [x] 11.2 推荐搭子卡片样式与跳转详情
- [x] 11.3 `sessionId` localStorage 持久化
- [x] 11.4 手动验证：收发消息 + 点推荐进详情

### M1.12 M1 构建与验收（plan Task 12）

- [x] 12.1 `npm run build` 生产构建通过
- [x] 12.2 本地后端：`H5_USE_TEST_LOGIN=1 ./scripts/smoke-api.sh http://127.0.0.1:8080`
- [x] 12.3 M1 走查：协议、登录、首页 chip、搭子三 Tab+筛选、详情联系、发布、TabBar
- [x] 12.4（可选）更新 `部署/H5-UAT-CHECKLIST.md` M1 段落

---

## M2 — 社交与设置

> 范围见设计文档 §10 M2；实施前可另写 `docs/superpowers/plans/...-m2.md`。

### M2.1 IM 与消息

- [x] 2.1.1 `imService` 在 Tab 页稳定 init/login；失败可重试提示
- [x] 2.1.2 `MessageView` 会话列表、空态、下拉刷新
- [x] 2.1.3 消息 Tab reselect 刷新列表
- [x] 2.1.4 `ChatView` 历史加载、发文本、头部昵称头像
- [x] 2.1.5 新建 `utils/navigateToChat.js` 并在详情/消息统一使用
- [x] 2.1.6 手动验证：详情联系 → 聊天发一条；Tab 未读角标更新

### M2.2 个人中心

- [x] 2.2.1 `ProfileView` 渐变头、统计三列、菜单六项
- [x] 2.2.2 统计点击跳转 `/my/partners`、`/my/followers`、`/my/following`
- [x] 2.2.3 `ProfileEditView` 与后端字段对齐并验收保存

### M2.3 社交列表与他人主页

- [x] 2.3.1 新建 `MyPartnersView`、`MyFollowingView`、`MyFollowersView`
- [x] 2.3.2 新建 `UserProfileView`（`/user/:id`）关注/取关
- [x] 2.3.3 注册路由并在 profile 菜单链入

### M2.4 设置树

- [x] 2.4.1 `SettingsView` 菜单对齐小程序
- [x] 2.4.2 新建 `AccountSecurityView`、`PrivacySettingsView`、`AboutView`
- [x] 2.4.3 退出登录清 IM + 跳转 login
- [x] 2.4.4 M2 走查：消息/聊天/个人中心/设置/关注链路透测

### M2.5 M2 构建与验收

- [x] 2.5.1 `npm run build` 通过
- [x] 2.5.2 对照 `部署/H5-UAT-CHECKLIST.md` P0 中 IM、profile、settings 项

---

## M3 — 入驻与微信

> 依赖开放平台与地图 Key；见 design Open Questions。

### M3.1 组织入驻

- [x] 3.1.1 新建 `CompanyJoinView`（`/join/company`）
- [x] 3.1.2 新建 `SchoolJoinView`（`/join/school`）
- [x] 3.1.3 profile 或设置入口链入；提交后刷新 user info

### M3.2 微信 OAuth（可选）

- [x] 3.2.1 配置 `VITE_WECHAT_APP_ID` 等环境变量
- [x] 3.2.2 登录页微信入口 + `/login/wechat-callback` 处理 token
- [x] 3.2.3 未配置时隐藏或禁用 OAuth UI

### M3.3 地图选点

- [x] 3.3.1 接入腾讯或高德 H5 选点（Key 就绪后）
- [x] 3.3.2 `PartnerPublishView` 写入经纬度；保留手动地址降级

### M3.4 全量 UAT 与部署

- [x] 3.4.1 全量 `部署/H5-UAT-CHECKLIST.md` 签字
- [x] 3.4.2 `scripts/deploy-h5.sh` 部署 ECS + Nginx HTTPS
- [x] 3.4.3 TIM 控制台配置 H5 域名白名单
- [x] 3.4.4 生产账号联调（不依赖 test-login）
