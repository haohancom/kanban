#!/usr/bin/env bash
set -euo pipefail

JAR_PATH="${1:-backend/target/kanban-0.0.1-SNAPSHOT.jar}"
SERVER_ADDRESS="${2:-0.0.0.0}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR_FILE="$PROJECT_ROOT/$JAR_PATH"

if [[ ! -f "$JAR_FILE" ]]; then
  echo "未找到 jar 文件: $JAR_FILE"
  exit 1
fi

JAR_DIR="$(cd "$(dirname "$JAR_FILE")" && pwd)"
LOG_DIR="$JAR_DIR/logs"
mkdir -p "$LOG_DIR"

PID_FILE="$LOG_DIR/kanban-runtime.pid"
OUT_LOG="$LOG_DIR/kanban-runtime.log"
ERR_LOG="$LOG_DIR/kanban-runtime-error.log"

if [[ -f "$PID_FILE" ]]; then
  EXISTING_PID="$(cat "$PID_FILE")"
  if [[ -n "$EXISTING_PID" ]] && ps -p "$EXISTING_PID" > /dev/null 2>&1; then
    echo "检测到已有服务进程(PID=$EXISTING_PID)在运行，请先执行 stop-kanban-jar.sh"
    exit 1
  fi
  rm -f "$PID_FILE"
fi

nohup java -Dserver.address="$SERVER_ADDRESS" -jar "$JAR_FILE" \
  > "$OUT_LOG" 2> "$ERR_LOG" < /dev/null &
echo "$!" > "$PID_FILE"

echo "启动成功，PID=$!"
echo "访问地址: http://$SERVER_ADDRESS:8080"
echo "日志文件: $OUT_LOG"
echo "错误日志: $ERR_LOG"
