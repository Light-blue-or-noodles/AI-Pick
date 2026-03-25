#!/bin/bash
# 增量更新 ECS 上的 AI-Pick 服务（包括代码和数据库变更）
# 用法：./update-ecs.sh <ECS公网IP>
# 示例：./update-ecs.sh 59.110.0.107

set -e
ECS_IP="${1:-59.110.0.107}"
REPO_URL="https://github.com/Light-blue-or-noodles/AI-Pick.git"
BRANCH="feature/pick-0.0.1"

echo "=== AI-Pick 增量更新到 ECS ($ECS_IP) ==="
echo "分支: $BRANCH"
echo ""

ssh -o StrictHostKeyChecking=accept-new root@"$ECS_IP" "bash -s" << REMOTE_SCRIPT
set -e
REPO_URL="https://github.com/Light-blue-or-noodles/AI-Pick.git"
DEPLOY_DIR="/opt/AI-Pick"
BACKEND_DIR="\$DEPLOY_DIR/backend/aipick-backend"

echo "[1/6] 更新代码..."
cd "\$DEPLOY_DIR"
git fetch origin
# 强制使用远程版本，丢弃本地修改
git reset --hard origin/$BRANCH 2>/dev/null || (git checkout -f $BRANCH 2>/dev/null || git checkout -b $BRANCH origin/$BRANCH)
git pull origin $BRANCH

echo "[2/6] 检查 MySQL 变更..."
# 检查 migration 目录是否有新文件
if [ -d "\$BACKEND_DIR/src/main/resources/migration" ]; then
  echo "发现 migration 文件，准备执行数据库变更..."
  cd "\$BACKEND_DIR"
  
  # 获取 MySQL 密码
  DB_PASSWORD=\$(grep DB_PASSWORD .env 2>/dev/null | cut -d'=' -f2 || echo "")
  if [ -z "\$DB_PASSWORD" ]; then
    echo "警告: 无法获取数据库密码，跳过数据库变更"
  else
    # 执行所有 migration SQL 文件
    for sql_file in src/main/resources/migration/*.sql; do
      if [ -f "\$sql_file" ]; then
        echo "  执行: \$sql_file"
        docker exec -i aipick-mysql mysql -uroot -p"\$DB_PASSWORD" aipick < "\$sql_file" 2>/dev/null || echo "    跳过（可能已执行过）"
      fi
    done
  fi
else
  echo "无 migration 文件，跳过数据库变更"
fi

echo "[3/6] 停止旧服务..."
systemctl stop aipick 2>/dev/null || true
sleep 2

echo "[4/6] 重新打包后端..."
cd "\$BACKEND_DIR"
mvn clean package -DskipTests -q

echo "[5/6] 启动新服务..."
mkdir -p "\$BACKEND_DIR/target"
cp target/aipick-backend-*.jar "\$BACKEND_DIR/target/aipick-backend-1.0.0.jar"
systemctl start aipick
sleep 5

echo "[6/6] 验证服务状态..."
if curl -s http://localhost:8080/api/health | grep -q "UP"; then
  echo "✅ 服务启动成功，健康检查通过"
else
  echo "⚠️ 服务可能未完全启动，请稍后手动检查"
fi

echo ""
echo "=== 更新完成 ==="
echo "版本: $BRANCH"
echo "部署目录: \$DEPLOY_DIR"
echo "API 地址: http://\$ECS_IP/api/"
REMOTE_SCRIPT

echo ""
echo "=== 远程更新执行完毕 ==="
