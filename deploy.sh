#!/bin/bash
# SpringBoot项目自动部署脚本

# 项目路径（服务器上的项目目录）
PROJECT_DIR="/opt/your-project"
# JAR包名称（根据实际打包结果修改，可在target目录查看）
JAR_NAME="demo-0.0.1-SNAPSHOT.jar"
# 服务端口（用于查找并关闭旧进程）
PORT=8080

# 进入项目目录
cd $PROJECT_DIR || { echo "项目目录不存在"; exit 1; }

# 1. 拉取最新代码
echo "===== 拉取远程最新代码 ====="
git pull origin main
if [ $? -ne 0 ]; then
  echo "拉取代码失败，退出部署"
  exit 1
fi

# 2. 打包项目（跳过测试）
echo "===== 开始打包项目 ====="
mvn clean package -Dmaven.test.skip=true
if [ $? -ne 0 ]; then
  echo "打包失败，退出部署"
  exit 1
fi

# 3. 停止当前运行的服务
echo "===== 停止旧服务 ====="
# 查找占用指定端口的进程ID
OLD_PID=$(netstat -nlp | grep :$PORT | awk '{print $7}' | awk -F '/' '{print $1}')
if [ -n "$OLD_PID" ]; then
  kill -9 $OLD_PID
  echo "已杀死旧进程：$OLD_PID"
else
  echo "没有运行中的旧服务"
fi

# 4. 启动新服务（后台运行并输出日志）
echo "===== 启动新服务 ====="
nohup java -jar target/$JAR_NAME --server.port=$PORT > app.log 2>&1 &

# 检查启动是否成功
sleep 3
NEW_PID=$(netstat -nlp | grep :$PORT | awk '{print $7}' | awk -F '/' '{print $1}')
if [ -n "$NEW_PID" ]; then
  echo "部署成功！新进程ID：$NEW_PID"
  echo "日志查看：tail -f $PROJECT_DIR/app.log"
else
  echo "部署失败，请查看日志：$PROJECT_DIR/app.log"
fi
    