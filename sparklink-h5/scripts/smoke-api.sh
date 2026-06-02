#!/usr/bin/env bash
# H5 本地联调：API 冒烟（不启动浏览器）
# 用法: ./scripts/smoke-api.sh [BASE_URL]
# 默认 BASE_URL 为空时请求 https://www.aipick.cloud；传 http://127.0.0.1:8080 测本地后端

set -e
BASE="${1:-https://www.aipick.cloud}"
BASE="${BASE%/}"
USER="${H5_TEST_USER:-test}"
PASS="${H5_TEST_PASS:-123456}"

echo "=== API base: $BASE ==="

code=$(curl -sS -o /tmp/h5-health.json -w "%{http_code}" "$BASE/api/health")
echo "GET /api/health -> HTTP $code"
if [ "$code" != "200" ]; then
  cat /tmp/h5-health.json 2>/dev/null || true
  exit 1
fi

login=$(curl -sS -X POST "$BASE/api/user/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
echo "$login" | head -c 200
echo ""

token=$(echo "$login" | python3 -c "import sys,json; d=json.load(sys.stdin); print((d.get('data') or {}).get('token',''))" 2>/dev/null || true)
uid=$(echo "$login" | python3 -c "import sys,json; d=json.load(sys.stdin); print((d.get('data') or {}).get('userId',''))" 2>/dev/null || true)

if [ -z "$token" ] || [ -z "$uid" ]; then
  if [ "${H5_USE_TEST_LOGIN:-}" = "1" ]; then
    login=$(curl -sS -X POST "$BASE/api/user/test-login" -H 'Content-Type: application/json' -d '{}')
    token=$(echo "$login" | python3 -c "import sys,json; d=json.load(sys.stdin); print((d.get('data') or {}).get('token',''))" 2>/dev/null || true)
    uid=$(echo "$login" | python3 -c "import sys,json; d=json.load(sys.stdin); print((d.get('data') or {}).get('userId',''))" 2>/dev/null || true)
    echo "使用 test-login"
  fi
fi
if [ -z "$token" ] || [ -z "$uid" ]; then
  echo "登录失败：设置 H5_TEST_USER/H5_TEST_PASS，或本地 H5_USE_TEST_LOGIN=1 ./scripts/smoke-api.sh http://127.0.0.1:8080"
  exit 1
fi
echo "登录成功 userId=$uid"

curl -sS -o /tmp/h5-info.json -w "GET /api/user/info -> HTTP %{http_code}\n" \
  -H "Authorization: Bearer $token" \
  -H "X-User-Id: $uid" \
  "$BASE/api/user/info"

curl -sS -o /tmp/h5-partner.json -w "GET /api/partner -> HTTP %{http_code}\n" \
  -H "Authorization: Bearer $token" \
  -H "X-User-Id: $uid" \
  "$BASE/api/partner?scopeType=platform"

curl -sS -o /tmp/h5-usersig.json -w "GET /api/im/usersig -> HTTP %{http_code}\n" \
  -H "Authorization: Bearer $token" \
  -H "X-User-Id: $uid" \
  "$BASE/api/im/usersig"

chat=$(curl -sS -X POST "$BASE/api/chat" \
  -H "Authorization: Bearer $token" \
  -H 'Content-Type: application/json' \
  -d '{"message":"你好，请简短回复"}')
chat_code=$(echo "$chat" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('code', -1))" 2>/dev/null || echo -1)
echo "POST /api/chat (JWT only, no X-User-Id) -> code $chat_code"
echo "$chat" | head -c 300
echo ""
if [ "$chat_code" != "0" ]; then
  echo "AI 对话失败"
  exit 1
fi

echo "=== API 冒烟完成 ==="
