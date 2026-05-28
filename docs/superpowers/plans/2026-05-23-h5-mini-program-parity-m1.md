# H5 小程序对齐 — M1 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 M1 里程碑——登录/协议、首页、搭子全链路（含筛选）、搭子发布增强、AI 聊天对齐、TabBar 视觉与交互，达到可与小程序对照验收的状态。

**Architecture:** 在 `sparklink-h5` 上按小程序页面逐页迁移；新增 `components`/`composables`/`utils/navigateToChat.js`；路由注册于 `src/router/index.js`；业务 API 继续走 `utils/request.js`。参考实现路径：`sparklink-mini-program/pages/*`。

**Tech Stack:** Vue 3, Vite 5, Vue Router 4, Pinia, Axios, Vant 4, tim-js-sdk（M1 仅 Tab 角标沿用现有逻辑，IM 深度对齐在 M2）

**设计依据:**
- [`docs/superpowers/specs/2026-05-23-h5-mini-program-parity-design.md`](../specs/2026-05-23-h5-mini-program-parity-design.md)
- [`openspec/changes/h5-mini-program-parity/proposal.md`](../../openspec/changes/h5-mini-program-parity/proposal.md)
- [`openspec/changes/h5-mini-program-parity/design.md`](../../openspec/changes/h5-mini-program-parity/design.md)

**M1 范围（含）:** login、agreement×2、home、partner、partner-detail、partner-publish、filter、ai-chat、AppTabBar  
**M1 范围（不含）:** message/chat 深度、profile 菜单全量、settings 树、关注/粉丝、入驻（M2/M3）

---

## 文件结构（M1 将创建/修改）

| 路径 | 职责 |
|------|------|
| `src/components/PageNavBar.vue` | 统一返回栏 |
| `src/components/NetworkImage.vue` | 封面/头像加载失败兜底 |
| `src/composables/usePartnerFilter.js` | `filter_partner` 读写 |
| `src/utils/navigateToChat.js` | 聊天/AI 跳转（M1 仅 AI 用） |
| `src/views/AgreementUserView.vue` | 用户协议 |
| `src/views/AgreementPrivacyView.vue` | 隐私政策 |
| `src/views/PartnerFilterView.vue` | 搭子筛选 |
| `src/styles/variables.css` | 扩充 rpx→px 令牌 |
| `src/components/AppTabBar.vue` | 对齐悬浮 TabBar |
| `src/views/HomeView.vue` | 对齐 index.wxml |
| `src/views/PartnerView.vue` | 筛选入口、scope tabs 样式 |
| `src/views/PartnerDetailView.vue` | 申请/联系等 |
| `src/views/PartnerPublishView.vue` | 完整发布表单 |
| `src/views/AiChatView.vue` | 推荐卡片、历史 |
| `src/views/LoginView.vue` | 协议链接 |
| `src/router/index.js` | 新路由 |

---

### Task 1: 设计令牌与全局样式

**Files:**
- Modify: `sparklink-h5/src/styles/variables.css`
- Modify: `sparklink-h5/src/styles/global.css`

- [ ] **Step 1: 扩充 CSS 变量**

在 `variables.css` 追加（与 `sparklink-mini-program/styles/variables.wxss` 对齐，rpx÷2 约等于 px）：

```css
:root {
  --primary-color: #5fb3a8;
  --primary-bg: #e8f6f5;
  --page-horizontal: 16px;
  --tab-bar-float-radius: 24px;
  --tab-bar-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
  --font-base: 14px;
  --font-lg: 16px;
}
```

- [ ] **Step 2: 页面底部留白**

在 `global.css` 的 `.page` 增加：

```css
.page--with-tab {
  padding-bottom: calc(88px + env(safe-area-inset-bottom, 0px));
}
```

`MainLayout` 子路由根节点加 class `page page--with-tab`。

- [ ] **Step 3: 验证构建**

Run: `cd sparklink-h5 && npm run build`  
Expected: exit 0

---

### Task 2: PageNavBar 与 NetworkImage 组件

**Files:**
- Create: `sparklink-h5/src/components/PageNavBar.vue`
- Create: `sparklink-h5/src/components/NetworkImage.vue`

- [ ] **Step 1: 创建 PageNavBar**

```vue
<template>
  <van-nav-bar :title="title" left-arrow @click-left="onBack" />
</template>
<script setup>
import { useRouter } from 'vue-router';
defineProps({ title: { type: String, default: '' } });
const router = useRouter();
function onBack() {
  if (window.history.length > 1) router.back();
  else router.push({ name: 'home' });
}
</script>
```

- [ ] **Step 2: 创建 NetworkImage**

参考 `sparklink-mini-program/components/network-image/network-image.js`：`src` 用 `resolveMediaUrl`；`@error` 时切 `defaultSrc`。

- [ ] **Step 3: 验证构建**

Run: `cd sparklink-h5 && npm run build`  
Expected: exit 0

---

### Task 3: 协议页与路由

**Files:**
- Create: `sparklink-h5/src/views/AgreementUserView.vue`
- Create: `sparklink-h5/src/views/AgreementPrivacyView.vue`
- Modify: `sparklink-h5/src/router/index.js`

- [ ] **Step 1: 复制协议正文**

从 `sparklink-mini-program/pages/agreement/user-agreement/user-agreement.wxml` 提取纯文本/HTML 段落，放入 `AgreementUserView.vue` 的 `<div class="agreement-body">`。隐私政策同理。

- [ ] **Step 2: 注册路由**

```javascript
{
  path: '/agreement/user',
  name: 'agreement-user',
  component: () => import('@/views/AgreementUserView.vue'),
  meta: { public: true, title: '用户协议' }
},
{
  path: '/agreement/privacy',
  name: 'agreement-privacy',
  component: () => import('@/views/AgreementPrivacyView.vue'),
  meta: { public: true, title: '隐私政策' }
}
```

- [ ] **Step 3: 手动验证**

Run: `npm run dev`，访问 `/agreement/user`、`/agreement/privacy`，确认可滚动阅读。

---

### Task 4: 登录页协议链接

**Files:**
- Modify: `sparklink-h5/src/views/LoginView.vue`

- [ ] **Step 1: 替换协议占位链接**

将「用户协议」「隐私政策」的 `href="#"` 改为：

```vue
<router-link :to="{ name: 'agreement-user' }">用户协议</router-link>
<router-link :to="{ name: 'agreement-privacy' }">隐私政策</router-link>
```

- [ ] **Step 2: 未勾选协议时禁止登录**

保持现有 `agreeProtocol` 校验；样式与小程序 login 页一致（白卡片表单）。

- [ ] **Step 3: 手动验证**

未勾选点击登录 → Toast；勾选后 dev test-login 或账号密码 → 跳转 `/home`。

---

### Task 5: TabBar 对齐小程序 custom-tab-bar

**Files:**
- Modify: `sparklink-h5/src/components/AppTabBar.vue`
- Modify: `sparklink-h5/src/layouts/MainLayout.vue`

- [ ] **Step 1: TabBar 视觉**

对齐 `custom-tab-bar/index.wxss`：
- 固定底栏：`left/right: 12px`，`border-radius: 24px`，`box-shadow: var(--tab-bar-shadow)`
- 激活项背景 `rgba(95, 179, 168, 0.12)`
- 中间发布钮：圆形渐变、上浮 `translateY(-20px)`（保持现有逻辑，微调尺寸）

- [ ] **Step 2: Tab 重复点击（partner / home）**

在 `AppTabBar.vue` 中，若 `route.name` 已是目标 tab，则 `router.replace` 同路由并 `emit('reselect')`；`PartnerView` 监听 refresh（`onActivated` 调 `loadPartners`）。

消息 Tab 的 reselect 刷新留 M2。

- [ ] **Step 3: MainLayout 加 class**

```vue
<router-view class="page page--with-tab" />
```

- [ ] **Step 4: 手动验证**

四 Tab 切换正常；发布钮跳转 `/partner-publish`；消息角标仍显示（已有 auth.unreadCount）。

---

### Task 6: 首页对齐 index

**Files:**
- Modify: `sparklink-h5/src/views/HomeView.vue`
- 资源：复制或引用 `sparklink-mini-program/images/home-ai-mascot.png` → `sparklink-h5/public/images/home-ai-mascot.png`

- [ ] **Step 1: 布局结构**

对齐 `index.wxml`：
- 吉祥物图 `home-ai-mascot.png`
- 标题「你好！我是 AI 助手」、副标题「告诉我你想找什么样的搭子」
- 三个 chip：游戏搭子、运动搭子、AI 对话（跳转 ai-chat 带 query `quick`）

- [ ] **Step 2: 样式**

参考 `index.wxss`：居中、chip 圆角边框、主色 chip。

- [ ] **Step 3: 手动验证**

点击 chip → `/ai-chat?quick=...` 自动发首条（已有 `AiChatView.tryEntryQuick`）。

---

### Task 7: 搭子筛选页 filter_partner

**Files:**
- Create: `sparklink-h5/src/composables/usePartnerFilter.js`
- Create: `sparklink-h5/src/views/PartnerFilterView.vue`
- Modify: `sparklink-h5/src/views/PartnerView.vue`
- Modify: `sparklink-h5/src/router/index.js`

- [ ] **Step 1: usePartnerFilter**

```javascript
const KEY = 'filter_partner';
export function loadPartnerFilter() {
  try {
    return JSON.parse(localStorage.getItem(KEY) || 'null') || defaultFilter();
  } catch {
    return defaultFilter();
  }
}
export function savePartnerFilter(filter) {
  localStorage.setItem(KEY, JSON.stringify(filter));
}
function defaultFilter() {
  return {
    distance: 'all',
    gender: 'all',
    partnerType: 'all',
    matchLevel: 'all',
    partnerStatus: 'all'
  };
}
```

- [ ] **Step 2: PartnerFilterView**

对齐 `filter.wxml`：距离/性别/类型/匹配度/状态选项；底部「重置」「确定」；确定时 `savePartnerFilter` + `router.back()`。

- [ ] **Step 3: PartnerView 增加筛选入口**

顶栏右侧增加「筛选」按钮 → `router.push({ name: 'partner-filter' })`。`onActivated` 中若存在 filter，对 `partners` 做客户端过滤（与小程序一致：M1 可先本地过滤已加载列表；若后端有 `/api/partner/filter` 则优先 API——读 `PartnerController` 确认）。

- [ ] **Step 4: 路由**

```javascript
{
  path: '/partner/filter',
  name: 'partner-filter',
  component: () => import('@/views/PartnerFilterView.vue'),
  meta: { title: '搭子筛选' }
}
```

- [ ] **Step 5: 手动验证**

设筛选 → 返回列表 → 列表条数/内容变化；重置生效。

---

### Task 8: 搭子列表与卡片样式

**Files:**
- Modify: `sparklink-h5/src/views/PartnerView.vue`
- Modify: `sparklink-h5/src/components/PartnerCard.vue`

- [ ] **Step 1: scope-tabs 样式**

对齐 `partner.wxss` 下划线 Tab（Pick搭/同事搭/校友搭）。

- [ ] **Step 2: PartnerCard 使用 NetworkImage**

封面用 `NetworkImage`；默认图 `/images/partner-banner.jpg`（放 `public/images/`）。

- [ ] **Step 3: 下拉刷新**

`van-pull-refresh` 已存在，确保 `@refresh` 调用 `loadPartners`。

- [ ] **Step 4: 手动验证**

三 Tab 切换请求 `scopeType` 正确；卡片点击进入详情。

---

### Task 9: 搭子详情增强

**Files:**
- Modify: `sparklink-h5/src/views/PartnerDetailView.vue`
- 参考: `sparklink-mini-program/pages/partner-detail/partner-detail.js`

- [ ] **Step 1: 展示字段**

确保展示：标题、类型、人数、描述、地址、发布者头像昵称、状态标签。

- [ ] **Step 2: 联系 TA**

保留 `POST /api/im/prep-peer` + 跳转 chat（M1 可跳转 chat 页占位，M2 完善 IM UI）；按钮文案与小程序一致。

- [ ] **Step 3: 申请加入（若小程序有）**

若 `partner-detail.js` 含 `POST /api/partner/{id}/apply`，在 H5 增加「申请」按钮与状态提示。

- [ ] **Step 4: 手动验证**

打开有效 id → 字段完整；联系/申请无 401。

---

### Task 10: 搭子发布完整表单

**Files:**
- Modify: `sparklink-h5/src/views/PartnerPublishView.vue`
- 参考: `sparklink-mini-program/pages/partner-publish/partner-publish.js`
- 参考: `sparklink-mini-program/utils/partnerPreferenceTags.js`（已存在于 H5）

- [ ] **Step 1: 搭子类型与偏好**

15 类类型 picker；偏好标签多选最多 5 个（`PREFERENCE_TAGS` / `MAX_SELECT`）。

- [ ] **Step 2: 可见范围**

`scopeSelected`: public / colleague / alumni，位值 1/2/4；根据 `GET /api/user/info` 的 `companyName`/`schoolName` 禁用不可用项；提交 `scopes: [1]` 等数组。

- [ ] **Step 3: 时间与人数**

`planDate`、`planTimeStr`、`memberCount`（映射 `targetCount`）；M1 日期可用 `van-date-picker`，时间可选。

- [ ] **Step 4: 地点**

M1 使用手动输入 `location`（地址文案）；经纬度可选留空（地图 SDK 属 M3）。

- [ ] **Step 5: 封面与提交 body**

上传 `/api/partner/upload-image` 字段 `image`；提交：

```javascript
await post('/api/partner', {
  title,
  type: typeIndex,
  content: description,
  preference: selectedPreferences.join(','),
  scopes: [1], // 按勾选合并位值
  targetCount: memberCount,
  location: locationDisplay,
  coverImage: coverImageUrl
});
```

- [ ] **Step 6: 手动验证**

发布成功 → 跳转 partner 列表可见新帖。

---

### Task 11: AI 聊天对齐

**Files:**
- Modify: `sparklink-h5/src/views/AiChatView.vue`
- 参考: `sparklink-mini-program/pages/ai-chat/ai-chat.wxml`

- [ ] **Step 1: 消息气泡布局**

用户消息右对齐、AI 回复左对齐；加载中 `van-loading`。

- [ ] **Step 2: 推荐搭子卡片**

`recommends` 列表展示头像+标题，点击 `router.push({ name: 'partner-detail', params: { id } })`（已有逻辑，对齐样式）。

- [ ] **Step 3: sessionId**

登录后请求带 token；`sessionId` 持久化 `localStorage.sessionId`。

- [ ] **Step 4: 手动验证**

发消息收回复；推荐卡片可点进详情。

---

### Task 12: M1 构建与冒烟

**Files:**
- Modify: `部署/H5-UAT-CHECKLIST.md`（可选：标注 M1 已通过项）

- [ ] **Step 1: 生产构建**

Run: `cd sparklink-h5 && npm run build`  
Expected: `dist/` 生成，无错误

- [ ] **Step 2: API 冒烟（本地后端）**

Run:

```bash
cd sparklink-h5
H5_USE_TEST_LOGIN=1 ./scripts/smoke-api.sh http://127.0.0.1:8080
```

Expected: health、login、user/info、partner、usersig 均 200

- [ ] **Step 3: M1 走查清单**

| 项 | 通过 |
|----|------|
| 协议页可打开 | ☐ |
| 登录+协议勾选 | ☐ |
| 首页 chip → AI | ☐ |
| 搭子三 Tab + 筛选 | ☐ |
| 详情 + 联系 | ☐ |
| 发布搭子 | ☐ |
| TabBar 样式与发布钮 | ☐ |

---

## Spec 覆盖自检（M1）

| 需求来源 | 对应 Task |
|----------|-----------|
| 协议页 | Task 3–4 |
| 首页 | Task 6 |
| 搭子列表/筛选/详情/发布 | Task 7–10 |
| AI 聊天 | Task 11 |
| TabBar | Task 5 |
| 设计令牌 | Task 1 |
| 构建/UAT | Task 12 |

**M2 留待下一计划：** message、chat、profile 菜单、settings 树等。

---

## 执行后：生成 OpenSpec tasks

M1 本计划完成后，由负责人或 AI 执行：

```text
根据 docs/superpowers/plans/2026-05-23-h5-mini-program-parity-m1.md 为 change h5-mini-program-parity 生成 specs 与 tasks.md（M1 段落与 Task 1–12 一一对应 checkbox）
```

再执行：

```text
/opsx:apply h5-mini-program-parity 只实现 M1
```
