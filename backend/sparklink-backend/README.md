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

### AI Chat 联网搜索配置

`application.yml` 中可配置以下参数：

```yaml
app:
  chat:
    web-search:
      enabled: true
      endpoint: https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp
      timeout-ms: 5000
      max-results: 5
      domain-whitelist: []
      domain-blacklist: []
```

- `enabled`: 是否启用联网搜索总开关（`true/false`）。
- `endpoint`: 百炼 WebSearch MCP 连接地址（默认 `https://dashscope.aliyuncs.com/api/v1/mcps/WebSearch/mcp`）。
- `timeout-ms`: 联网搜索超时时间（毫秒）。
- `max-results`: 每次联网搜索最多返回结果数。
- `domain-whitelist`: 域名白名单，非空时仅保留白名单域名结果。
- `domain-blacklist`: 域名黑名单，命中黑名单域名的结果会被过滤。
- `ChatRequest.webSearchMode`: 每次请求可选 `auto/on/off`。
  - `auto`: 按策略自动判定是否触发联网搜索。
  - `on`: 强制触发联网搜索。
  - `off`: 强制关闭联网搜索。

### AI Chat Dify Cloud 知识检索配置

知识检索依赖 Dify Cloud Dataset API，建议通过环境变量注入敏感信息：

```bash
export DIFY_API_KEY=你的DifyApiKey
export DIFY_DATASET_ID=你的DifyDatasetId
```

`application.yml` 示例：

```yaml
app:
  dify:
    enabled: true
    base-url: https://api.dify.ai/v1
    api-key: ${DIFY_API_KEY:}
    dataset-id: ${DIFY_DATASET_ID:}
    timeout-ms: 6000
    retrieve:
      top-k: 6
      threshold-enabled: true
      score-threshold: 0.55
      search-method: hybrid_search
      reranking-enable: false
      max-inject-records: 4
      max-inject-chars-per-record: 400
```

参数说明：
- `DIFY_API_KEY`：Dify Cloud API Key，未配置时将触发检索降级。
- `DIFY_DATASET_ID`：知识库 Dataset ID，需与 Dify 后台一致。
- `app.dify.retrieve.*`：检索策略参数（TopK、阈值过滤、检索方法、重排、注入上限）；
- 生产环境请通过环境变量/配置中心注入密钥，不要将真实密钥提交到仓库。

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

## Memory Library 配置与运维

### 1) 必填配置

- DashScope API Key（建议通过环境变量注入）：

```bash
export DASHSCOPE_API_KEY=你的密钥
```

- 记忆库 ID（`application.yml`）：

```yaml
memory:
  library:
    enabled: true
    knowledgebase-id: ${MEMORY_LIBRARY_KNOWLEDGEBASE_ID:your-knowledgebase-id}
    similarity-threshold: 0.6
    max-results: 8
```

说明：
- `DASHSCOPE_API_KEY` 为空时，依赖 DashScope 的能力不可用；
- `knowledgebase-id` 不要写入真实值，使用占位符并通过环境变量覆盖；
- 示例：`export MEMORY_LIBRARY_KNOWLEDGEBASE_ID=你的真实KnowledgeBaseId`；
- `knowledgebase-id` 需与阿里云 DashScope 侧已创建的 Knowledge Base 对应；
- 生产环境建议通过环境变量或配置中心覆盖以上参数，不要将密钥写死在仓库。

### 2) Redis Stream 运行说明

Memory 异步写入链路使用 Redis Stream，默认键如下：
- 主队列：`memory:events:main`
- 重试队列：`memory:events:retry`
- 死信队列：`memory:events:dlq`
- 幂等键前缀：`memory:event:idempotent:`

建议巡检项：
- `memory:events:main` 长度是否持续堆积（消费者是否异常）；
- `memory:events:retry` 是否突增（外部依赖抖动）；
- 幂等键 TTL 是否正常过期（避免重复消费和键泄漏）。

### 3) DLQ 运维建议

- 触发场景：超过最大重试次数、不可重试异常、原始消息反序列化失败；
- 排障优先级：先看 `failure_reason`，再结合上游请求和日志定位；
- 处置流程：修复根因后，按需将 DLQ 事件回放到主队列或人工补偿；
- 安全要求：DLQ 记录中的敏感字段会做脱敏，但运维侧仍需遵守最小权限与日志访问审计策略。

## License

MIT