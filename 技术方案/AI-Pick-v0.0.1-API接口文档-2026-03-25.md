# AI-Pick v0.0.1 API 接口文档

> 版本：v0.0.1 | 日期：2026-03-25 | 状态：已上线
> 基于代码反向生成

---

## 一、通用说明

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| 协议 | HTTPS |
| 域名 | www.aipick.cloud |
| 基础路径 | /api |
| 完整 URL | https://www.aipick.cloud/api |
| 请求格式 | JSON |
| 响应格式 | JSON |

### 1.2 认证方式

**Header 认证**：
```
X-User-Id: {用户ID}
```

**免登录接口**：
- `POST /user/wechat-login` - 微信登录
- `GET /home/recommend` - 首页推荐（仅展示）
- `GET /activity` - 活动列表
- `GET /partner` - 搭子列表

### 1.3 公共响应结构

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {},
  "timestamp": 1774414044371
}
```

**响应码说明**：

| code | 含义 |
|------|------|
| 0 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未登录或登录已过期 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 1.4 分页参数

| 参数 | 类型 | 说明 | 默认值 |
|------|------|------|--------|
| page | Integer | 页码 | 1 |
| size | Integer | 每页数量 | 10 |

**分页响应**：
```json
{
  "records": [],
  "total": 100,
  "size": 10,
  "current": 1,
  "pages": 10
}
```

---

## 二、用户模块

### 2.1 微信登录

**接口**：`POST /user/wechat-login`

**请求参数**：
```json
{
  "code": "微信授权码",
  "nickname": "用户昵称",
  "avatarUrl": "头像URL",
  "gender": 1
}
```

**响应**：
```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "JWT令牌",
    "userId": 12345,
    "isNewUser": true
  }
}
```

### 2.2 获取用户信息

**接口**：`GET /user/info`

**Header**：`X-User-Id: {用户ID}`

**响应**：
```json
{
  "code": 0,
  "data": {
    "id": 12345,
    "nickname": "张三",
    "avatar": "https://...",
    "gender": 1,
    "age": 25,
    "company": "阿里巴巴",
    "school": "浙江大学",
    "bio": "个人简介",
    "interests": ["篮球", "摄影"],
    "location": "杭州"
  }
}
```

### 2.3 更新用户信息

**接口**：`PUT /user/info`

**请求参数**：
```json
{
  "nickname": "新昵称",
  "avatar": "新头像URL",
  "gender": 1,
  "age": 26,
  "company": "新公司",
  "school": "新学校",
  "bio": "新简介",
  "interests": ["篮球", "摄影", "旅行"],
  "location": "北京"
}
```

---

## 三、首页模块

### 3.1 获取推荐内容

**接口**：`GET /home/recommend`

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| latitude | Double | 纬度（可选） |
| longitude | Double | 经度（可选） |

**响应**：
```json
{
  "code": 0,
  "data": {
    "partners": [
      {
        "id": 1,
        "title": "周末一起打篮球",
        "type": 1,
        "typeName": "运动",
        "location": "北京朝阳",
        "matchScore": 92,
        "user": {
          "nickname": "李四",
          "avatar": "https://...",
          "gender": 1
        }
      }
    ],
    "activities": [
      {
        "id": 1,
        "title": "羽毛球友谊赛",
        "coverImage": "https://...",
        "location": "北京朝阳体育馆",
        "startTime": "2026-03-30 14:00",
        "currentParticipants": 5,
        "maxParticipants": 10
      }
    ]
  }
}
```

---

## 四、搭子模块

### 4.1 发布搭子

**接口**：`POST /partner`

**请求参数**：
```json
{
  "type": 1,
  "title": "周末一起打篮球",
  "description": "想找个篮球搭子，周末一起打球",
  "location": "北京朝阳公园",
  "latitude": 39.9042,
  "longitude": 116.4074,
  "planTime": "2026-03-30T14:00:00",
  "scopeType": 1,
  "images": ["https://..."]
}
```

### 4.2 获取搭子列表

**接口**：`GET /partner`

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| page | Integer | 页码 |
| size | Integer | 每页数量 |
| type | Integer | 类型筛选（可选） |
| scopeType | String | 范围：platform/company/school |

**响应**：
```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 1,
        "title": "周末一起打篮球",
        "description": "想找个篮球搭子...",
        "type": 1,
        "typeName": "运动",
        "location": "北京朝阳",
        "latitude": 39.9042,
        "longitude": 116.4074,
        "planTime": "2026-03-30 14:00",
        "scopeType": 1,
        "scopeTypeName": "平台公开",
        "images": ["https://..."],
        "user": {
          "id": 123,
          "nickname": "李四",
          "avatar": "https://...",
          "gender": 1,
          "age": 25
        },
        "status": 1,
        "createTime": "2026-03-25 10:00"
      }
    ],
    "total": 100
  }
}
```

### 4.3 筛选搭子

**接口**：`GET /partner/filter`

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| page | Integer | 页码 |
| size | Integer | 每页数量 |
| type | Integer | 类型 |
| location | String | 位置关键词 |
| planTimeStart | DateTime | 计划时间开始 |
| planTimeEnd | DateTime | 计划时间结束 |
| gender | Integer | 发布者性别 |

### 4.4 获取搭子详情

**接口**：`GET /partner/{id}`

**响应**：
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "title": "周末一起打篮球",
    "description": "详细描述...",
    "type": 1,
    "location": "北京朝阳公园",
    "latitude": 39.9042,
    "longitude": 116.4074,
    "planTime": "2026-03-30 14:00",
    "images": ["https://..."],
    "user": {
      "id": 123,
      "nickname": "李四",
      "avatar": "https://...",
      "gender": 1,
      "age": 25,
      "company": "阿里巴巴",
      "school": "浙江大学"
    },
    "status": 1,
    "applicantCount": 3,
    "createTime": "2026-03-25 10:00"
  }
}
```

### 4.5 应征搭子

**接口**：`POST /partner/{id}/apply`

**请求参数**：
```json
{
  "message": "我也想打篮球，可以一起吗？"
}
```

### 4.6 获取我的搭子

**接口**：`GET /partner/my`

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| page | Integer | 页码 |
| size | Integer | 每页数量 |
| type | String | published/applied |

### 4.7 AI 润色描述

**接口**：`POST /partner/ai/description`

**请求参数**：
```json
{
  "title": "周末一起打篮球",
  "description": "想找个篮球搭子",
  "type": 1
}
```

**响应**：
```json
{
  "code": 0,
  "data": "🏀 周末篮球约起来！本人球龄5年，擅长投篮和防守，想找志同道合的球友一起切磋技艺。时间灵活，地点可商量，欢迎各路高手来战！"
}
```

---

## 五、活动模块

### 5.1 发布活动

**接口**：`POST /activity`

**请求参数**：
```json
{
  "title": "羽毛球友谊赛",
  "description": "欢迎羽毛球爱好者参加",
  "type": 1,
  "category": "运动",
  "location": "北京朝阳体育馆",
  "latitude": 39.9042,
  "longitude": 116.4074,
  "startTime": "2026-03-30T14:00:00",
  "endTime": "2026-03-30T17:00:00",
  "maxParticipants": 10,
  "coverImage": "https://...",
  "images": ["https://..."]
}
```

### 5.2 获取活动列表

**接口**：`GET /activity`

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| page | Integer | 页码 |
| size | Integer | 每页数量 |
| type | Integer | 类型筛选（可选） |
| category | String | 分类（可选） |
| keyword | String | 关键词搜索（可选） |

### 5.3 获取活动详情

**接口**：`GET /activity/{id}`

**响应**：
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "title": "羽毛球友谊赛",
    "description": "活动详情...",
    "type": 1,
    "category": "运动",
    "location": "北京朝阳体育馆",
    "latitude": 39.9042,
    "longitude": 116.4074,
    "startTime": "2026-03-30 14:00",
    "endTime": "2026-03-30 17:00",
    "maxParticipants": 10,
    "currentParticipants": 5,
    "coverImage": "https://...",
    "images": ["https://..."],
    "organizer": {
      "id": 123,
      "nickname": "李四",
      "avatar": "https://..."
    },
    "isRegistered": false,
    "status": 1,
    "createTime": "2026-03-25 10:00"
  }
}
```

### 5.4 报名活动

**接口**：`POST /activity/{id}/register`

### 5.5 取消报名

**接口**：`POST /activity/{id}/cancel`

### 5.6 上传活动图片

**接口**：`POST /activity/upload-image`

**请求**：multipart/form-data
| 字段 | 说明 |
|------|------|
| file | 图片文件（JPG/PNG/GIF/WEBP，最大5MB） |

**响应**：
```json
{
  "code": 0,
  "data": {
    "url": "https://www.aipick.cloud/api/static/covers/xxx.jpg"
  }
}
```

### 5.7 获取活动日历

**接口**：`GET /activity/calendar`

**参数**：
| 参数 | 类型 | 说明 |
|------|------|------|
| year | Integer | 年份 |
| month | Integer | 月份 |

**响应**：
```json