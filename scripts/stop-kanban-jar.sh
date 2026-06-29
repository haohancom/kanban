#!/usr/bin/env bash
set -euo pipefail

JAR_PATH="${1:-backend/target/kanban-0.0.1-SNAPSHOT.jar}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
JAR_FILE="$PROJECT_ROOT/$JAR_PATH"
JAR_NAME="$(basename "$JAR_PATH")"

LOG_DIR="$(dirname "$JAR_FILE")/logs"
PID_FILE="$LOG_DIR/kanban-runtime.pid"

stopped_any=0

if [[ -f "$PID_FILE" ]]; then
  PID="$(cat "$PID_FILE")"
  if [[ -n "$PID" ]] && ps -p "$PID" > /dev/null 2>&1; then
    kill "$PID" || true
    echo "已发送停止信号: PID=$PID"
    stopped_any=1
  fi
  rm -f "$PID_FILE"
fi

if [[ -f "$JAR_FILE" ]]; then
  pids="$(ps -eo pid=,args= | awk -v jar="$JAR_NAME" '$0 ~ /java/ && $0 ~ jar {print $1}')"
  if [[ -n "$pids" ]]; then
    while IFS= read -r pid; do
      if [[ "$pid" != "${PID:-}" ]]; then
        kill "$pid" || true
        echo "已发送停止信号: PID=$pid"
        stopped_any=1
      fi
    done <<< "$pids"
  fi
fi

if [[ $stopped_any -eq 1 ]]; then
  echo "停止完成"
else
  echo "未检测到运行中的项目进程"
fi
