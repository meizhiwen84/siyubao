#!/bin/bash
set -euo pipefail

source /etc/profile >/dev/null 2>&1 || true

PROJECT_DIR="${PROJECT_DIR:-/opt/siyubao-admin}"
REPO_URL="${REPO_URL:-}"
BRANCH="${BRANCH:-master}"
PORT="${PORT:-6943}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
LOG_FILE="${LOG_FILE:-$PROJECT_DIR/app.log}"

mkdir -p "$PROJECT_DIR"
cd "$PROJECT_DIR"

if [ ! -d ".git" ]; then
  if [ -z "$REPO_URL" ]; then
    echo "REPO_URL 为空，无法克隆源码（例如：export REPO_URL=git@github.com:xxx/yyy.git）"
    exit 1
  fi
  echo "===== 克隆源码 ====="
  git clone -b "$BRANCH" "$REPO_URL" .
else
  echo "===== 拉取远程最新代码 ====="
  git fetch --all
  git reset --hard "origin/$BRANCH"
fi

echo "===== 开始打包 admin-app（跳过测试） ====="
./mvnw -pl admin-app -am clean package -Dmaven.test.skip=true -Dmaven.compiler.heapSize=1024m

JAR_PATH="$(ls -1 admin-app/target/*.jar 2>/dev/null | grep -v 'original-' | head -n 1 || true)"
if [ -z "$JAR_PATH" ]; then
  echo "未找到 admin-app 的 jar 包，请检查打包结果：$PROJECT_DIR/admin-app/target"
  exit 1
fi

echo "===== 停止旧服务（端口：$PORT） ====="
OLD_PID="$(lsof -ti tcp:"$PORT" 2>/dev/null || true)"
if [ -z "$OLD_PID" ] && command -v netstat >/dev/null 2>&1; then
  OLD_PID="$(netstat -nlp 2>/dev/null | grep ":$PORT " | awk '{print $7}' | awk -F '/' '{print $1}' | head -n 1 || true)"
fi
if [ -n "$OLD_PID" ]; then
  kill -9 "$OLD_PID" || true
  echo "已杀死旧进程：$OLD_PID"
else
  echo "没有运行中的旧服务"
fi

echo "===== 启动新服务 ====="
nohup java $JAVA_OPTS -jar "$JAR_PATH" --server.port="$PORT" > "$LOG_FILE" 2>&1 &

sleep 3
NEW_PID="$(lsof -ti tcp:"$PORT" 2>/dev/null || true)"
if [ -n "$NEW_PID" ]; then
  echo "部署成功！新进程ID：$NEW_PID"
  echo "日志查看：tail -f $LOG_FILE"
else
  echo "部署失败，请查看日志：$LOG_FILE"
  exit 1
fi

