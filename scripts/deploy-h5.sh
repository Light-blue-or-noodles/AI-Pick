#!/bin/bash
# 构建 sparklink-h5 并部署到阿里云 ECS（Nginx 静态目录）
#
# 推荐全量部署使用: ./scripts/deploy-prod.sh
# 本脚本仅 H5 时使用。
#
# 用法: ./scripts/deploy-h5.sh [ECS_HOST] [REMOTE_DIR]
# 示例: ./scripts/deploy-h5.sh root@59.110.0.107 /var/www/sparklink-h5

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
H5_DIR="$ROOT/sparklink-h5"
HOST="${1:-root@59.110.0.107}"
REMOTE_DIR="${2:-/var/www/sparklink-h5}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519}"

RSYNC_SSH="ssh -i $SSH_KEY -o StrictHostKeyChecking=no"

echo "=== 构建 H5 ==="
cd "$H5_DIR"
npm ci
npm run build
echo ""

echo "=== 上传到 $HOST:$REMOTE_DIR ==="
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$HOST" "mkdir -p $REMOTE_DIR"
if ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$HOST" "command -v rsync >/dev/null 2>&1"; then
  rsync -avz -e "$RSYNC_SSH" --delete "$H5_DIR/dist/" "$HOST:$REMOTE_DIR/"
else
  echo "⚠️  服务器未安装 rsync，改用 tar 上传"
  tar czf - -C "$H5_DIR/dist" . | ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$HOST" \
    "rm -rf $REMOTE_DIR/* && tar xzf - -C $REMOTE_DIR"
fi
echo ""

echo "=== 完成 ==="
echo "Nginx 需配置 root $REMOTE_DIR 与 /api/ 反代（见 部署/nginx/sparklink-h5.conf）"
echo "或运行全量部署: ./scripts/deploy-prod.sh"
echo "验证: curl -I https://www.aipick.cloud/  &&  curl https://www.aipick.cloud/api/health"
