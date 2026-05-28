# Spark Link H5 上线验收清单

## 部署前（P0）

- [ ] `npm run build` 在 `sparklink-h5` 目录成功无报错
- [ ] ECS 已创建目录 `/var/www/sparklink-h5` 并执行 `scripts/deploy-h5.sh`
- [ ] Nginx 已加载 `部署/nginx/sparklink-h5.conf`（`try_files` + `/api/` 反代）
- [ ] `client_max_body_size` ≥ 6m（头像/搭子封面上传）
- [ ] HTTPS 443 可用（页面与 API 同为 HTTPS，避免 Mixed Content）
- [ ] 后端环境变量 `CORS_ALLOWED_ORIGINS` 包含 H5 域名（若前后端跨域）

## M1 功能（h5-mini-program-parity）

- [ ] 用户协议 / 隐私政策页可打开，登录页链接有效
- [ ] 首页 AI 吉祥物 + 快捷 chip → AI 对话
- [ ] 搭子列表筛选（`filter_partner`）往返生效
- [ ] TabBar 悬浮样式 + 发布钮 + Tab 重复点击刷新
- [ ] 发布搭子：类型、偏好、可见范围、时间、封面

## 功能验收（P0）

- [ ] 账号密码登录成功，刷新后登录态保持
- [ ] 401 / 用户不存在时自动跳转登录页
- [ ] 首页 → AI 聊天可发送并收到回复
- [ ] 搭子列表三 Tab（Pick / 同事 / 校友）有数据或空态正常
- [ ] 搭子详情 → 联系 TA 进入 IM 聊天并可发消息
- [ ] 发布搭子（标题、类型、描述、可选封面）成功
- [ ] 消息列表加载 IM 会话，未读角标更新
- [ ] 个人中心展示昵称头像，编辑资料保存成功
- [ ] 退出登录后 IM 登出、需重新登录

## M2/M3 功能（h5-mini-program-parity）

- [ ] 个人中心统计三列跳转（搭子/粉丝/关注）
- [ ] 我的公司 / 我的学校 加入成功
- [ ] 设置树：账号安全、隐私、关于、退出
- [ ] 他人主页关注/取关、发消息
- [ ] （可选）`VITE_WECHAT_APP_ID` 配置后微信登录

## 安全与运维（P1）

- [ ] 生产关闭 `test-login` / `allow-test-login`
- [ ] 不在前端硬编码 JWT Secret / IM 密钥
- [ ] 上传文件经 Nginx 与 Spring `multipart` 限制一致
- [ ] 建议：后端校验 `X-User-Id` 与 JWT subject 一致

## 灰度建议

1. 子域名 `h5.aipick.cloud` 先内测
2. 验证通过后再在主域名或对外宣传链接切换
