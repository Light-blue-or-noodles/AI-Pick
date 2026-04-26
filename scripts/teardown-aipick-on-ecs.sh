#!/bin/bash
# 在「阿里云 ECS」上执行：停止 aipick 服务、清掉本项目的 Docker MySQL/Redis 与持久化数据目录（不可恢复，除非有备份）
# 用法见：部署/DEPLOY-FULL-RESET-v2.md
#
#   chmod +x teardown-aipick-on-ecs.sh
#   ./teardown-aipick-on-ecs.sh [后端目录] --i-understand-data-loss
#
# 若不传目录，会依次尝试：/opt/OpenClaw/backend/aipick-backend、/opt/aipick-backend、/opt/AI-Pick/backend/aipick-backend

set -euo pipefail

BACKEND_DIR=""
CONFIRM=""

# 支持: ./teardown-aipick-on-ecs.sh --i-understand-data-loss
#   或: ./teardown-aipick-on-ecs.sh /opt/.../aipick-backend --i-understand-data-loss
for arg in "$@"; do
  if [ "$arg" = "--i-understand-data-loss" ]; then
    CONFIRM="yes"
  elif [ -z "$BACKEND_DIR" ] && [ -d "$arg" ]; then
    BACKEND_DIR="$arg"
  fi
done

resolve_backend_dir() {
  if [ -n "$BACKEND_DIR" ] && [ -d "$BACKEND_DIR" ] && [ -f "$BACKEND_DIR/docker-compose.prod.yml" ]; then
    return 0
  fi
  for d in /opt/OpenClaw/backend/aipick-backend /opt/aipick-backend /opt/AI-Pick/backend/aipick-backend; do
    if [ -f "$d/docker-compose.prod.yml" ]; then
      BACKEND_DIR="$d"
      return 0
    fi
  done
  return 1
}

if [ "$CONFIRM" != "yes" ]; then
  echo "本脚本会：停止并禁用 systemd 服务 aipick、停止并删除 aipick-mysql / aipick-redis 容器、删除后端目录下 data/mysql 与 data/redis 中的数据。"
  echo "若需保留数据库，请先 mysqldump 备份。"
  echo ""
  echo "确认请执行："
  echo "  $0 /path/to/aipick-backend --i-understand-data-loss"
  echo "  或（自动探测 /opt 下常见路径）："
  echo "  $0 --i-understand-data-loss"
  exit 1
fi

if ! resolve_backend_dir; then
  echo "未找到含 docker-compose.prod.yml 的后端目录。请显式指定，例如："
  echo "  $0 /opt/OpenClaw/backend/aipick-backend --i-understand-data-loss"
  exit 1
fi

echo "=== 将清空部署（目录: $BACKEND_DIR）==="

# 1) systemd
if systemctl is-active --quiet aipick 2>/dev/null; then
  systemctl stop aipick
fi
if systemctl is-enabled --quiet aipick 2>/dev/null; then
  systemctl disable aipick
fi
if [ -f /etc/systemd/system/aipick.service ]; then
  rm -f /etc/systemd/system/aipick.service
  systemctl daemon-reload
  echo "已移除 /etc/systemd/system/aipick.service"
fi

# 2) Docker Compose
cd "$BACKEND_DIR"
if command -v docker &>/dev/null; then
  if docker compose version &>/dev/null; then
    docker compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true
  elif command -v docker-compose &>/dev/null; then
    docker-compose -f docker-compose.prod.yml down --remove-orphans 2>/dev/null || true
  fi
  # 与项目命名一致的容器，若仍残留则删除
  docker rm -f aipick-mysql aipick-redis 2>/dev/null || true
fi

# 3) 宿主机上的持久化目录（由 compose 挂到 data/mysql、data/redis）
if [ -d "$BACKEND_DIR/data/mysql" ] || [ -d "$BACKEND_DIR/data/redis" ]; then
  rm -rf "$BACKEND_DIR/data/mysql" "$BACKEND_DIR/data/redis"
  echo "已删除 $BACKEND_DIR/data/mysql 与 data/redis"
fi

# 4) 旧 jar（可选，避免误跑旧包；不删 .env）
if [ -f "$BACKEND_DIR/target/aipick-backend-1.0.0.jar" ]; then
  rm -f "$BACKEND_DIR/target/aipick-backend-1.0.0.jar"
  echo "已删除 $BACKEND_DIR/target/aipick-backend-1.0.0.jar"
fi

echo ""
echo "=== 清理完成。下一步请按 部署/DEPLOY-FULL-RESET-v2.md 重新 run-on-server / 打 jar / 建 systemd / 若需要再跑表结构同步 ==="
echo "提示：.env 仍保留在 $BACKEND_DIR/.env ，如需改密码请自行编辑后重建容器。"
