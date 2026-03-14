# 通过 GitHub 在 ECS 上部署

代码推送到 GitHub 后，在 ECS 上通过 **git clone / git pull** 拉取代码并部署，无需本机 scp 上传。

---

## 一、把代码推送到 GitHub

### 1. 在 GitHub 创建仓库

1. 打开 [GitHub](https://github.com/new)。
2. 仓库名建议：`OpenClaw`（或任意名称）。
3. 选择 **Private** 或 **Public**，**不要**勾选 “Add a README”（本地已有代码）。
4. 创建仓库后，记下仓库 URL，例如：`https://github.com/你的用户名/OpenClaw.git`。

### 2. 本地初始化并推送

在项目根目录执行（将 `你的用户名/OpenClaw` 换成你的仓库地址）：

```bash
cd /Users/yanleishi/AI/Project/OpenClaw

# 初始化 Git（若尚未初始化）
git init
git branch -M main

# 添加远程仓库（替换为你的 GitHub 仓库 URL）
git remote add origin https://github.com/你的用户名/OpenClaw.git

# 添加并提交
git add .
git status   # 确认没有误加入 .env、target、uploads 等
git commit -m "feat: initial commit, backend + mini-program + deploy"

# 推送（首次可能需登录 GitHub 或配置 SSH/Token）
git push -u origin main
```

若使用 **SSH**：

```bash
git remote add origin git@github.com:你的用户名/OpenClaw.git
git push -u origin main
```

> **注意**：`.env`、`target/`、`uploads/` 已写入 `.gitignore`，不会被提交。密钥和密码只保存在服务器本地的 `.env` 中。

---

## 二、在 ECS 上通过 GitHub 部署

### 方式 A：一键脚本（推荐）

在 ECS 上执行（**替换为你的仓库 URL**）：

```bash
# 若为公开仓库，可直接拉取脚本并执行（需先上传脚本或复制内容）
# 这里以「先 clone 再在仓库内执行」为例
cd /opt
git clone https://github.com/你的用户名/OpenClaw.git
cd OpenClaw/scripts
chmod +x deploy-from-github.sh
./deploy-from-github.sh https://github.com/你的用户名/OpenClaw.git
```

脚本会：克隆仓库到 `/opt/OpenClaw`，进入 `backend/aipick-backend`，执行 `run-on-server.sh`（安装 Docker/JDK/Maven、启动 MySQL/Redis、提示 .env 与 systemd）。

### 方式 B：手动克隆后部署

```bash
# 1. SSH 登录 ECS
ssh root@你的公网IP

# 2. 克隆仓库（首次）
cd /opt
git clone https://github.com/你的用户名/OpenClaw.git
cd OpenClaw/backend/aipick-backend

# 3. 创建 .env（仅首次，按提示填写 DB_PASSWORD、JWT_SECRET、DASHSCOPE_API_KEY）
cat > .env << 'EOF'
DB_PASSWORD=你的数据库强密码
JWT_SECRET=至少32位随机字符串
DASHSCOPE_API_KEY=你的百炼API-Key
WECHAT_APPID=
WECHAT_SECRET=
EOF

# 4. 运行部署脚本
chmod +x run-on-server.sh
./run-on-server.sh

# 5. 按脚本提示：打包 jar（或本机打包后 scp）、配置 systemd、Nginx
```

### 后续更新代码（仅拉取再重启）

```bash
cd /opt/OpenClaw
git pull
cd backend/aipick-backend
# 若需重新打包
mvn clean package -DskipTests
systemctl restart aipick
```

---

## 三、私有仓库：在 ECS 上配置访问

若仓库为 **Private**，ECS 需能访问 GitHub，任选其一：

- **HTTPS + Personal Access Token**  
  克隆时使用：`https://你的Token@github.com/你的用户名/OpenClaw.git`，或配置 `git config credential.helper store` 后输入一次 Token。
- **SSH 密钥**  
  在 ECS 上生成 SSH 密钥，把公钥添加到 GitHub：Settings → SSH and GPG keys → New SSH key。克隆用：`git@github.com:你的用户名/OpenClaw.git`。

---

## 四、可选：GitHub Actions 自动部署

仓库中已包含 workflow 模板：**`.github/workflows/deploy-ecs.yml`**。

在 GitHub 仓库 **Settings → Secrets and variables → Actions** 中新增：

| Secret 名称     | 说明           |
|-----------------|----------------|
| `ECS_HOST`      | ECS 公网 IP    |
| `ECS_USER`      | SSH 用户名，如 `root` |
| `ECS_SSH_KEY`   | 登录 ECS 的 SSH 私钥全文 |

推送代码到默认分支（如 `main`）后，Actions 会 SSH 到 ECS 执行 `git pull`、`mvn package`、`systemctl restart aipick`。若不需要自动部署，可删除 `.github/workflows/deploy-ecs.yml`。

---

## 流程小结

| 步骤 | 操作 |
|------|------|
| 1 | GitHub 建仓，本地 `git init` → `git remote add origin` → `git add` → `git commit` → `git push` |
| 2 | ECS：`git clone` 你的仓库到 `/opt/OpenClaw`，或运行 `scripts/deploy-from-github.sh <仓库URL>` |
| 3 | ECS：在 `backend/aipick-backend` 下创建 `.env`，执行 `run-on-server.sh` |
| 4 | 按脚本提示完成 jar 打包、systemd、Nginx；后续改代码后 `git pull` + 重启服务即可 |

详细环境要求与故障排查见 **DEPLOY-GUIDE-ALIYUN.md**、**DEPLOY-THIS-ECS.md**。
