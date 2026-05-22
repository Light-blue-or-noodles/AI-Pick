# 技术架构方案（Java 版）

**日期**：2026-03-05
**项目**：PopSoda AI 社交小程序

---

## 技术选型

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 前端 | 微信小程序 | 宿主环境 |
| 后端 | Spring Boot 3.2 | 最新 LTS |
| ORM | MyBatis-Plus 3.5 | 简化 CRUD |
| 数据库 | MySQL 8.0 | 主数据库 |
| 缓存 | Redis | 热点数据缓存 |
| AI | Spring AI Alibaba | 百炼/DashScope |
| LBS | 腾讯位置服务 SDK | 附近的人/活动 |
| IM | 腾讯云 IM | 即时通讯 |
| 文件存储 | 腾讯云 COS | 图片/文件存储 |
| 推送 | 腾讯推送 | 消息推送 |

---

## 项目结构

```
├── wechat-mini-program/     # 微信小程序前端
│   ├── pages/
│   │   ├── home/           # AI 首页/发现
│   │   ├── partner/        # 搭子模块
│   │   ├── activity/       # 活动模块
│   │   ├── message/        # 消息中心
│   │   └── profile/        # 个人中心
│   ├── components/
│   ├── utils/
│   └── api/
│
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com/aipick/
│   │       ├── controller/  # REST API
│   │       ├── service/     # 业务逻辑
│   │       ├── mapper/      # MyBatis-Plus Mapper
│   │       ├── entity/      # 实体类
│   │       ├── dto/         # 数据传输对象
│   │       ├── config/      # 配置类
│   │       ├── ai/          # AI 能力封装
│   │       └── common/      # 公共组件
│   │   
│   └── src/main/resources/
│       ├── mapper/          # XML 映射
│       └── application.yml
```

---

## 核心 API 设计

### 用户模块
```
POST   /api/user/register     # 注册
POST   /api/user/login         # 登录（手机号+验证码）
GET    /api/user/profile       # 获取个人信息
PUT    /api/user/profile       # 更新画像
POST   /api/user/company       # 加入公司（邀请码）
POST   /api/user/school        # 加入学校（邀请码）
GET    /api/user/interests     # 获取兴趣标签
POST   /api/user/interests     # 更新兴趣标签
```

### 搭子模块
```
POST   /api/partner            # 发布搭子
GET    /api/partner            # 搭子列表（支持筛选）
GET    /api/partner/{id}       # 搭子详情
POST   /api/partner/{id}/apply # 应征搭子
GET    /api/partner/{id}/applicants # 应征者列表
POST   /api/partner/{id}/accept # 接受应征
POST   /api/partner/{id}/reject # 拒绝应征
POST   /api/partner/ai/generate # AI 生成文案
POST   /api/partner/ai/recommend # AI 智能推荐
GET    /api/partner/ai/match-score # 匹配度计算
```

### 活动模块
```
POST   /api/activity           # 发布活动
GET    /api/activity            # 活动列表（支持筛选）
GET    /api/activity/{id}       # 活动详情
POST   /api/activity/{id}/join  # 报名活动
POST   /api/activity/{id}/cancel # 取消报名
POST   /api/activity/{id}/checkin # 签到
GET    /api/activity/{id}/participants # 参与者列表
POST   /api/activity/ai/generate # AI 生成文案
POST   /api/activity/ai/recommend # AI 智能推荐
```

### AI 模块
```
POST   /api/ai/chat            # AI 对话（首页）
POST   /api/ai/generate-text   # AI 生成文本
POST   /api/ai/recommend       # AI 智能推荐
POST   /api/ai/search          # AI 智能搜索
POST   /api/ai/match-score     # 计算匹配度
POST   /api/ai/tags-suggest    # 兴趣标签建议
POST   /api/ai/break-ice       # 生成破冰话题
```

### 消息模块
```
GET    /api/message/list       # 消息列表
GET    /api/message/{id}       # 消息详情
POST   /api/message/send       # 发送消息
POST   /api/message/read       # 标记已读
```

---

## 核心依赖（pom.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.aipick</groupId>
    <artifactId>aipick-backend</artifactId>
    <version>1.0.0</version>
    
    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- MyBatis-Plus -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
        
        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
        
        <!-- Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- Spring AI Alibaba -->
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-starter</artifactId>
            <version>1.0.0-M2.1</version>
        </dependency>
        
        <!-- 腾讯云 IM -->
        <dependency>
            <groupId>com.tencentcloudapi</groupId>
            <artifactId>tencentcloud-sdk-java-im</artifactId>
            <version>3.1.700</version>
        </dependency>
        
        <!-- 腾讯云 COS -->
        <dependency>
            <groupId>com.qcloud</groupId>
            <artifactId>cos_api</artifactId>
            <version>5.6.155</version>
        </dependency>
        
        <!-- 腾讯位置服务 -->
        <dependency>
            <groupId>com.tencent</groupId>
            <artifactId>tencent-lbs-java-sdk</artifactId>
            <version>1.0.2</version>
        </dependency>
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

---

## 数据库核心表设计

### user（用户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| phone | VARCHAR(20) | 手机号 |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像URL |
| company_id | BIGINT | 公司ID |
| company_name | VARCHAR(100) | 公司名称 |
| company_verified | TINYINT | 公司认证状态 |
| school_id | BIGINT | 学校ID |
| school_name | VARCHAR(100) | 学校名称 |
| school_verified | TINYINT | 学校认证状态 |
| interests | JSON | 兴趣标签数组 |
| location | POINT | 位置坐标 |
| latitude | DECIMAL(10,8) | 纬度 |
| longitude | DECIMAL(11,8) | 经度 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### company（公司表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 公司名称 |
| invite_code | VARCHAR(20) | 邀请码 |
| member_count | INT | 成员数量 |
| create_time | DATETIME | 创建时间 |

### school（学校表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 学校名称 |
| invite_code | VARCHAR(20) | 邀请码 |
| member_count | INT | 成员数量 |
| create_time | DATETIME | 创建时间 |

### partner（搭子表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 发布者ID |
| title | VARCHAR(100) | 标题 |
| description | TEXT | 描述 |
| tags | JSON | 兴趣标签 |
| scope | VARCHAR(20) | 可见范围（ALL/COLLEAGUE/ALUMNI/NEARBY） |
| max_participants | INT | 最大参与人数 |
| latitude | DECIMAL(10,8) | 纬度 |
| longitude | DECIMAL(11,8) | 经度 |
| address | VARCHAR(255) | 地址 |
| status | INT | 状态（0-进行中/1-已完成/2-已取消） |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### partner_apply（搭子申请表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| partner_id | BIGINT | 搭子ID |
| user_id | BIGINT | 申请者ID |
| message | VARCHAR(255) | 应征消息 |
| status | INT | 状态（0-待处理/1-已接受/2-已拒绝） |
| create_time | DATETIME | 创建时间 |

### activity（活动表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 发布者ID |
| title | VARCHAR(100) | 标题 |
| description | TEXT | 描述 |
| tags | JSON | 兴趣标签 |
| scope | VARCHAR(20) | 可见范围 |
| event_time | DATETIME | 活动时间 |
| max_participants | INT | 最大参与人数 |
| latitude | DECIMAL(10,8) | 纬度 |
| longitude | DECIMAL(11,8) | 经度 |
| address | VARCHAR(255) | 地址 |
| status | INT | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### activity_participant（活动参与者表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| activity_id | BIGINT | 活动ID |
| user_id | BIGINT | 用户ID |
| status | INT | 状态（0-已报名/1-已签到/2-已取消） |
| create_time | DATETIME | 创建时间 |

---

## 配置示例（application.yml）

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/aipick?useUnicode=true&characterEncoding=utf8
    username: root
    password: 
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}

# MyBatis-Plus
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

# 腾讯云配置
tencent:
  im:
    app-id: ${TENCENT_IM_APP_ID:}
    secret-id: ${TENCENT_SECRET_ID:}
    secret-key: ${TENCENT_SECRET_KEY:}
  cos:
    secret-id: ${TENCENT_COS_SECRET_ID:}
    secret-key: ${TENCENT_COS_SECRET_KEY:}
    bucket: ${TENCENT_COS_BUCKET:}
    region: ap-beijing
  lbs:
    key: ${TENCENT_LBS_KEY:}
```

---

## AI 能力封装

```java
@Service
public class AiService {
    
    @Autowired
    private ChatClient chatClient;
    
    // AI 对话（首页）
    public String chat(String message, UserContext context) {
        // 1. 意图识别
        // 2. 参数提取
        // 3. 检索匹配
        // 4. 生成回复
    }
    
    // AI 生成文案
    public String generateText(String prompt, String type) {
        // 针对搭子/活动生成描述文案
    }
    
    // AI 智能推荐
    public List<Partner> recommend(Long userId, int limit) {
        // 基于用户画像和兴趣推荐
    }
    
    // 计算匹配度
    public int calculateMatchScore(Long userId1, Long userId2) {
        // 兴趣匹配 + 圈子匹配 + 位置 proximity
    }
}
```

---

*由浅蓝 AI 助理记录 - 2026-03-05*