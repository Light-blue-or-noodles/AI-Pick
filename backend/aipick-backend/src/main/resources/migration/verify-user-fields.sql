-- 验证用户表是否包含 company_name 和 school_name 字段
-- 使用方法：mysql -u root -p < verify-user-fields.sql

USE aipick;

-- 检查表结构
SELECT '=== t_user 表结构 ===' AS info;
DESCRIBE t_user;

-- 检查是否包含 company_name 字段
SELECT '=== 检查 company_name 字段 ===' AS info;
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'aipick' 
  AND TABLE_NAME = 't_user' 
  AND COLUMN_NAME = 'company_name';

-- 检查是否包含 school_name 字段
SELECT '=== 检查 school_name 字段 ===' AS info;
SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'aipick' 
  AND TABLE_NAME = 't_user' 
  AND COLUMN_NAME = 'school_name';

-- 如果字段不存在，执行以下 SQL 添加
-- ALTER TABLE t_user ADD COLUMN company_name VARCHAR(100) DEFAULT NULL COMMENT '公司名称' AFTER openid;
-- ALTER TABLE t_user ADD COLUMN school_name VARCHAR(100) DEFAULT NULL COMMENT '学校名称' AFTER company_name;

-- 查询示例数据
SELECT '=== 示例用户数据 ===' AS info;
SELECT id, username, nickname, company_name, school_name, create_time 
FROM t_user 
LIMIT 5;
