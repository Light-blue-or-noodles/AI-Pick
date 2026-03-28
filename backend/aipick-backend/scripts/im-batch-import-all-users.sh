#!/usr/bin/env bash
# 将 t_user 中未逻辑删除的用户批量导入腾讯云 IM（account_import，可重复执行）。
# 使用前：1) 配置 tencent.im.sdk-app-id、key、identifier；2) 设置与服务端一致的密钥。
#
# 示例：
#   export IM_BATCH_IMPORT_SECRET='your-secret'
#   export BASE_URL='http://127.0.0.1:8080'
#   ./scripts/im-batch-import-all-users.sh

set -euo pipefail
BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
SECRET="${IM_BATCH_IMPORT_SECRET:-}"

if [[ -z "$SECRET" ]]; then
  echo "请设置环境变量 IM_BATCH_IMPORT_SECRET（与 application 中 app.im-batch-import-secret 一致）" >&2
  exit 1
fi

curl -sS -X POST "${BASE_URL}/api/im/admin/batch-import-users" \
  -H "Content-Type: application/json" \
  -H "X-Im-Batch-Secret: ${SECRET}" \
  -d '{}' | python3 -m json.tool 2>/dev/null || cat
