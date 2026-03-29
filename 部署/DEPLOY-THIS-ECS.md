# 用当前 ECS 部署后端（操作清单）

针对你这台 **北京地域** ECS（`i-2ze96wkuggis1yydhtuc`）的部署步骤。

---

## 控制台已帮你完成

- **实例已启动**：已在控制台点击「启动」，实例会在一两分钟内变为「运行中」。

---

## 你需要手动完成

### 1. 配置安全组（放行 22、80、443）

浏览器已打开 **公网入方向规则** 页面，若未打开可点：

**https://ecs.console.aliyun.com/securityGroupDetail/region/cn-beijing/groupId/sg-2ze96wkuggis1yy89vw6/detail/internetIngress**

**方式 A：快速添加（推荐）**

1. 点击 **「快速添加规则」**。
2. 在弹层中**只保留**这三项勾选，其余取消勾选：
   - **SSH 远程连接 Linux 实例**（端口 22）
   - **Web HTTP 流量访问**（端口 80）
   - **Web HTTPS 流量访问**（端口 443）
3. 若「确定」可点，点击 **「确定」** 保存。若提示“规则已存在”或确定为灰，说明已有这些规则，可跳过。
4. 关闭弹层。

**方式 B：手动增加规则**

1. 点击 **「增加规则」**。
2. 在表格中新行或弹窗中逐条添加：

| 授权策略 | 协议类型 | 端口范围 | 授权对象   | 描述 |
|----------|----------|----------|------------|------|
| 允许     | TCP      | 22/22    | 0.0.0.0/0  | SSH  |
| 允许     | TCP      | 80/80    | 0.0.0.0/0  | HTTP |
| 允许     | TCP      | 443/443  | 0.0.0.0/0  | HTTPS |

3. 保存。

### 2. 获取公网 IP

1. 打开 [实例列表（北京）](https://ecs.console.aliyun.com/#/server/list?regionId=cn-beijing)。
2. 找到你的实例，等状态为 **运行中** 后，记下 **公网 IP**（如 `47.93.xxx.xxx`）。后面 SSH 和上传都用这个 IP。

### 3. 在 ECS 上拉取代码（推荐：从 GitHub）

若代码已推送到 GitHub，可在 ECS 上直接克隆并部署，无需本机 scp。详见 **GITHUB-DEPLOY.md**。

**方式 A：从 GitHub 部署（推荐）**

```bash
ssh root@你的公网IP
cd /opt && git clone https://github.com/你的用户名/OpenClaw.git
cd OpenClaw/scripts && chmod +x deploy-from-github.sh
./deploy-from-github.sh https://github.com/你的用户名/OpenClaw.git
```

**方式 B：本机 scp 上传**

```bash
cd /Users/yanleishi/AI/Project/OpenClaw
scp -r backend/aipick-backend root@你的公网IP:/opt/
```

### 4. 在服务器上执行部署脚本

SSH 登录后，在服务器上执行：

```bash
ssh root@你的公网IP
```

登录成功后：

```bash
# 一键安装环境并启动中间件（会提示你创建 .env）
cd /opt/aipick-backend
chmod +x run-on-server.sh
./run-on-server.sh
```

脚本会：

- 安装 Docker、Docker Compose、JDK 17、Maven、Git、Nginx（若未安装）
- 提示你创建 `/opt/aipick-backend/.env` 并填写 `DB_PASSWORD`、`JWT_SECRET`、`DASHSCOPE_API_KEY`
- 启动 MySQL、Redis（Docker）
- 创建日志目录、提示你打包并配置 systemd、Nginx

按脚本中的提示完成 **.env**、**打包 jar**、**systemd 与 Nginx** 即可。

---

## 简要流程回顾

1. 控制台：安全组添加入方向 22、80、443 → 保存。  
2. 控制台：实例列表记下公网 IP。  
3. 本机：`scp -r backend/aipick-backend root@公网IP:/opt/`  
4. 本机：`ssh root@公网IP`  
5. 服务器：`cd /opt/aipick-backend && chmod +x run-on-server.sh && ./run-on-server.sh`  
6. 按脚本提示创建 .env、打包 jar、配置 systemd 与 Nginx。

详细说明见 **DEPLOY-GUIDE-ALIYUN.md**。
