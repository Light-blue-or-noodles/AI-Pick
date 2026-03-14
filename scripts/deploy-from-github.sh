#!/bin/bash
# 在 ECS 上通过 GitHub 拉取代码并部署后端
# 用法：在 ECS 上执行，需传入 GitHub 仓库 URL（HTTPS 或 SSH）
# 示例：curl -sL <raw_url> | bash -s -- https://github.com/你的用户名/OpenClaw.git
# 或：先 scp 本脚本到 ECS，再 chmod +x deploy-from-github.sh && ./deploy-from-github.sh https://github.com/你的用户名/OpenClaw.git

set -e
REPO_URL="${1:-}"
DEPLOY_DIR="/opt/OpenClaw"
BACKEND_DIR="$DEPLOY_DIR/backend/aipick-backend"

if [ -z "$REPO_URL" ]; then
  echo "用法: $0 <GitHub仓库URL>"
  echo "示例: $0 https://github.com/你的用户名/OpenClaw.git"
  exit 1
fi

echo "=== 从 GitHub 部署到 ECS ==="
echo "仓库: $REPO_URL"
echo "目录: $DEPLOY_DIR"
echo ""

# 安装 Git（若未安装）
if ! command -v git &>/dev/null; then
  echo "安装 Git..."
  if [ -f /etc/redhat-release ]; then
    yum install -y git
  else
    apt update && apt install -y git
  fi
fi

# 克隆或拉取
if [ -d "$DEPLOY_DIR/.git" ]; then
  echo "已存在仓库，执行 git pull..."
  cd "$DEPLOY_DIR"
  git pull --rebase || git pull
else
  echo "克隆仓库..."
  mkdir -p "$(dirname "$DEPLOY_DIR")"
  git clone "$REPO_URL" "$DEPLOY_DIR"
  cd "$DEPLOY_DIR"
fi

# 进入后端目录并执行部署脚本
if [ ! -f "$BACKEND_DIR/run-on-server.sh" ]; then
  echo "错误: 未找到 $BACKEND_DIR/run-on-server.sh"
  exit 1
fi

cd "$BACKEND_DIR"
chmod +x run-on-server.sh
exec ./run-on-server.sh
