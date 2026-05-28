# H5 小程序对齐 — M3 实施计划（概要）

**Goal:** 公司/学校入驻、可选微信 OAuth、地图选点降级、全量 UAT。

**API:** `POST /api/user/company`、`POST /api/user/school`

**微信:** 需开放平台网站应用；未配置时隐藏入口。`VITE_WECHAT_APP_ID` 可选。

**地图:** `VITE_MAP_PROVIDER` + `VITE_MAP_KEY`；未配置则保留手动地址。

**部署:** `scripts/deploy-h5.sh`、`部署/H5-UAT-CHECKLIST.md`、TIM H5 域名白名单。
