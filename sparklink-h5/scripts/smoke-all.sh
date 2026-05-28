#!/usr/bin/env bash
# H5 一键冒烟：构建 + API（可选）+ 预览路由
# 用法:
#   ./scripts/smoke-all.sh                    # 仅 build + 启动 preview + 路由
#   ./scripts/smoke-all.sh --api prod         # 加线上 API（需有效账号）
#   ./scripts/smoke-all.sh --api local        # 加本地 API（需后端+MySQL+test-login）

set -e
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

API_MODE=""
for arg in "$@"; do
  case "$arg" in
    --api=*) API_MODE="${arg#--api=}" ;;
    --api) API_MODE="prod" ;;
    --api-prod) API_MODE="prod" ;;
    --api-local) API_MODE="local" ;;
  esac
done

echo ">>> [1/4] npm run build"
npm run build

echo ">>> [2/4] 启动 vite preview（后台）"
if lsof -i :4173 >/dev/null 2>&1; then
  echo "端口 4173 已占用，复用现有服务"
else
  npm run preview -- --host 127.0.0.1 --port 4173 >/tmp/h5-preview.log 2>&1 &
  PREVIEW_PID=$!
  trap 'kill $PREVIEW_PID 2>/dev/null || true' EXIT
  for i in $(seq 1 30); do
    if curl -sS -o /dev/null "http://127.0.0.1:4173/" 2>/dev/null; then
      break
    fi
    sleep 0.5
  done
fi

echo ">>> [3/4] UI 路由冒烟"
chmod +x "$ROOT/scripts/smoke-ui-routes.sh"
"$ROOT/scripts/smoke-ui-routes.sh" "http://127.0.0.1:4173"

if [ -n "$API_MODE" ]; then
  echo ">>> [4/4] API 冒烟 ($API_MODE)"
  chmod +x "$ROOT/scripts/smoke-api.sh"
  if [ "$API_MODE" = "local" ]; then
    H5_USE_TEST_LOGIN=1 "$ROOT/scripts/smoke-api.sh" "http://127.0.0.1:8080"
  else
    "$ROOT/scripts/smoke-api.sh" "https://www.aipick.cloud"
  fi
else
  echo ">>> [4/4] 跳过 API（加 --api-local 或 --api-prod）"
fi

echo ""
echo "=== 全部自动化冒烟通过（API 未测时需本地后端或生产账号）==="
