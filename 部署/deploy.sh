#!/bin/bash
# SparkLink 本地一键部署脚本
# 使用方式: ./deploy.sh [dev|prod]

set -e

# 配置
SERVER_IP="59.110.0.107"
SERVER_USER="root"
SSH_KEY="$HOME/.ssh/id_ed25519"
REMOTE_DIR="/opt/sparklink"
SERVICE_NAME="sparklink"

# 环境判断
ENV=${1:-prod}
echo "🚀 开始部署 SparkLink 后端服务 [环境: $ENV]"
echo "=========================================="

# 1. 检查前置条件
echo "📋 步骤 1/6: 检查前置条件..."
if [ ! -f "$SSH_KEY" ]; then
    echo "❌ 错误: SSH 密钥不存在: $SSH_KEY"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "❌ 错误: Maven 未安装"
    exit 1
fi

if ! command -v java &> /dev/null; then
    echo "❌ 错误: Java 未安装"
    exit 1
fi

echo "✅ 前置条件检查通过"

# 2. 进入项目目录
echo "📋 步骤 2/6: 进入项目目录..."
cd "$(dirname "$0")/backend/sparklink-backend"
echo "✅ 当前目录: $(pwd)"

# 3. 拉取最新代码（可选）
echo "📋 步骤 3/6: 拉取最新代码..."
if [ -d ".git" ]; then
    git pull origin $(git branch --show-current) || echo "⚠️  拉取代码失败，使用本地代码继续"
else
    echo "⚠️  不是 Git 仓库，跳过拉取"
fi

# 4. 打包应用
echo "📋 步骤 4/6: 打包应用..."
export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo "$JAVA_HOME")
mvn clean package -DskipTests -q

if [ ! -f "target/sparklink-backend-1.0.0.jar" ]; then
    echo "❌ 错误: 打包失败，jar 文件不存在"
    exit 1
fi

echo "✅ 打包成功: target/sparklink-backend-1.0.0.jar"

# 5. 备份并上传
echo "📋 步骤 5/6: 备份并上传..."
echo "⏳ 连接服务器 $SERVER_IP..."

# 备份旧版本
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SERVER_USER@$SERVER_IP" "
    if [ -f $REMOTE_DIR/app.jar ]; then
        mv $REMOTE_DIR/app.jar $REMOTE_DIR/app.jar.backup.\$(date +%Y%m%d_%H%M%S)
        echo '✅ 旧版本已备份'
    fi
"

# 上传新版本
echo "⏳ 上传 jar 包..."
scp -i "$SSH_KEY" -o StrictHostKeyChecking=no \
    "target/sparklink-backend-1.0.0.jar" \
    "$SERVER_USER@$SERVER_IP:$REMOTE_DIR/app.jar"

echo "✅ 上传成功"

# 6. 重启服务
echo "📋 步骤 6/6: 重启服务..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no "$SERVER_USER@$SERVER_IP" "
    echo '⏳ 停止旧服务...'
    systemctl stop $SERVICE_NAME 2>/dev/null || pkill -f 'java -jar $REMOTE_DIR/app.jar' 2>/dev/null || true
    sleep 2
    
    echo '⏳ 启动新服务...'
    systemctl start $SERVICE_NAME 2>/dev/null || (
        cd $REMOTE_DIR && 
        export JWT_SECRET=sparklink-secret-key-2026-secure-token &&
        export DB_PASSWORD=root &&
        export TENCENT_IM_SDK_APP_ID=1600133993 &&
        export TENCENT_IM_IDENTIFIER=administrator &&
        export TENCENT_IM_KEY=8806e0cd94d52d2d5a1d6d3f9e47253b8274de8cf262ee66961dcbe154cead64 &&
        nohup java -jar $REMOTE_DIR/app.jar --spring.profiles.active=$ENV > /var/log/sparklink.log 2>&1 &
    )
    
    sleep 5
    
    # 检查服务状态
    if pgrep -f 'java -jar $REMOTE_DIR/app.jar' > /dev/null; then
        echo '✅ 服务启动成功'
    else
        echo '❌ 服务启动失败，查看日志:'
        tail -20 /var/log/sparklink.log 2>/dev/null || echo '日志文件不存在'
        exit 1
    fi
"

# 7. 健康检查
echo "📋 健康检查..."
sleep 3
if curl -s "http://$SERVER_IP:8080/api/health" | grep -q "UP"; then
    echo "✅ 健康检查通过"
else
    echo "⚠️  健康检查未通过，请手动检查"
fi

echo ""
echo "=========================================="
echo "🎉 部署完成!"
echo ""
echo "服务地址: http://$SERVER_IP:8080/api"
echo "健康检查: http://$SERVER_IP:8080/api/health"
echo "日志查看: ssh -i $SSH_KEY $SERVER_USER@$SERVER_IP 'tail -f /var/log/sparklink.log'"
echo "=========================================="