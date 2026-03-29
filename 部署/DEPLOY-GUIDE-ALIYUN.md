# 阿里云配置与后端部署分步指南

按顺序完成以下步骤，即可在阿里云 ECS 上完成环境配置和后端服务部署。

---

## 第一步：购买与配置阿里云 ECS

### 1.1 购买 ECS

1. 登录 [阿里云控制台](https://ecs.console.aliyun.com/)。
2. 选择 **创建实例**（或 购买云服务器 ECS）。
3. 建议配置：
   - **地域**：选离目标用户近的（如华东1）
   - **实例规格**：2 核 4 GiB 起（如 ecs.t6-c1m2.large 或 突发性能型）
   - **镜像**：**CentOS 7.9** 或 **Ubuntu 20.04**
   - **系统盘**：40 GB 及以上
   - **网络**：按量付费或包年包月自选
4. **设置 root 密码**（务必记住），或绑定密钥对。
5. 完成购买，记下 **公网 IP**（如 `47.96.xxx.xxx`）。

### 1.2 配置安全组（放行端口）

1. 在 ECS 控制台找到你的实例 → 点击实例 ID。
2. 左侧或本页找到 **安全组** → 点击安全组 ID。
3. **配置规则** → **入方向** → **手动添加**，添加如下规则：

| 端口范围 | 授权对象 | 协议 | 说明 |
|----------|----------|------|------|
| 22/22    | 0.0.0.0/0 | TCP | SSH 登录 |
| 80/80    | 0.0.0.0/0 | TCP | HTTP（后续可做重定向） |
| 443/443  | 0.0.0.0/0 | TCP | HTTPS |
| 8080/8080 | 0.0.0.0/0 | TCP | 后端接口（仅测试时可开放；正式建议只通过 Nginx 443 访问） |

> 生产环境建议：只开放 22、80、443，不对外直接开放 8080，由 Nginx 反向代理。

保存后，用 SSH 测试能否登录。

### 1.3 登录服务器

在本地终端执行（将 `your-server-ip` 换成你的公网 IP）：

```bash
ssh root@your-server-ip
```

输入 root 密码后即可进入服务器。

---

## 第二步：在服务器上安装基础软件

在 ECS 上执行以下命令（**CentOS 7** 示例；若是 **Ubuntu**，将 `yum` 改为 `apt`，包名略有不同）。

### 2.1 更新系统

```bash
# CentOS
yum update -y

# Ubuntu
# apt update && apt upgrade -y
```

### 2.2 安装 Docker

```bash
curl -fsSL https://get.docker.com | bash -s docker
systemctl enable docker
systemctl start docker
docker --version
```

### 2.3 安装 Docker Compose

```bash
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

### 2.4 安装 JDK 17、Maven、Git、Nginx

```bash
# CentOS
yum install -y git java-17-openjdk-devel maven nginx

# Ubuntu
# apt install -y git openjdk-17-jdk maven nginx
```

验证：

```bash
java -version   # 应显示 17
mvn -v
git --version
nginx -v
```

---

## 第三步：部署 MySQL 和 Redis（Docker）

### 3.1 准备部署目录与文件

在服务器上创建目录并上传项目文件（二选一）。

**方式 A：本地上传（推荐）**

在**本地**项目根目录执行（替换为你的服务器 IP）：

```bash
# 在本地 OpenClaw 目录下
cd /Users/yanleishi/AI/Project/OpenClaw

# 上传整个后端目录（含 docker-compose.prod.yml、src/main/resources/init.sql）
scp -r backend/aipick-backend root@your-server-ip:/opt/
```

上传后服务器上路径为 `/opt/aipick-backend`，后续步骤均以此路径为准。

**方式 B：服务器上 Git 克隆**

```bash
ssh root@your-server-ip
mkdir -p /opt && cd /opt
git clone <你的仓库地址> OpenClaw
cd OpenClaw/backend/aipick-backend
```

### 3.2 创建环境变量文件

在服务器上进入后端目录并创建 `.env`（**务必改成你自己的强密码和 API Key**）：

```bash
cd /opt/aipick-backend

cat > .env << 'EOF'
DB_PASSWORD=你的数据库强密码
JWT_SECRET=你的JWT密钥至少32位
DASHSCOPE_API_KEY=你的百炼API-Key
EOF
```

### 3.3 启动 MySQL 和 Redis

```bash
cd /opt/aipick-backend
docker-compose -f docker-compose.prod.yml up -d
docker-compose -f docker-compose.prod.yml ps
```

应看到 `aipick-mysql` 和 `aipick-redis` 均为 Up。首次启动 MySQL 会自动执行 `init.sql` 建表。

### 3.4 检查数据库

```bash
docker exec -it aipick-mysql mysql -uroot -p
# 输入 .env 里的 DB_PASSWORD

MySQL> SHOW DATABASES;
MySQL> USE aipick; SHOW TABLES;
MySQL> exit;
```

---

## 第四步：配置生产环境并打包后端

### 4.1 生产配置文件

项目已提供 `application-prod.yml`，使用环境变量：

- `DB_PASSWORD`、`JWT_SECRET`、`DASHSCOPE_API_KEY` 从 `.env` 或系统环境变量读取。
- 数据库地址为 `localhost:3306`，Redis 为 `localhost:6379`（与 Docker 映射一致）。

若你在服务器上改过 `.env`，无需再改配置文件。

### 4.2 打包方式（二选一）

**方式 A：在本地打包后上传（推荐）**

在本地：

```bash
cd /Users/yanleishi/AI/Project/OpenClaw/backend/aipick-backend
mvn clean package -DskipTests
```

将生成的 jar 上传到服务器：

```bash
scp target/aipick-backend-1.0.0.jar root@your-server-ip:/opt/aipick-backend/
```

**方式 B：在服务器上打包**

```bash
cd /opt/aipick-backend
mvn clean package -DskipTests
```

### 4.3 确认 jar 与目录

在服务器上应存在：

- `/opt/aipick-backend/target/aipick-backend-1.0.0.jar`
- `/opt/aipick-backend/.env`

---

## 第五步：用 systemd 运行后端服务

### 5.1 创建日志目录并创建 systemd 服务文件

```bash
mkdir -p /var/log/aipick
```

```bash
cat > /etc/systemd/system/aipick.service << 'EOF'
[Unit]
Description=AI-Pick Backend Service
After=network.target docker.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/aipick-backend
EnvironmentFile=/opt/aipick-backend/.env
ExecStart=/usr/bin/java -jar /opt/aipick-backend/target/aipick-backend-1.0.0.jar --spring.profiles.active=prod
ExecStop=/bin/kill -15 $MAINPID
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
```

若你的部署路径不是 `/opt/aipick-backend`，请把上面两处 `/opt/aipick-backend` 改成实际路径。

### 5.2 启动并设置开机自启

```bash
systemctl daemon-reload
systemctl start aipick
systemctl enable aipick
systemctl status aipick
```

状态应为 `active (running)`。

### 5.3 查看日志

```bash
journalctl -u aipick -f
```

看到 Spring Boot 启动完成、无报错即可。按 `Ctrl+C` 退出日志。

### 5.4 本机验证接口

在服务器上：

```bash
curl -s http://localhost:8080/api/actuator/health
# 若项目有健康检查接口，可返回 OK；若无则可能 404，但 8080 有响应即说明服务在跑
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/
```

或在本地浏览器访问：`http://你的公网IP:8080/api/`（仅当安全组开放 8080 时）。

---

## 第六步：配置 Nginx 反向代理（可选但推荐）

若希望通过 80/443 访问、且为后续 HTTPS 做准备，可先配 HTTP。

### 6.1 创建 Nginx 配置

```bash
cat > /etc/nginx/conf.d/aipick.conf << 'EOF'
server {
    listen 80;
    server_name _;
    
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        proxy_buffering off;
    }
}
EOF
```

### 6.2 测试并重载 Nginx

```bash
nginx -t
systemctl restart nginx
systemctl enable nginx
```

在本地访问：`http://你的公网IP/api/`，应能访问到后端（若后端有根路径接口则会返回内容）。

---

## 部署检查清单

- [ ] ECS 安全组已放行 22、80、443（及临时 8080 如需要）
- [ ] Docker、Docker Compose、JDK17、Maven、Git、Nginx 已安装
- [ ] `.env` 中已设置强密码和 `DASHSCOPE_API_KEY`
- [ ] `docker-compose -f docker-compose.prod.yml up -d` 后 MySQL、Redis 正常
- [ ] 数据库 `aipick` 及表已存在
- [ ] `aipick-backend-1.0.0.jar` 存在且 `systemctl status aipick` 为 active
- [ ] 本机 `curl http://localhost:8080/api/...` 有响应
- [ ] Nginx 已配置并重启，外网可通过 `http://公网IP/api/` 访问

---

## 常见问题

**1. 8080 端口被占用**

```bash
lsof -i :8080
kill -9 <PID>
```

**2. 数据库连不上**

- 确认 MySQL 容器在运行：`docker ps | grep mysql`
- 确认 `.env` 中 `DB_PASSWORD` 与 `docker-compose.prod.yml` 中一致
- 测试：`docker exec -it aipick-mysql mysql -uroot -p` 用同一密码登录

**3. 服务启动报错找不到 main**

- 确认 jar 路径与 `ExecStart` 中的路径一致
- 确认是 `aipick-backend-1.0.0.jar`（与 pom.xml 中 version 一致）

**4. 小程序或浏览器访问 API 跨域/超时**

- 先确认同一台机子上 `curl http://localhost:8080/api/...` 正常
- 再查安全组是否放行 80/443，Nginx 是否把 `/api/` 反代到 8080

完成以上步骤后，阿里云 ECS 上的基础环境与后端服务即部署完成。后续可进行域名备案、SSL 证书配置与小程序服务器域名配置，详见 `DEPLOYMENT.md`。
