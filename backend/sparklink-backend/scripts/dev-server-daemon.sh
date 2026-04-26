#!/usr/bin/env bash
# 在本机以「可脱离终端」方式启动 sparklink-backend（nohup + 日志 + PID 文件）
# Agent 在 Cursor 里代跑仍无法保证长驻，请在你自己的终端执行本脚本。
# 用法（在 sparklink-backend 目录下）:
#   chmod +x scripts/dev-server-daemon.sh
#   ./scripts/dev-server-daemon.sh start|stop|status|logs
# 日志: .local/dev-server.log ；PID: .local/dev-server.pid

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mkdir -p .local
PID_FILE="${ROOT}/.local/dev-server.pid"
LOG_FILE="${ROOT}/.local/dev-server.log"
PORT="${DEV_SERVER_PORT:-8080}"

is_running() {
  if [[ ! -f "$PID_FILE" ]]; then
    return 1
  fi
  local pid
  pid="$(cat "$PID_FILE" 2>/dev/null || true)"
  if [[ -z "$pid" ]]; then
    return 1
  fi
  if kill -0 "$pid" 2>/dev/null; then
    return 0
  fi
  return 1
}

cmd_start() {
  if is_running; then
    echo "已在运行: PID $(cat "$PID_FILE")，端口 ${PORT}。需重启请先: $0 stop"
    exit 0
  fi
  if lsof -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "端口 ${PORT} 已被其它进程占用，请结束占用或设 DEV_SERVER_PORT=其它端口 并改 application.yml"
    exit 1
  fi
  echo "==> 启动中（日志: $LOG_FILE）"
  # shellcheck disable=SC2046
  nohup mvn -q spring-boot:run -DskipTests >>"$LOG_FILE" 2>&1 &
  echo $! >"$PID_FILE"
  echo "PID $(cat "$PID_FILE") 已写入 $PID_FILE"
  echo "稍等片刻后: $0 status  或  tail -f $LOG_FILE"
}

cmd_stop() {
  if ! is_running; then
    rm -f "$PID_FILE"
    echo "未在运行（或 PID 已失效，已清理 pid 文件）"
    exit 0
  fi
  local pid
  pid="$(cat "$PID_FILE")"
  echo "==> 停止 PID $pid"
  kill "$pid" 2>/dev/null || true
  # 给 JVM 时间退出
  for _ in 1 2 3 4 5 6 7 8 9 10; do
    if kill -0 "$pid" 2>/dev/null; then sleep 0.5; else break; fi
  done
  if kill -0 "$pid" 2>/dev/null; then
    echo "仍存活，执行 kill -9 $pid"
    kill -9 "$pid" 2>/dev/null || true
  fi
  rm -f "$PID_FILE"
  echo "已停止"
}

cmd_status() {
  if is_running; then
    echo "运行中: PID $(cat "$PID_FILE")"
    lsof -iTCP:"$PORT" -sTCP:LISTEN 2>/dev/null || true
  else
    echo "未运行（$PID_FILE 无有效进程）"
    rm -f "$PID_FILE"
  fi
}

cmd_logs() {
  exec tail -f "$LOG_FILE"
}

case "${1:-}" in
  start)  cmd_start ;;
  stop)   cmd_stop ;;
  status) cmd_status ;;
  logs)   cmd_logs ;;
  *)
    echo "用法: $0 start|stop|status|logs"
    exit 1
    ;;
esac
