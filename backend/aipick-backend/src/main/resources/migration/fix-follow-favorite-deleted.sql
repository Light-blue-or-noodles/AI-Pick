-- 修复 t_follow 和 t_favorite 表缺少 deleted 字段的问题
USE aipick;

-- 为 t_follow 表添加 deleted 字段（如果不存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.columns
               WHERE table_schema = 'aipick' AND table_name = 't_follow' AND column_name = 'deleted');
SET @sql := IF(@exist = 0,
               'ALTER TABLE t_follow ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT ''删除标记 0-正常 1-删除''',
               'SELECT ''deleted column already exists in t_follow''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 为 t_favorite 表添加 deleted 字段（如果不存在）
SET @exist2 := (SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'aipick' AND table_name = 't_favorite' AND column_name = 'deleted');
SET @sql2 := IF(@exist2 = 0,
                'ALTER TABLE t_favorite ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT ''删除标记 0-正常 1-删除''',
                'SELECT ''deleted column already exists in t_favorite''');
PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;
