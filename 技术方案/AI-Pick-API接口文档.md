# AI-Pick API 接口文档

> 版本：v1.0 | 日期：2026-03-05 | 状态：MVP 接口定义

---

## 一、通用说明

### 1.1 基础信息
- 基础URL：`http://localhost:8080/api`
- 认证方式：Bearer Token（JWT）
- 请求格式：JSON
- 响应格式：JSON

### 1.2 接口安全要求

⚠️ **除以下接口外，所有接口必须登录才能访问：**

| 接口 | 无需登录 |
|------|----------|
| POST /user/wechat-login | ✅ 微信一键登录 |
| GET /home/recommend | ⚠️ 仅展示，无敏感数据 |

**所有需要认证的接口，Header 必须携带：**
```
Authorization: Bearer {token}
```

**无 Token 访问返回：**
```json
{
  "code": 401,
  "message": "未登录或登录已过期"
}
```

### 1.3 数据安全防护

- **接口限流**：同一 IP/用户每分钟最多 60 次请求
- **敏感数据**：返回数据自动脱敏（手机号、身份证号等）
- **越权防护**：所有查询接口必须验证当前用户身份
- **数据隔离**：用户只能访问自己的数据

### 1.2 公共响应结构
```json
{
  "code": 0,
  "message": "success",
  "data": { }
}
```

### 1.3 错误码
| code | message | 说明 |
|------|---------|------|
| 0 | success | 成功 |
| 400 | 参数错误 | 请求参数有误 |
| 401 | 未登录 | 需要登录 |
| 403 | 无权限 | 权限不足 |
| 404 | 资源不存在 | 数据不存在 |
| 500 | 系统错误 | 服务器内部错误 |

---

## 二、用户模块

⚠️ **除以下接口外，本模块其他接口需要登录认证**

### 2.1 微信一键登录（无需登录）
```
POST /user/wechat-login
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | string | 是 | 小程序 wx.login() 返回的 code |
| userInfo | object | 否 | 用户基本信息（nickname/avatar/gender） |

**说明：**
- 使用微信小程序 wx.login() 获取 code
- 后端用 code 换取微信 openid
- 新用户自动创建账号

**响应：**
```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 123456,
    "nickname": "用户昵称",
    "avatar": "https://...",
    "isNew": false
  }
}
```

### 2.2 绑定微信手机号（需登录）
```
POST /user/bind-phone
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| encryptedData | string | 是 | 获取手机号返回的 encryptedData |
| iv | string | 是 | 获取手机号返回的 iv |
```

### 2.3 获取个人信息
```
GET /user/profile
```
**请求头：**
| 参数 | 说明 |
|------|------|
| Authorization | Bearer {token} |

**响应：**
```json
{
  "code": 0,
  "data": {
    "id": 123456,
    "nickname": "昵称",
    "avatar": "https://...",
    "gender": 1,
    "birthday": "1995-01-01",
    "bio": "个人简介",
    "interests": ["游戏", "运动"],
    "companyName": "字节跳动",
    "companyVerified": true,
    "schoolName": "清华大学",
    "schoolVerified": true,
    "createTime": "2026-01-01 00:00:00"
  }
}
```

### 2.4 更新个人信息
```
PUT /user/profile
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | string | 否 | 昵称 |
| avatar | string | 否 | 头像URL |
| gender | int | 否 | 性别（0-保密/1-男/2-女） |
| birthday | string | 否 | 生日 |
| bio | string | 否 | 个人简介 |
| interests | array | 否 | 兴趣标签 |

### 2.5 加入公司
```
POST /user/company
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| companyName | string | 是 | 公司名称 |
| inviteCode | string | 是 | 邀请码 |

### 2.6 加入学校
```
POST /user/school
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| schoolName | string | 是 | 学校名称 |
| inviteCode | string | 是 | 邀请码 |

---

## 三、搭子模块

⚠️ **本模块所有接口需要登录认证**

### 3.1 发布搭子
```
POST /partner
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 标题（最长20字） |
| description | string | 是 | 描述（最长200字） |
| type | string | 是 | 类型（游戏/运动/学习/美食/旅行/电影/音乐/其他） |
| tags | array | 否 | 兴趣标签 |
| maxParticipants | int | 是 | 最大人数（2-20） |
| scope | string | 是 | 范围（ALL/COLLEAGUE/ALUMNI/NEARBY） |
| latitude | number | 否 | 纬度 |
| longitude | number | 否 | 经度 |
| address | string | 否 | 地址 |

**响应：**
```json
{
  "code": 0,
  "data": {
    "id": 123456,
    "title": "寻找羽毛球搭子",
    "createTime": "2026-03-05 18:00:00"
  }
}
```

### 3.2 搭子列表
```
GET /partner
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 类型筛选 |
| scope | string | 否 | 范围筛选 |
| tags | string | 否 | 标签筛选（逗号分隔） |
| latitude | number | 否 | 纬度 |
| longitude | number | 否 | 经度 |
| distance | int | 否 | 距离（km） |
| page | int | 否 | 页码（默认1） |
| size | int | 否 | 每页数量（默认20） |

**响应：**
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "id": 123456,
        "userId": 123,
        "nickname": "昵称",
        "avatar": "https://...",
        "age": 25,
        "gender": 1,
        "title": "寻找羽毛球搭子",
        "description": "周末想打球",
        "type": "运动",
        "tags": ["羽毛球", "跑步"],
        "scope": "ALL",
        "maxParticipants": 4,
        "currentParticipants": 2,
        "matchScore": 92,
        "distance": 1.2,
        "createTime": "2026-03-05 18:00:00"
      }
    ],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

### 3.3 搭子详情
```
GET /partner/{id}
```
**响应：**
```json
{
  "code": 0,
  "data": {
    "id": 123456,
    "userId": 123,
    "nickname": "昵称",
    "avatar": "https://...",
    "age": 25,
    "gender": 1,
    "starRating": 4.9,
    "activityLevel": "HIGH",
    "bio": "个人简介",
    "tags": ["游戏", "运动"],
    "companyName": "字节跳动",
    "companyVerified": true,
    "schoolName": "清华大学",
    "schoolVerified": true,
    "title": "寻找羽毛球搭子",
    "description": "周末想打球",
    "type": "运动",
    "maxParticipants": 4,
    "currentParticipants": 2,
    "scope": "ALL",
    "latitude": 39.9,
    "longitude": 116.4,
    "address": "望京SOHO",
    "matchScore": 92,
    "isFollowed": false,
    "createTime": "2026-03-05 18:00:00"
  }
}
```

### 3.4 应征搭子
```
POST /partner/{id}/apply
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 是 | 应征消息（最长100字） |

### 3.5 搭子应征列表
```
GET /partner/{id}/applicants
```
**响应：**
```json
{
  "code": 0,
  "data": [
    {
      "id": 789,
      "userId": 456,
      "nickname": "应征者昵称",
      "avatar": "https://...",
      "message": "我可以一起",
      "matchScore": 88,
      "status": 0,
      "createTime": "2026-03-05 18:00:00"
    }
  ]
}
```

### 3.6 接受/拒绝应征
```
POST /partner/{partnerId}/accept
POST /partner/{partnerId}/reject
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| applicantId | int | 是 | 应征者ID |

### 3.7 我的搭子
```
GET /partner/my
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 类型（joined/created） |

---

## 四、活动模块

⚠️ **本模块所有接口需要登录认证**

### 4.1 发布活动
```
POST /activity
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 标题 |
| description | string | 是 | 描述 |
| type | string | 是 | 类型（线下/线上/组队/交友） |
| tags | array | 否 | 标签 |
| eventTime | string | 是 | 活动时间 |
| maxParticipants | int | 是 | 最大人数 |
| scope | string | 是 | 范围 |
| latitude | number | 否 | 纬度 |
| longitude | number | 否 | 经度 |
| address | string | 否 | 地址 |
| coverImage | string | 否 | 封面图URL |
| fee | number | 否 | 费用（0为免费） |

### 4.2 活动列表
```
GET /activity
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 类型筛选 |
| scope | string | 否 | 范围筛选 |
| fee | number | 否 | 费用筛选（0-免费） |
| latitude | number | 否 | 纬度 |
| longitude | number | 否 | 经度 |
| distance | int | 否 | 距离 |
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

### 4.3 活动详情
```
GET /activity/{id}
```
**响应：**
```json
{
  "code": 0,
  "data": {
    "id": 123456,
    "userId": 123,
    "organizer": {
      "id": 123,
      "nickname": "主办方昵称",
      "avatar": "https://...",
      "starRating": 4.9
    },
    "title": "周末桌游局",
    "description": "轻松愉快的桌游活动",
    "type": "线下",
    "tags": ["组队"],
    "coverImages": ["https://..."],
    "eventTime": "2026-03-08 14:00:00",
    "maxParticipants": 20,
    "currentParticipants": 12,
    "fee": 0,
    "scope": "ALL",
    "latitude": 39.9,
    "longitude": 116.4,
    "address": "XX桌游馆",
    "status": 0,
    "isJoined": false,
    "isFavorited": false,
    "participants": [
      { "id": 1, "avatar": "https://..." },
      { "id": 2, "avatar": "https://..." }
    ],
    "createTime": "2026-03-05 18:00:00"
  }
}
```

### 4.4 报名活动
```
POST /activity/{id}/join
```

### 4.5 取消报名
```
POST /activity/{id}/cancel
```

### 4.6 签到
```
POST /activity/{id}/checkin
```

### 4.7 我的活动
```
GET /activity/my
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 类型（joined/created） |

---

## 五、消息模块

⚠️ **本模块所有接口需要登录认证**

### 5.1 消息列表
```
GET /message/list
```
**响应：**
```json
{
  "code": 0,
  "data": {
    "list": [
      {
        "conversationId": 123456,
        "userId": 123,
        "nickname": "对方昵称",
        "avatar": "https://...",
        "lastMessage": "最后一条消息",
        "lastMessageTime": "2026-03-05 18:00:00",
        "unreadCount": 2
      }
    ]
  }
}
```

### 5.2 聊天详情
```
GET /message/conversation/{id}
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |

### 5.3 发送消息
```
POST /message/send
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| receiverId | int | 是 | 接收者ID |
| type | string | 是 | 类型（text/image/location） |
| content | string | 是 | 消息内容 |
| latitude | number | 否 | 纬度（位置类型） |
| longitude | number | 否 | 经度（位置类型） |

### 5.4 标记已读
```
POST /message/read
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| conversationId | int | 是 | 会话ID |

---

## 六、AI 模块

⚠️ **本模块所有接口需要登录认证**

### 6.1 AI 对话
```
POST /ai/chat
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | string | 是 | 用户消息 |
| context | object | 否 | 上下文（位置、筛选条件等） |

**响应：**
```json
{
  "code": 0,
  "data": {
    "reply": "为你找到了3个合适的搭子...",
    "type": "recommend",
    "data": {
      "partners": [...],
      "activities": [...]
    }
  }
}
```

### 6.2 AI 生成文案
```
POST /ai/generate-text
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 类型（partner/activity） |
| keywords | string | 是 | 关键词 |
| tone | string | 否 | 风格（正式/轻松/幽默） |

### 6.3 AI 智能推荐
```
POST /ai/recommend
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 类型（partner/activity） |
| latitude | number | 否 | 纬度 |
| longitude | number | 否 | 经度 |
| limit | int | 否 | 数量限制 |

### 6.4 计算匹配度
```
GET /ai/match-score
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | int | 是 | 对比用户ID |

---

## 七、通用模块

⚠️ **本模块所有接口需要登录认证**

### 7.1 首页推荐
```
GET /home/recommend
```
**响应：**
```json
{
  "code": 0,
  "data": {
    "partners": [...],
    "activities": [...]
  }
}
```

### 7.2 收藏/取消收藏
```
POST /favorite/{type}/{id}
```
**type**: partner / activity

### 7.3 关注/取消关注
```
POST /follow/{userId}
```

### 7.4 获取收藏列表
```
GET /favorite/list
```
**请求参数：**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | partner / activity |

---

*文档版本：v1.0*
*最后更新：2026-03-05*