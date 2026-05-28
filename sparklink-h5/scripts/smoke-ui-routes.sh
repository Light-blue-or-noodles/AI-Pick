#!/usr/bin/env bash
# H5 静态路由冒烟：对已 build 的 dist 或 preview 服务检查关键路径返回 HTML
# 用法: ./scripts/smoke-ui-routes.sh [BASE_URL]
# 默认 http://127.0.0.1:4173 （需先 npm run preview）

set -e
BASE="${1:-http://127.0.0.1:4173}"
BASE="${BASE%/}"

routes=(
  "/"
  "/login"
  "/agreement/user"
  "/agreement/privacy"
  "/home"
  "/partner"
  "/partner/filter"
  "/ai-chat"
)

fail=0
echo "=== UI route smoke: $BASE ==="
for r in "${routes[@]}"; do
  code=$(curl -sS -o /tmp/h5-route.html -w "%{http_code}" "$BASE$r" || echo "000")
  if [ "$code" != "200" ]; then
    echo "FAIL $r -> HTTP $code"
    fail=1
  else
    if ! grep -q 'id="app"' /tmp/h5-route.html 2>/dev/null; then
      echo "FAIL $r -> no #app shell"
      fail=1
    else
      echo "OK   $r -> HTTP 200 (#app)"
    fi
  fi
done

if [ "$fail" -ne 0 ]; then
  exit 1
fi
echo "=== UI route smoke 完成 ==="
