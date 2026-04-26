#!/usr/bin/env bash
# 本机从 aipick 整库复制到 sparklink：创建目标库后，表结构 + 数据 + 例程一次性导入
# 在 backend/sparklink-backend 目录下执行：chmod +x scripts/migrate-aipick-to-sparklink.sh && ./scripts/migrate-aipick-to-sparklink.sh
# 环境变量：MYSQL_HOST MYSQL_PORT MYSQL_USER MYSQL_PWD（或 DB_PASSWORD，默认与 application.yml 的 root/root 一致）
# 源/目标库名可覆盖：SRC_DB=aipick DST_DB=sparklink

set -euo pipefail

MYSQL_BIN="${MYSQL_BIN:-/Applications/MySQLWorkbench.app/Contents/MacOS/mysql}"
MYSQLDUMP_BIN="${MYSQLDUMP_BIN:-/Applications/MySQLWorkbench.app/Contents/MacOS/mysqldump}"
if [[ ! -x "$MYSQL_BIN" ]]; then
  MYSQL_BIN="mysql"
  MYSQLDUMP_BIN="mysqldump"
fi

HOST="${MYSQL_HOST:-127.0.0.1}"
PORT="${MYSQL_PORT:-3306}"
USER="${MYSQL_USER:-root}"
export MYSQL_PWD="${MYSQL_PWD:-${DB_PASSWORD:-root}}"

SRC="${SRC_DB:-aipick}"
DST="${DST_DB:-sparklink}"

echo "==> 检查源库 ${SRC}"
if ! "$MYSQL_BIN" -h "$HOST" -P "$PORT" -u "$USER" -e "USE \`${SRC}\`;" 2>/dev/null; then
  echo "错误: 源库 '${SRC}' 不存在或无法连接。请检查 MySQL 与 MYSQL_PWD/DB_PASSWORD。"
  exit 1
fi

echo "==> 创建目标库 ${DST}（若已存在会保留，数据将追加/冲突时报错，请先自行处理空库）"
"$MYSQL_BIN" -h "$HOST" -P "$PORT" -u "$USER" -e \
  "CREATE DATABASE IF NOT EXISTS \`${DST}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "==> 导出 ${SRC} 并导入 ${DST}（大库可能需较长时间）"
# mysqldump 单库导出含 USE `源库`；改为 USE `目标库`（反引号须避免被 shell 当命令替换）
USE_FIX='s/USE `'"$SRC"'`/USE `'"$DST"'`/'
"$MYSQLDUMP_BIN" -h "$HOST" -P "$PORT" -u "$USER" \
  --single-transaction --routines --triggers --events --add-drop-table \
  "$SRC" | sed "$USE_FIX" | "$MYSQL_BIN" -h "$HOST" -P "$PORT" -u "$USER" "$DST"

echo "==> 完成。请启动 sparklink-backend 做验证；确认无误后如需删除旧库，在 MySQL 中执行: DROP DATABASE \`${SRC}\`;"
