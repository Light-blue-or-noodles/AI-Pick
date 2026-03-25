-- ============================================================
-- 本地 → 阿里云 ECS 数据库结构同步脚本（幂等，可重复执行）
-- ============================================================
-- 用途：把本机已执行过的 MySQL 变更，在 ECS 上执行一遍，使云端与本地表结构一致。
-- 执行方式：在 ECS 上连接 MySQL 后执行本文件，例如：
--   mysql -h 127.0.0.1 -P 3306 -u root -p aipick < sync-schema-to-ecs.sql
-- 若某列已存在会跳过或报 Duplicate column（可忽略），不会破坏数据。
-- ============================================================

USE aipick;

-- ------------------------------------------------------------
-- 1. 用户表：company_name / school_name（幂等，兼容 MySQL 5.7/8.x）
-- ------------------------------------------------------------
DELIMITER //
DROP PROCEDURE IF EXISTS add_user_company_school_columns//
CREATE PROCEDURE add_user_company_school_columns()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'company_name') = 0 THEN
    ALTER TABLE t_user ADD COLUMN company_name VARCHAR(100) DEFAULT NULL COMMENT '公司名称' AFTER openid;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'school_name') = 0 THEN
    ALTER TABLE t_user ADD COLUMN school_name VARCHAR(100) DEFAULT NULL COMMENT '学校名称' AFTER company_name;
  END IF;
END//
DELIMITER ;
CALL add_user_company_school_columns();
DROP PROCEDURE IF EXISTS add_user_company_school_columns;

-- ------------------------------------------------------------
-- 2. 活动表：images（多图 JSON，首张为封面）
-- ------------------------------------------------------------
DELIMITER //
DROP PROCEDURE IF EXISTS add_activity_images_column//
CREATE PROCEDURE add_activity_images_column()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 't_activity'
        AND COLUMN_NAME = 'images') = 0 THEN
    ALTER TABLE t_activity
    ADD COLUMN images TEXT DEFAULT NULL COMMENT '多图JSON数组，首张为封面';
  END IF;
END//
DELIMITER ;
CALL add_activity_images_column();
DROP PROCEDURE IF EXISTS add_activity_images_column;

-- ------------------------------------------------------------
-- 3. 用户表：birthday、tags（编辑资料保存生日与兴趣标签）
-- ------------------------------------------------------------
DELIMITER //
DROP PROCEDURE IF EXISTS add_user_birthday_tags_columns//
CREATE PROCEDURE add_user_birthday_tags_columns()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'birthday') = 0 THEN
    ALTER TABLE t_user ADD COLUMN birthday VARCHAR(20) DEFAULT NULL COMMENT '生日 yyyy-MM-dd' AFTER school_name;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'tags') = 0 THEN
    ALTER TABLE t_user ADD COLUMN tags VARCHAR(500) DEFAULT NULL COMMENT '兴趣标签 JSON 数组' AFTER birthday;
  END IF;
END//
DELIMITER ;
CALL add_user_birthday_tags_columns();
DROP PROCEDURE IF EXISTS add_user_birthday_tags_columns;

-- ------------------------------------------------------------
-- 校验（可选）：查看当前表结构
-- ------------------------------------------------------------
-- DESCRIBE t_user;
-- DESCRIBE t_activity;
