#!/bin/bash
# SparkLink 生产一键部署：后端 jar + H5 静态 + Nginx + systemd(sparklink)
#
# 用法:
#   ./scripts/deploy-prod.sh [prod]              # 全量部署
#   ./scripts/deploy-prod.sh prod --backend-only # 仅后端
#   ./scripts/deploy-prod.sh prod --h5-only        # 仅 H5 + Nginx
#   ./scripts/deploy-prod.sh prod --skip-nginx     # 跳过 Nginx 同步
#
# 服务器约定:
#   JAR:     /opt/sparklink/app.jar
#   环境变量: /opt/sparklink/.env
#   服务名:   sparklink (systemd)
#   H5 目录:  /var/www/sparklink-h5

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$ROOT/backend/sparklink-backend"
H5_DIR="$ROOT/sparklink-h5"

SERVER_IP="${SERVER_IP:-59.110.0.107}"
SERVER_USER="${SERVER_USER:-root}"
SSH_KEY="${SSH_KEY:-$HOME/.ssh/id_ed25519}"
SSH_TARGET="${SERVER_USER}@${SERVER_IP}"

REMOTE_DIR="/opt/sparklink"
H5_REMOTE_DIR="/var/www/sparklink-h5"
SERVICE_NAME="sparklink"
LEGACY_SERVICE="aipick"

ENV="${1:-prod}"
shift || true

DEPLOY_BACKEND=1
DEPLOY_H5=1
SYNC_NGINX=1

while [ $# -gt 0 ]; do
  case "$1" in
    --backend-only)
      DEPLOY_H5=0
      SYNC_NGINX=0
      ;;
    --h5-only)
      DEPLOY_BACKEND=0
      ;;
    --skip-nginx)
      SYNC_NGINX=0
      ;;
    *)
      echo "未知参数: $1"
      exit 1
      ;;
  esac
  shift
done

SSH_OPTS=(-i "$SSH_KEY" -o StrictHostKeyChecking=no)
SCP_OPTS=(-i "$SSH_KEY" -o StrictHostKeyChecking=no)

remote() {
  ssh "${SSH_OPTS[@]}" "$SSH_TARGET" "$@"
}

echo "🚀 SparkLink 生产部署 [profile=$ENV, backend=$DEPLOY_BACKEND, h5=$DEPLOY_H5, nginx=$SYNC_NGINX]"
echo "=========================================="

if [ ! -f "$SSH_KEY" ]; then
  echo "❌ SSH 密钥不存在: $SSH_KEY"
  exit 1
fi

# ---------------------------------------------------------------------------
# 后端
# ---------------------------------------------------------------------------
if [ "$DEPLOY_BACKEND" -eq 1 ]; then
  echo ""
  echo "📦 [1/4] 打包后端..."
  if ! command -v mvn >/dev/null 2>&1; then
    echo "❌ 未安装 Maven"
    exit 1
  fi
  export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null || true)}"
  cd "$BACKEND_DIR"
  mvn clean package -DskipTests -q
  if [ ! -f "target/sparklink-backend-1.0.0.jar" ]; then
    echo "❌ 打包失败: target/sparklink-backend-1.0.0.jar 不存在"
    exit 1
  fi
  echo "✅ 后端打包完成"

  echo ""
  echo "📤 [2/4] 上传 jar 并配置 systemd(sparklink)..."
  remote "mkdir -p $REMOTE_DIR /var/log/sparklink"

  # 首次部署：从 legacy aipick 迁移 .env
  remote "bash -s" <<REMOTE_ENV
set -e
if [ ! -f $REMOTE_DIR/.env ]; then
  if [ -f /opt/aipick/.env ]; then
    cp /opt/aipick/.env $REMOTE_DIR/.env
    chmod 600 $REMOTE_DIR/.env
    echo '✅ 已从 /opt/aipick/.env 迁移到 $REMOTE_DIR/.env'
  else
    echo '⚠️  $REMOTE_DIR/.env 不存在，请参考 部署/sparklink.env.example 在服务器创建'
  fi
fi
REMOTE_ENV

  remote "
    if [ -f $REMOTE_DIR/app.jar ]; then
      mv $REMOTE_DIR/app.jar $REMOTE_DIR/app.jar.backup.\$(date +%Y%m%d_%H%M%S)
      echo '✅ 已备份旧 jar'
    fi
  "

  scp "${SCP_OPTS[@]}" \
    "$BACKEND_DIR/target/sparklink-backend-1.0.0.jar" \
    "$SSH_TARGET:$REMOTE_DIR/app.jar"

  scp "${SCP_OPTS[@]}" \
    "$ROOT/部署/sparklink.service" \
    "$SSH_TARGET:/etc/systemd/system/${SERVICE_NAME}.service"

  remote "bash -s" <<REMOTE_SYSTEMD
set -e
# 迁移历史上传目录（aipick -> sparklink）
if [ ! -e $REMOTE_DIR/uploads ] && [ -d /opt/aipick/uploads ]; then
  ln -s /opt/aipick/uploads $REMOTE_DIR/uploads
  echo '✅ 已链接 uploads -> /opt/aipick/uploads'
fi

systemctl daemon-reload
systemctl enable ${SERVICE_NAME}

# 停止 legacy 服务，释放 8080
if systemctl is-active --quiet ${LEGACY_SERVICE} 2>/dev/null; then
  systemctl stop ${LEGACY_SERVICE}
  systemctl disable ${LEGACY_SERVICE} || true
  echo '✅ 已停止 legacy 服务 ${LEGACY_SERVICE}'
fi

systemctl restart ${SERVICE_NAME}
sleep 5

if systemctl is-active --quiet ${SERVICE_NAME}; then
  echo '✅ ${SERVICE_NAME} 服务运行中'
else
  echo '❌ ${SERVICE_NAME} 启动失败'
  journalctl -u ${SERVICE_NAME} -n 30 --no-pager || true
  exit 1
fi
REMOTE_SYSTEMD

  echo "✅ 后端部署完成"
fi

# ---------------------------------------------------------------------------
# H5
# ---------------------------------------------------------------------------
if [ "$DEPLOY_H5" -eq 1 ]; then
  echo ""
  echo "🌐 [3/4] 构建并上传 H5..."
  if ! command -v npm >/dev/null 2>&1; then
    echo "❌ 未安装 npm"
    exit 1
  fi
  cd "$H5_DIR"
  npm ci
  npm run build
  remote "mkdir -p $H5_REMOTE_DIR"
  if remote "command -v rsync >/dev/null 2>&1"; then
    rsync -avz -e "ssh ${SSH_OPTS[*]}" --delete "$H5_DIR/dist/" "$SSH_TARGET:$H5_REMOTE_DIR/"
  else
    echo "⚠️  服务器未安装 rsync，改用 tar 上传"
    tar czf - -C "$H5_DIR/dist" . | ssh "${SSH_OPTS[@]}" "$SSH_TARGET" \
      "rm -rf $H5_REMOTE_DIR/* && tar xzf - -C $H5_REMOTE_DIR"
  fi
  echo "✅ H5 上传完成"
fi

# ---------------------------------------------------------------------------
# Nginx
# ---------------------------------------------------------------------------
if [ "$SYNC_NGINX" -eq 1 ]; then
  echo ""
  echo "🔧 [4/4] 同步 Nginx 配置..."
  scp "${SCP_OPTS[@]}" \
    "$ROOT/部署/nginx/sparklink-h5.conf" \
    "$SSH_TARGET:/etc/nginx/conf.d/sparklink.conf"

  remote "bash -s" <<'REMOTE_NGINX'
set -e
# 禁用 legacy 全量反代配置，避免与 sparklink.conf 冲突
if [ -f /etc/nginx/conf.d/aipick.conf ]; then
  mv /etc/nginx/conf.d/aipick.conf /etc/nginx/conf.d/aipick.conf.disabled.$(date +%Y%m%d_%H%M%S)
  echo '✅ 已禁用 aipick.conf'
fi
nginx -t
systemctl reload nginx
echo '✅ Nginx 已 reload'
REMOTE_NGINX

  echo "✅ Nginx 配置已更新"
fi

# ---------------------------------------------------------------------------
# 验收
# ---------------------------------------------------------------------------
echo ""
echo "📋 健康检查..."
sleep 2

HEALTH_OK=0
if curl -sf "http://${SERVER_IP}:8080/api/health" | grep -q '"UP"'; then
  echo "✅ 本地 API: http://${SERVER_IP}:8080/api/health"
  HEALTH_OK=1
else
  echo "⚠️  本地 API 健康检查未通过"
fi

if curl -sf "https://www.aipick.cloud/api/health" | grep -q '"UP"'; then
  echo "✅ 公网 API: https://www.aipick.cloud/api/health"
  HEALTH_OK=1
fi

if [ "$DEPLOY_H5" -eq 1 ]; then
  H5_CODE=$(curl -s -o /dev/null -w "%{http_code}" "https://www.aipick.cloud/" || true)
  if [ "$H5_CODE" = "200" ]; then
    echo "✅ H5 首页: https://www.aipick.cloud/ (HTTP $H5_CODE)"
  else
    echo "⚠️  H5 首页: https://www.aipick.cloud/ (HTTP $H5_CODE)"
  fi
fi

echo ""
echo "=========================================="
echo "🎉 部署流程结束"
echo ""
echo "JAR:      $REMOTE_DIR/app.jar"
echo "服务:     systemctl status $SERVICE_NAME"
echo "环境变量: $REMOTE_DIR/.env"
echo "H5:       $H5_REMOTE_DIR"
echo "日志:     journalctl -u $SERVICE_NAME -f"
echo "=========================================="

if [ "$DEPLOY_BACKEND" -eq 1 ] && [ "$HEALTH_OK" -eq 0 ]; then
  exit 1
fi
