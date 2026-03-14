#!/bin/bash
# 数据库迁移脚本 - 添加 company_name 和 school_name 字段

echo "执行数据库迁移：添加公司名称和学校名称字段..."

# 使用 JDBC 执行 SQL (通过 Java)
cd ~/AI/project/OpenClaw/backend/aipick-backend

# 创建临时 SQL 文件
cat > /tmp/migration.sql << 'EOF'
USE aipick;
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS company_name VARCHAR(100) DEFAULT NULL COMMENT '公司名称' AFTER openid;
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS school_name VARCHAR(100) DEFAULT NULL COMMENT '学校名称' AFTER company_name;
SELECT 'Migration completed successfully!' as status;
EOF

# 尝试使用 mysql 命令执行
if command -v mysql &> /dev/null; then
    mysql -u root -proot < /tmp/migration.sql
else
    echo "MySQL 客户端未安装，请手动执行以下 SQL:"
    echo ""
    cat /tmp/migration.sql
    echo ""
    echo "或者使用数据库管理工具（如 Sequel Pro, DBeaver, MySQL Workbench）执行上述 SQL"
fi
