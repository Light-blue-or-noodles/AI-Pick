#!/bin/bash
# 在 ECS 上执行：MySQL/Redis 已启动后，打包 jar、配置 systemd、Nginx
# 用法: 先确保 MySQL/Redis 已启动 (docker-compose -f docker-compose.prod.yml up -d)，再运行本脚本
# 在 ECS 上: cd /opt/AI-Pick/backend/aipick-backend && docker-compose -f docker-compose.prod.yml up -d
# 等待约 1～2 分钟后: bash /opt/AI-Pick/scripts/complete-deploy-on-ecs.sh

set -e
BACKEND_DIR="/opt/AI-Pick/backend/aipick-backend"
cd "$BACKEND_DIR"

echo "=== 1. 检查 MySQL/Redis ==="
docker-compose -f docker-compose.prod.yml ps || true
echo ""

echo "=== 2. 打包后端 jar ==="
# 使用 Java 17 打包（Spring Boot 3 / maven-compiler 需要）
if [ -d /usr/lib/jvm/java-17-openjdk-17.0.18.0.8-1.0.2.1.al8.x86_64 ]; then
  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-17.0.18.0.8-1.0.2.1.al8.x86_64
elif [ -d /usr/lib/jvm/java-17 ]; then
  export JAVA_HOME=/usr/lib/jvm/java-17
fi
[ -n "$JAVA_HOME" ] && export PATH=$JAVA_HOME/bin:$PATH
mvn -q package -DskipTests -B
ls -la target/*.jar
echo ""

echo "=== 3. 配置 systemd ==="
tee /etc/systemd/system/aipick.service << EOF
[Unit]
Description=AI-Pick Backend
After=network.target docker.service

[Service]
Type=simple
User=root
WorkingDirectory=$BACKEND_DIR
EnvironmentFile=$BACKEND_DIR/.env
ExecStart=/usr/bin/java -jar $BACKEND_DIR/target/aipick-backend-1.0.0.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable aipick
systemctl start aipick
sleep 3
systemctl status aipick --no-pager
echo ""

echo "=== 4. 配置 Nginx（若已安装）==="
if command -v nginx &>/dev/null; then
  tee /etc/nginx/conf.d/aipick.conf << 'NGXEOF'
server {
  listen 80;
  server_name _;
  root /var/www/sparklink-h5;
  index index.html;
  client_max_body_size 6m;
  location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
  location / {
    try_files $uri $uri/ /index.html;
  }
}
NGXEOF
  mkdir -p /var/www/sparklink-h5
  echo "H5 静态目录已创建: /var/www/sparklink-h5（请用 scripts/deploy-h5.sh 上传 dist）"
  nginx -t && systemctl restart nginx && echo "Nginx 已重启"
else
  echo "未安装 Nginx，跳过。可用: yum install -y nginx 或 apt install -y nginx"
fi
echo ""
echo "=== 部署完成。验证: curl http://localhost:8080/api/ 或 http://公网IP/api/ ==="
