-- 用户常驻/当前位置（与匹配度、推荐等使用），格式：城市名或 "纬度,经度" GCJ-02
-- 在本地或 ECS 上执行前请确认列不存在：SHOW COLUMNS FROM t_user LIKE 'location';

SET @db := DATABASE();
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'location'
);
SET @sql := IF(@col_exists = 0,
  'ALTER TABLE t_user ADD COLUMN location VARCHAR(200) DEFAULT NULL COMMENT ''常驻/当前位置(城市或lat,lon)'' AFTER tags',
  'SELECT ''column location already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
