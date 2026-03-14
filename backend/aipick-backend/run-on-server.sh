#!/bin/bash
# 在 ECS 上首次部署时运行：安装环境、启动 MySQL/Redis，并提示后续步骤
# 用法：chmod +x run-on-server.sh && ./run-on-server.sh

set -e
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

echo "=== AI-Pick 后端部署脚本 (目录: $DEPLOY_DIR) ==="
echo ""

# 检测系统
if [ -f /etc/redhat-release ]; then
  PKG="yum"
  INSTALL_CMD="yum install -y"
  JAVA_PKG="java-17-openjdk-devel"
  MAVEN_PKG="maven"
elif [ -f /etc/debian_version ]; then
  PKG="apt"
  INSTALL_CMD="apt update && apt install -y"
  JAVA_PKG="openjdk-17-jdk"
  MAVEN_PKG="maven"
else
  echo "未识别的系统，请手动安装 Docker / JDK17 / Maven / Nginx 后重新运行。"
  exit 1
fi

# 安装 Docker
if ! command -v docker &>/dev/null; then
  echo "[1/5] 安装 Docker..."
  curl -fsSL https://get.docker.com | bash -s docker
  systemctl enable docker
  systemctl start docker
else
  echo "[1/5] Docker 已安装，跳过"
fi

# 安装 Docker Compose
if ! command -v docker-compose &>/dev/null; then
  echo "[2/5] 安装 Docker Compose..."
  curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
  chmod +x /usr/local/bin/docker-compose
else
  echo "[2/5] Docker Compose 已安装，跳过"
fi

# 安装 JDK17、Maven、Git、Nginx
for cmd in java mvn git nginx; do
  if ! command -v $cmd &>/dev/null; then
    echo "[3/5] 安装 JDK17 / Maven / Git / Nginx..."
    if [ "$PKG" = "yum" ]; then
      yum install -y git $JAVA_PKG $MAVEN_PKG nginx
    else
      apt update && apt install -y git $JAVA_PKG $MAVEN_PKG nginx
    fi
    break
  fi
done
echo "[3/5] JDK/Maven/Git/Nginx 检查完成"

# 创建 .env
if [ ! -f "$DEPLOY_DIR/.env" ]; then
  echo ""
  echo "[4/5] 未检测到 .env，已生成模板，请编辑后重新运行本脚本。"
  cat > "$DEPLOY_DIR/.env" << 'ENVEOF'
DB_PASSWORD=请改为强密码
JWT_SECRET=请改为至少32位随机字符串
DASHSCOPE_API_KEY=你的百炼API-Key
WECHAT_APPID=
WECHAT_SECRET=
ENVEOF
  echo "  已写入: $DEPLOY_DIR/.env"
  echo "  编辑: vim $DEPLOY_DIR/.env 或 nano $DEPLOY_DIR/.env"
  exit 0
fi

echo "[4/5] .env 已存在"

# 启动 MySQL、Redis
echo "[5/5] 启动 MySQL 与 Redis..."
mkdir -p "$DEPLOY_DIR/data/mysql" "$DEPLOY_DIR/data/redis"
docker-compose -f docker-compose.prod.yml up -d
echo "  等待 MySQL 就绪..."
sleep 10
docker-compose -f docker-compose.prod.yml ps

mkdir -p /var/log/aipick

echo ""
echo "=== 中间件已就绪。请按以下步骤完成后端与 Nginx ==="
echo ""
echo "1) 打包并放置 jar（二选一）："
echo "   本机在项目 backend/aipick-backend 下执行: mvn clean package -DskipTests"
echo "   然后: scp target/aipick-backend-1.0.0.jar root@此服务器IP:/opt/aipick-backend/target/"
echo "   或在本机打包后上传 jar 到: $DEPLOY_DIR/target/aipick-backend-1.0.0.jar"
echo ""
echo "2) 配置 systemd 并启动后端："
echo "   sudo tee /etc/systemd/system/aipick.service << EOF"
echo "[Unit]"
echo "Description=AI-Pick Backend"
echo "After=network.target docker.service"
echo "[Service]"
echo "Type=simple"
echo "User=root"
echo "WorkingDirectory=$DEPLOY_DIR"
echo "EnvironmentFile=$DEPLOY_DIR/.env"
echo "ExecStart=/usr/bin/java -jar $DEPLOY_DIR/target/aipick-backend-1.0.0.jar --spring.profiles.active=prod"
echo "Restart=always"
echo "RestartSec=10"
echo "[Install]"
echo "WantedBy=multi-user.target"
echo "EOF"
echo "   sudo systemctl daemon-reload && sudo systemctl start aipick && sudo systemctl enable aipick"
echo ""
echo "3) 配置 Nginx 反向代理（可选）："
echo "   sudo tee /etc/nginx/conf.d/aipick.conf << 'NGXEOF'"
echo "server { listen 80; server_name _; location /api/ { proxy_pass http://127.0.0.1:8080/api/; proxy_set_header Host \$host; proxy_set_header X-Real-IP \$remote_addr; } }"
echo "NGXEOF"
echo "   sudo nginx -t && sudo systemctl restart nginx"
echo ""
echo "完成上述步骤后，可用: curl http://localhost:8080/api/ 或 http://公网IP/api/ 验证。"
