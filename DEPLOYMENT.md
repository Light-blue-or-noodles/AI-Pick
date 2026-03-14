# AI-Pick 项目部署文档

> 完整的阿里云 + 微信小程序生产环境部署指南

---

## 📋 目录

1. [部署架构](#部署架构)
2. [环境准备](#环境准备)
3. [阿里云服务器部署](#阿里云服务器部署)
4. [中间件部署（Docker）](#中间件部署 docker)
5. [后端服务部署](#后端服务部署)
6. [微信小程序部署](#微信小程序部署)
7. [域名和 HTTPS 配置](#域名和 https 配置)
8. [生产环境检查清单](#生产环境检查清单)
9. [常见问题](#常见问题)

---

## 部署架构

```
┌─────────────────────────────────────────────────────────┐
│                     用户端                               │
│              微信小程序（腾讯云部署）                      │
└────────────────────┬────────────────────────────────────┘
                     │ HTTPS (443)
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  阿里云 ECS 服务器                        │
│  ┌──────────────────────────────────────────────────┐   │
│  │              Nginx (反向代理)                      │   │
│  └──────────────────────────────────────────────────┘   │
│         │                    │                           │
│         ▼                    ▼                           │
│  ┌─────────────┐      ┌─────────────┐                   │
│  │ Spring Boot │      │   MySQL     │                   │
│  │   :8080     │      │   :3306     │                   │
│  └─────────────┘      └─────────────┘                   │
│         │                    │                           │
│         ▼                    ▼                           │
│  ┌─────────────┐      ┌─────────────┐                   │
│  │    Redis    │      │   百炼 AI    │                   │
│  │   :6379     │      │   (API)     │                   │
│  └─────────────┘      └─────────────┘                   │
└─────────────────────────────────────────────────────────┘
```

---

## 环境准备

### 1. 阿里云资源准备

| 资源 | 配置要求 | 数量 | 说明 |
|------|----------|------|------|
| ECS 服务器 | 2 核 4G 及以上 | 1 台 | CentOS 7.9 / Ubuntu 20.04+ |
| 域名 | 已备案 | 1 个 | 用于 API 接口和小程序服务器域名 |
| SSL 证书 | 免费/付费 | 1 个 | 阿里云 SSL 证书服务 |

### 2. 腾讯小程序资源准备

| 资源 | 说明 |
|------|------|
| 小程序账号 | 已注册并认证 |
| AppID | 在微信公众平台获取 |
| 服务器域名 | 需在小程序后台配置（HTTPS） |

### 3. 本地开发环境

```bash
# 需要安装的工具
- JDK 17+
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+
- Git
- 微信开发者工具
```

---

## 阿里云服务器部署

### 1. 登录服务器

```bash
ssh root@your-server-ip
```

### 2. 安装基础软件

```bash
# 更新系统
yum update -y  # CentOS
# 或
apt update && apt upgrade -y  # Ubuntu

# 安装 Docker
curl -fsSL https://get.docker.com | bash -s docker
systemctl enable docker
systemctl start docker

# 安装 Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# 安装 Git
yum install -y git  # CentOS
# 或
apt install -y git  # Ubuntu

# 安装 JDK 17
yum install -y java-17-openjdk-devel  # CentOS
# 或
apt install -y openjdk-17-jdk  # Ubuntu

# 安装 Maven
yum install -y maven  # CentOS
# 或
apt install -y maven  # Ubuntu

# 安装 Nginx
yum install -y nginx  # CentOS
# 或
apt install -y nginx  # Ubuntu
```

### 3. 配置安全组

在阿里云控制台开放以下端口：

| 端口 | 协议 | 说明 |
|------|------|------|
| 22 | TCP | SSH |
| 80 | TCP | HTTP（重定向到 HTTPS） |
| 443 | TCP | HTTPS |
| 3306 | TCP | MySQL（仅内网访问） |
| 6379 | TCP | Redis（仅内网访问） |

---

## 中间件部署（Docker）

### 1. 创建 Docker 网络

```bash
docker network create aipick-network
```

### 2. 创建 docker-compose.yml

在项目根目录创建 `docker-compose.prod.yml`：

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: aipick-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: aipick
      MYSQL_USER: aipick
      MYSQL_PASSWORD: ${DB_PASSWORD}
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - ./data/mysql:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - aipick-network
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-authentication-plugin=mysql_native_password

  redis:
    image: redis:7-alpine
    container_name: aipick-redis
    restart: always
    ports:
      - "6379:6379"
    volumes:
      - ./data/redis:/data
    networks:
      - aipick-network
    command: redis-server --appendonly yes

networks:
  aipick-network:
    driver: bridge
```

### 3. 创建数据库初始化脚本

创建 `init.sql`：

```sql
-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `openid` varchar(64) DEFAULT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `avatar` varchar(255) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `gender` tinyint DEFAULT 0,
  `company_name` varchar(100) DEFAULT NULL,
  `school_name` varchar(100) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建搭子表
CREATE TABLE IF NOT EXISTS `partner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text,
  `type` tinyint DEFAULT 1,
  `target_count` int DEFAULT 1,
  `current_count` int DEFAULT 1,
  `location` varchar(255) DEFAULT NULL,
  `plan_time` datetime DEFAULT NULL,
  `cover_image` varchar(255) DEFAULT NULL,
  `status` tinyint DEFAULT 0,
  `view_count` int DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建活动表
CREATE TABLE IF NOT EXISTS `activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text,
  `type` tinyint DEFAULT 1,
  `location` varchar(255) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `max_count` int DEFAULT 0,
  `current_count` int DEFAULT 0,
  `cover_image` varchar(255) DEFAULT NULL,
  `status` tinyint DEFAULT 0,
  `view_count` int DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建活动报名表
CREATE TABLE IF NOT EXISTS `activity_registration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `status` tinyint DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_user` (`activity_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建消息表
CREATE TABLE IF NOT EXISTS `message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  `content` text,
  `type` tinyint DEFAULT 1,
  `is_read` tinyint DEFAULT 0,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_from_user` (`from_user_id`),
  KEY `idx_to_user` (`to_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4. 启动中间件

```bash
# 上传文件到服务器后
cd /path/to/aipick-backend

# 创建环境变量文件
cat > .env << EOF
DB_PASSWORD=your-strong-password-here
JWT_SECRET=your-jwt-secret-key-here
DASHSCOPE_API_KEY=your-dashscope-api-key
EOF

# 启动中间件
docker-compose -f docker-compose.prod.yml up -d

# 查看状态
docker-compose -f docker-compose.prod.yml ps
```

---

## 后端服务部署

### 1. 部署后端代码

```bash
# 克隆代码（或上传打包好的 jar）
cd /path/to
git clone <your-repo-url> aipick-backend
cd aipick-backend

# 或者上传 jar 包
# scp target/aipick-backend-1.0.0.jar root@server:/path/to/
```

### 2. 修改生产环境配置

创建 `src/main/resources/application-prod.yml`：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api
  tomcat:
    uri-encoding: UTF-8
    max-threads: 200
    connection-timeout: 20000

spring:
  application:
    name: aipick-backend
  profiles:
    active: prod
  web:
    locale: zh_CN
    servlet:
      encoding:
        enabled: true
        charset: UTF-8
        force: true
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/aipick?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: ${DB_PASSWORD}
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 50
      filters: stat,wall,slf4j
      connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 5000ms
      lettuce:
        pool:
          max-active: 20
          max-wait: -1ms
          max-idle: 20
          min-idle: 5

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.aipick.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# JWT 配置
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000
  header: Authorization
  prefix: Bearer

# 日志配置
logging:
  level:
    com.aipick: info
    org.springframework: warn
  file:
    name: /var/log/aipick/backend.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 3. 打包项目

```bash
# 本地打包后上传，或在服务器打包
mvn clean package -DskipTests -P prod
```

### 4. 创建 systemd 服务

创建 `/etc/systemd/system/aipick.service`：

```ini
[Unit]
Description=AI-Pick Backend Service
After=syslog.target network.target

[Service]
Type=simple
User=root
WorkingDirectory=/path/to/aipick-backend
EnvironmentFile=/path/to/aipick-backend/.env
ExecStart=/usr/bin/java -jar target/aipick-backend-1.0.0.jar --spring.profiles.active=prod
ExecStop=/bin/kill -15 $MAINPID
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 5. 启动服务

```bash
# 重载 systemd
systemctl daemon-reload

# 启动服务
systemctl start aipick

# 设置开机自启
systemctl enable aipick

# 查看状态
systemctl status aipick

# 查看日志
journalctl -u aipick -f
```

---

## Nginx 配置

### 1. 创建 Nginx 配置

创建 `/etc/nginx/conf.d/aipick.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # HTTP 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL 证书配置
    ssl_certificate /etc/nginx/ssl/your-domain.crt;
    ssl_certificate_key /etc/nginx/ssl/your-domain.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    
    # 日志
    access_log /var/log/nginx/aipick-access.log;
    error_log /var/log/nginx/aipick-error.log;
    
    # 后端代理
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲配置
        proxy_buffering off;
    }
    
    # 静态资源（如果需要）
    location /static/ {
        alias /path/to/aipick-backend/static/;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 2. 启动 Nginx

```bash
# 测试配置
nginx -t

# 重启 Nginx
systemctl restart nginx

# 设置开机自启
systemctl enable nginx
```

---

## 微信小程序部署

### 1. 配置小程序 AppID

修改 `aipick-mini-program/app.js`：

```javascript
App({
  globalData: {
    userInfo: null,
    baseUrl: 'https://your-domain.com',  // 生产环境地址
    token: null
  },
  // ...
});
```

### 2. 配置服务器域名

登录 [微信公众平台](https://mp.weixin.qq.com/)：

1. 进入「开发」-> 「开发管理」-> 「开发设置」
2. 找到「服务器域名」配置
3. 添加以下域名：
   - **request 合法域名**: `https://your-domain.com`
   - **uploadFile 合法域名**: `https://your-domain.com`
   - **downloadFile 合法域名**: `https://your-domain.com`

### 3. 上传小程序代码

1. 打开 **微信开发者工具**
2. 导入项目：`/path/to/aipick-mini-program`
3. 填写正确的 AppID
4. 点击「上传」按钮
5. 填写版本号和备注

### 4. 提交审核

1. 登录微信公众平台
2. 进入「版本管理」
3. 选择刚上传的版本
4. 点击「提交审核」
5. 填写审核信息（功能介绍、测试账号等）

### 5. 发布上线

审核通过后：

1. 进入「版本管理」
2. 点击「发布」
3. 小程序即刻上线

---

## 域名和 HTTPS 配置

### 1. 申请 SSL 证书

在阿里云控制台：

1. 进入「SSL 证书服务」
2. 点击「购买证书」或「免费证书」
3. 填写域名信息
4. 完成域名验证（DNS 验证或文件验证）
5. 下载证书（选择 Nginx 格式）

### 2. 上传证书到服务器

```bash
# 创建证书目录
mkdir -p /etc/nginx/ssl

# 上传证书文件（将 crt 和 key 文件上传到此目录）
# scp your-domain.crt root@server:/etc/nginx/ssl/
# scp your-domain.key root@server:/etc/nginx/ssl/

# 设置权限
chmod 644 /etc/nginx/ssl/*.crt
chmod 600 /etc/nginx/ssl/*.key
```

### 3. 域名备案

如果域名未在阿里云备案：

1. 登录阿里云备案系统
2. 提交备案申请
3. 准备材料：
   - 个人身份证 / 企业营业执照
   - 域名证书
   - 网站负责人信息
4. 等待审核（通常 10-20 个工作日）

---

## 生产环境检查清单

### 部署前检查

- [ ] 阿里云 ECS 服务器已购买并配置
- [ ] 域名已购买并完成备案
- [ ] SSL 证书已申请并下载
- [ ] 小程序账号已注册并认证
- [ ] 百炼 AI API Key 已申请

### 中间件检查

- [ ] Docker 和 Docker Compose 已安装
- [ ] MySQL 容器运行正常
- [ ] Redis 容器运行正常
- [ ] 数据库已初始化
- [ ] 数据库密码已修改为强密码

### 后端检查

- [ ] JDK 17 已安装
- [ ] Maven 已安装
- [ ] 项目已打包
- [ ] systemd 服务已配置
- [ ] 服务已启动并运行正常
- [ ] 日志文件可正常写入

### Nginx 检查

- [ ] Nginx 已安装
- [ ] SSL 证书已部署
- [ ] Nginx 配置已测试通过
- [ ] HTTP 到 HTTPS 重定向正常
- [ ] 反向代理正常

### 小程序检查

- [ ] AppID 配置正确
- [ ] 服务器域名已配置（HTTPS）
- [ ] 小程序代码已上传
- [ ] 小程序已提交审核
- [ ] 小程序已发布上线

### 监控和告警

- [ ] 服务器监控已配置（CPU、内存、磁盘）
- [ ] 应用日志已配置轮转
- [ ] 数据库备份已配置
- [ ] 告警通知已配置

---

## 常见问题

### 1. 小程序请求失败

**问题**: 小程序调用接口返回 `request:fail url not in domain list`

**解决**: 
- 检查微信公众平台服务器域名配置
- 确保使用 HTTPS 协议
- 确保域名已备案

### 2. 后端服务无法启动

**问题**: `java.net.BindException: Address already in use`

**解决**:
```bash
# 查看端口占用
lsof -i :8080

# 停止占用进程
kill -9 <PID>

# 或修改端口
```

### 3. 数据库连接失败

**问题**: `Communications link failure`

**解决**:
- 检查 MySQL 容器是否运行：`docker ps`
- 检查数据库密码是否正确
- 检查网络连接：`telnet localhost 3306`

### 4. SSL 证书无效

**问题**: 浏览器提示证书错误

**解决**:
- 检查证书是否过期
- 检查域名是否匹配
- 检查证书链是否完整

### 5. 跨域问题

**问题**: 小程序请求被拦截

**解决**:
- 检查 Nginx CORS 配置
- 确保后端配置了正确的跨域头
- 检查小程序域名白名单

---

## 运维脚本

### 查看服务状态

```bash
#!/bin/bash
echo "=== AI-Pick 服务状态 ==="
echo ""
echo "后端服务:"
systemctl status aipick --no-pager
echo ""
echo "Nginx:"
systemctl status nginx --no-pager
echo ""
echo "MySQL:"
docker ps | grep mysql
echo ""
echo "Redis:"
docker ps | grep redis
```

### 日志查看

```bash
# 后端日志
tail -f /var/log/aipick/backend.log

# Nginx 访问日志
tail -f /var/log/nginx/aipick-access.log

# Nginx 错误日志
tail -f /var/log/nginx/aipick-error.log

# 系统日志
journalctl -u aipick -f
```

### 备份脚本

```bash
#!/bin/bash
# backup.sh - 数据库备份脚本

BACKUP_DIR="/backup/mysql"
DATE=$(date +%Y%m%d_%H%M%S)
DB_NAME="aipick"
DB_USER="root"
DB_PASS="your-password"

mkdir -p $BACKUP_DIR

mysqldump -u$DB_USER -p$DB_PASS $DB_NAME > $BACKUP_DIR/aipick_$DATE.sql

# 保留最近 7 天的备份
find $BACKUP_DIR -name "*.sql" -mtime +7 -delete

echo "备份完成：$BACKUP_DIR/aipick_$DATE.sql"
```

添加到 crontab：
```bash
# 每天凌晨 2 点备份
0 2 * * * /path/to/backup.sh
```

---

## 联系方式

如有问题，请联系：
- 技术支持：your-email@example.com
- 项目文档：/Users/yanleishi/AI/project/OpenClaw/

---

*最后更新：2026-03-14*
*版本：v1.0.0*
