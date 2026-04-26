# AIPick AI 微信小程序

> AIPick AI 智能社交搭子平台 - 微信小程序 MVP

## 项目结构

```
wechat-mini-program/
├── app.js                 # 小程序逻辑
├── app.json               # 全局配置
├── app.wxss               # 全局样式
├── sitemap.json           # 网站地图配置
├── images/                # 图片资源
├── pages/                 # 页面目录
│   ├── index/            # 首页 - AI 对话入口 + 智能推荐
│   ├── partner/          # 搭子页面 - 搭子列表 + 发布搭子
│   ├── activity/         # 活动页面 - 活动列表 + 发布活动
│   ├── message/          # 消息页面 - 消息列表
│   ├── profile/          # 我的页面 - 个人中心
│   └── chat/             # AI 聊天页面
└── utils/                 # 工具函数
    ├── request.js        # 网络请求封装
    └── util.js           # 通用工具函数
```

## 功能特性

### 1. 首页
- AI 智能助手入口（突出显示）
- 智能推荐搭子列表
- 用户距离展示
- 个性化标签

### 2. 搭子模块
- 搭子列表展示
- 分类筛选（游戏、运动、美食、学习、旅游）
- 发布搭子功能
- 成员数量显示

### 3. 活动模块
- 活动列表展示
- 分类筛选（最新、热门、游戏、运动、美食、学习、社交）
- 发布活动功能
- 报名功能
- 参与人数进度条

### 4. 消息模块
- 聊天消息列表
- 系统通知
- 消息未读数
- 在线状态显示
- 长按删除

### 5. 个人中心
- 用户信息展示
- 统计数据（搭子数、活动数、消息数）
- 功能菜单入口
- 设置入口

### 6. AI 聊天
- 智能对话
- 快捷回复
- 消息时间显示

## API 配置

后端 API 地址配置在 `app.js` 中：

```javascript
globalData: {
  baseUrl: 'https://www.aipick.cloud'
}
```

## 启动预览

1. 下载并安装 [微信开发者工具](https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html)
2. 打开微信开发者工具
3. 选择导入项目：`/Users/yanleishi/AI/Project/OpenClaw/wechat-mini-program`
4. 点击预览按钮

## 注意事项

- 需要在微信公众平台注册小程序并获取 AppID
- TabBar 图标需要自行准备（images 目录下）
- 默认请求远程 API（`https://www.aipick.cloud`）；本地后端调试见 `局域网真机调试.md`（`app.js` 中 `USE_LAN`）

## 技术栈

- 原生微信小程序
- WXML + WXSS + JavaScript
- Flex 布局
- CSS3 动画

## 更新日志

### v1.0.0 (2024-03-05)
- MVP 版本发布
- 5 个核心页面开发完成
- 底部 TabBar 导航
- AI 对话功能
- 搭子和活动发布功能