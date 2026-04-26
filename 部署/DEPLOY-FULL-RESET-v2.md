# AI-Pick 全量清空与重新部署（v2）

> 目标：在阿里云 ECS 上**停掉旧服务、清空本项目中间件与业务数据**（MySQL/Redis 持久化卷），再按统一路径**重新拉起完整后端栈**。  
> 与旧文档关系：`DEPLOYMENT.md` / `DEPLOY-GUIDE-ALIYUN.md` 仍作细节参考，**以本文件为当前推荐主流程**。

**重要**：

- 本流程会**删除 MySQL 与 Redis 的本地数据目录**，等同于**库表与用户数据全部清空**（无备份则不可恢复）。  
- 我（助理）**无法代你执行 SSH**；请你在能登录 ECS 的机器上按步骤操作。  
- 仓库内路径历史上存在多套：`/opt/OpenClaw/...`（`deploy-from-github.sh`）、`/opt/aipick-backend`（`scp`）、`/opt/AI-Pick/...`（旧脚本 `complete-deploy-on-ecs.sh`）。**下文统一以 `BACKEND_DIR` 指 `.../aipick-backend` 实解目录，请你用 `pwd` 确认本机是哪一个。**

---

## 一、部署拓扑（与现网一致）

```
微信小程序 HTTPS → 域名（备案） → ECS: Nginx 80/443 → 127.0.0.1:8080/api → Spring Boot (prod)
                                              └→ Docker: MySQL:3306、Redis:6379（仅本机）
```

- 后端上下文路径：`/api`（`application-prod.yml`）  
- 容器名（`docker-compose.prod.yml`）：`aipick-mysql`、`aipick-redis`  
- 数据目录（宿主机，相对 `BACKEND_DIR`）：`data/mysql`、`data/redis`  
- systemd 服务名：`aipick`  
- 产物 jar：`target/aipick-backend-1.0.0.jar`（与 `pom.xml` 版本 `1.0.0` 一致）

---

## 二、清空前：是否需要备份

若线上有需要保留的数据：

```bash
# 在能连上 ECS 上 MySQL 时（容器运行中、或最后的机会）
# 在 ECS 上，替换成你的 root 密码与库名
docker exec aipick-mysql mysqldump -uroot -p"$DB_PASS" aipick > /root/aipick-backup-$(date +%Y%m%d).sql
```

**不需要保留**则跳过，直接进入下一节。

---

## 三、在 ECS 上「清空服务与中间件数据」

### 3.1 将脚本放到服务器（二选一）

**方式 A：已在仓库中（推荐）**  
在本地有完整仓库时，将项目同步到服务器后执行：

```bash
cd /你克隆到的路径/OpenClaw/scripts
chmod +x teardown-aipick-on-ecs.sh
./teardown-aipick-on-ecs.sh /你的/BACKEND_DIR/aipick-backend --i-understand-data-loss
```

**方式 B：只上传这一个脚本**  

```bash
scp /你本机路径/OpenClaw/scripts/teardown-aipick-on-ecs.sh root@ECS公网IP:/root/
ssh root@ECS公网IP
chmod +x /root/teardown-aipick-on-ecs.sh
# 省略目录时，会依次尝试 /opt/OpenClaw、/opt/aipick-backend、/opt/AI-Pick 下常见 aipick-backend
./teardown-aipick-on-ecs.sh --i-understand-data-loss
```

> 显式指定目录时：`./teardown-aipick-on-ecs.sh /opt/OpenClaw/backend/aipick-backend --i-understand-data-loss`（目录与确认参数顺序可互换，脚本会识别）。

### 3.2 脚本实际做了什么

1. `systemctl stop` / `disable` **aipick**，删除 `/etc/systemd/system/aipick.service` 并 `daemon-reload`  
2. 在 `BACKEND_DIR` 下 `docker compose … down`（或 `docker-compose`），并 `docker rm -f` 残留 `aipick-mysql`、`aipick-redis`  
3. 删除 `BACKEND_DIR/data/mysql`、`BACKEND_DIR/data/redis`（**所有业务数据**）  
4. 删除 `BACKEND_DIR/target/aipick-backend-1.0.0.jar`（**不删 `.env`**，避免密钥丢失；若你换了 `DB_PASSWORD`，后面重建 MySQL 前**务必改 `.env` 与库一致**）

### 3.3 未自动处理项（按需手动）

- **Nginx**：不会删除 `conf.d` 里你配好的 `aipick.conf` 或 HTTPS 证书。若你要完全重建 HTTP 反代，可对照下节在改完 `server_name` 后 `nginx -t && systemctl restart nginx`。  
- **非本项目容器**（如其它名字的 MySQL）：本脚本不碰；请自行 `docker ps -a` 判断。

---

## 四、重新部署（完整服务）

### 4.1 确定代码在服务器上的位置

| 方式 | 典型 `BACKEND_DIR` |
|------|---------------------|
| GitHub clone（`scripts/deploy-from-github.sh`） | `/opt/OpenClaw/backend/aipick-backend` |
| 本机 scp 整个 `backend/aipick-backend` 到 `/opt` | `/opt/aipick-backend` |
| 历史路径 | `/opt/AI-Pick/backend/aipick-backend` |

**整仓库**推荐：

```bash
cd /opt
git clone <你的 OpenClaw 仓库 URL> OpenClaw
cd OpenClaw/backend/aipick-backend
```

### 4.2 配置环境变量

```bash
cd "$BACKEND_DIR"
# 若 .env 已被清空或不存在，参考 .env.example / 原模板填写：
# DB_PASSWORD、JWT_SECRET、DASHSCOPE_API_KEY、WECHAT_APPID、WECHAT_SECRET 等
vi .env
```

### 4.3 安装依赖与中间件（`run-on-server.sh`）

```bash
cd "$BACKEND_DIR"
chmod +x run-on-server.sh
./run-on-server.sh
```

- 若提示先生成 `.env` 模板，编辑保存后**再执行一次** `./run-on-server.sh`。  
- 成功后会拉起 **MySQL、Redis**（首次会执行 `init.sql` 建库表）。

### 4.4 打 jar 并安装 systemd、Nginx

在 **ECS 上**（与 `run-on-server.sh` 同机）：

```bash
cd "$BACKEND_DIR"
mvn -q clean package -DskipTests
sudo tee /etc/systemd/system/aipick.service << EOF
[Unit]
Description=AI-Pick Backend
After=network.target docker.service

[Service]
Type=simple
User=root
WorkingDirectory=$BACKEND_DIR
EnvironmentFile=$BACKEND_DIR/.env
ExecStart=/usr/bin/java -jar $BACKEND_DIR/target/aipick-backend-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
sudo systemctl daemon-reload
sudo systemctl enable aipick
sudo systemctl start aipick
sudo systemctl status aipick --no-pager
```

Nginx 最小反代（与现有文档一致，HTTP；若你已有 HTTPS，在原有 `server` 中保留证书块并只加 `location /api/`）：

```bash
sudo tee /etc/nginx/conf.d/aipick.conf << 'NGXEOF'
server {
  listen 80;
  server_name _;
  location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
NGXEOF
sudo nginx -t && sudo systemctl restart nginx
```

### 4.5 表结构：若 `init.sql` 落后于本仓库

新库用当前仓库 `init.sql` 做首次初始化。若你后续仍从旧 ECS 升上来，可执行项目内幂等脚本（路径以仓库为准）：

```bash
# 在 ECS 上，进入仓库根或把 sql 拷上去后
docker exec -i aipick-mysql mysql -uroot -p"你的DB_PASSWORD" aipick < /path/to/sync-schema-to-ecs.sql
# 或见 DEPLOYMENT.md「本地与 ECS 数据库结构同步」
```

然后：

```bash
sudo systemctl restart aipick
```

### 4.6 验证

```bash
curl -sS -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/
journalctl -u aipick -n 50 --no-pager
```

外网用域名/HTTPS 时，在小程序后台与 `aipick-mini-program` 的 API 基地址中配置**与线上一致**的 `https://你的域名`（**不要**在合法域名里写带路径的 `/api`；路径由小程序代码拼接）。

---

## 五、小程序与域名（简要）

- 微信公众平台：**开发 → 开发管理 → 开发设置 → 服务器域名**，填入已备案域名的 `https` 根，与证书一致。  
- 与细节流程见 `阿里云域名申请与微信小程序备案方案.md`、原 `DEPLOYMENT.md` 相关章节。

---

## 六、检查清单

- [ ] 已确认**不需要**旧库数据，或已完成 **mysqldump**  
- [ ] 已执行 `teardown-aipick-on-ecs.sh … --i-understand-data-loss`  
- [ ] `BACKEND_DIR` 中 `.env` 已填且与计划中的 MySQL 密码一致  
- [ ] `docker ps` 中 `aipick-mysql`、`aipick-redis` 为 Up  
- [ ] `systemctl status aipick` 为 active，`curl` 本机 8080 `/api/` 有响应  
- [ ] Nginx 已反代 80/443 至后端（按你现网选择 HTTP 或 HTTPS）  
- [ ] 表结构已用 `init.sql` 或 `sync-schema-to-ecs.sql` 对齐（如有需要）  
- [ ] 小程序 baseUrl/合法域名与线上一致  

---

*文档版本：v2*  
*依赖脚本：`scripts/teardown-aipick-on-ecs.sh`、`backend/aipick-backend/run-on-server.sh`、`scripts/deploy-from-github.sh`*
