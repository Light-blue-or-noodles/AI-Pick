#!/bin/bash
# 从本机通过 SSH 在 ECS 上执行「从 GitHub 部署」
# 用法：./deploy-to-ecs-now.sh <ECS公网IP>
# 示例：./deploy-to-ecs-now.sh 47.93.xxx.xxx

set -e
ECS_IP="${1:-$ECS_IP}"
REPO_URL="https://github.com/Light-blue-or-noodles/AI-Pick.git"

if [ -z "$ECS_IP" ]; then
  echo "用法: $0 <ECS公网IP>"
  echo "示例: $0 47.93.xxx.xxx"
  echo "或: ECS_IP=47.93.xxx.xxx $0"
  exit 1
fi

echo "=== 在 ECS ($ECS_IP) 上从 GitHub 部署 ==="
echo "将执行: 克隆/拉取 $REPO_URL → 运行部署脚本"
echo "若需密码，请根据提示输入 root 密码。"
echo ""

ssh -o StrictHostKeyChecking=accept-new root@"$ECS_IP" "bash -s" << 'REMOTE_SCRIPT'
set -e
REPO_URL="https://github.com/Light-blue-or-noodles/AI-Pick.git"
DEPLOY_DIR="/opt/AI-Pick"
BACKEND_DIR="$DEPLOY_DIR/backend/aipick-backend"

# 安装 Git（若未安装）
if ! command -v git &>/dev/null; then
  echo "安装 Git..."
  [ -f /etc/redhat-release ] && yum install -y git || (apt update && apt install -y git)
fi

# 克隆或拉取
if [ -d "$DEPLOY_DIR/.git" ]; then
  echo "已存在仓库，git pull..."
  cd "$DEPLOY_DIR" && git pull --rebase || git pull
else
  echo "克隆仓库..."
  mkdir -p /opt && git clone "$REPO_URL" "$DEPLOY_DIR"
  cd "$DEPLOY_DIR"
fi

# 运行部署脚本
if [ ! -f "$BACKEND_DIR/run-on-server.sh" ]; then
  echo "错误: 未找到 $BACKEND_DIR/run-on-server.sh"
  exit 1
fi
cd "$BACKEND_DIR"
chmod +x run-on-server.sh
exec ./run-on-server.sh
REMOTE_SCRIPT

echo ""
echo "=== 远程命令已执行完毕 ==="
