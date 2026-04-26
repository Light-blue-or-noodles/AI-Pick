# Aipick 后端服务

> Aipick AI 社交小程序 Spring Boot 3.2 MVP 后端

## 技术栈

- **框架**: Spring Boot 3.2
- **持久层**: MyBatis-Plus 3.5
- **数据库**: MySQL 8.0
- **缓存**: Redis 7-alpine
- **认证**: JWT
- **连接池**: Druid

## 项目结构

```
aipick-backend/
├── src/main/java/com/aipick/
│   ├── config/         # 配置类
│   ├── controller/     # 控制器
│   ├── service/       # 服务层
│   ├── mapper/        # 数据访问层
│   ├── entity/        # 实体类
│   ├── dto/           # 数据传输对象
│   └── common/        # 公共组件
├── src/main/resources/
│   ├── application.yml  # 应用配置
│   └── init.sql         # 数据库初始化脚本
└── pom.xml
```

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0
- Redis 7+

### 2. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/init.sql
```

### 3. 配置修改

修改 `application.yml` 中的数据库和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aipick
    username: root
    password: aipick123
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 启动项目

```bash
# 打包
mvn clean package

# 运行
mvn spring-boot:run

# 或直接运行 jar
java -jar target/aipick-backend-1.0.0.jar
```

服务启动后访问: http://localhost:8080/api

## API 接口

### 用户模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/user/register | 用户注册 | 否 |
| POST | /api/user/login | 用户登录 | 否 |
| GET | /api/user/info | 获取用户信息 | 是 |
| PUT | /api/user/info | 更新用户信息 | 是 |

### 搭子模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/partner | 发布搭子 | 是 |
| GET | /api/partner/list | 搭子列表 | 否 |
| GET | /api/partner/{id} | 搭子详情 | 否 |
| POST | /api/partner/{id}/apply | 应征搭子 | 是 |
| GET | /api/partner/{id}/applies | 应征列表 | 是 |

### 活动模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/activity | 发布活动 | 是 |
| GET | /api/activity/list | 活动列表 | 否 |
| GET | /api/activity/{id} | 活动详情 | 否 |
| POST | /api/activity/{id}/register | 报名活动 | 是 |
| DELETE | /api/activity/{id}/register | 取消报名 | 是 |
| GET | /api/activity/{id}/registrations | 报名列表 | 是 |

### AI 对话模块

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/chat | AI 对话 | 是 |
| GET | /api/chat/history/{sessionId} | 会话历史 | 是 |

## 请求头说明

需要认证的接口需要在请求头中添加:

```
Authorization: Bearer <token>
X-User-Id: <用户ID>
```

## 响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1706832000000
}
```

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器错误 |

## 开发规范

遵循阿里巴巴 Java 开发规约:
- 命名风格: 驼峰命名
- 类注释: 使用 `@author` 标注作者
- 方法注释: 使用 Javadoc 风格
- 常量定义: 使用枚举或常量类

## License

MIT