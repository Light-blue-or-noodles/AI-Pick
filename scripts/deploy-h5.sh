#!/bin/bash
# 构建 sparklink-h5 并部署到阿里云 ECS（Nginx 静态目录）
# 用法: ./scripts/deploy-h5.sh [ECS_HOST] [REMOTE_DIR]
# 示例: ./scripts/deploy-h5.sh root@59.110.0.107 /var/www/sparklink-h5

set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
H5_DIR="$ROOT/sparklink-h5"
HOST="${1:-root@59.110.0.107}"
REMOTE_DIR="${2:-/var/www/sparklink-h5}"

echo "=== 构建 H5 ==="
cd "$H5_DIR"
npm ci
npm run build
echo ""

echo "=== 上传到 $HOST:$REMOTE_DIR ==="
ssh "$HOST" "mkdir -p $REMOTE_DIR"
rsync -avz --delete "$H5_DIR/dist/" "$HOST:$REMOTE_DIR/"
echo ""

echo "=== 完成 ==="
echo "请确保 Nginx 已配置 root $REMOTE_DIR 与 /api/ 反代（见 部署/nginx/sparklink-h5.conf）"
echo "验证: curl -I http://<域名>/  与  curl https://<域名>/api/health"
