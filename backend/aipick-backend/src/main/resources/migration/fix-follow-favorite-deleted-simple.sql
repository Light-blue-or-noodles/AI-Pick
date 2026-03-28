-- 修复 t_follow 和 t_favorite 表缺少 deleted 字段的问题（简化版）
USE aipick;

-- 为 t_follow 表添加 deleted 字段
ALTER TABLE t_follow ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '删除标记0-正常1-删除';

-- 为 t_favorite 表添加 deleted 字段
ALTER TABLE t_favorite ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '删除标记0-正常1-删除';
