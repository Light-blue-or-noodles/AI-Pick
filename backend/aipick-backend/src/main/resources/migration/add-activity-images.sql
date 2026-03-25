-- 活动多图：首张为封面，其余在详情展示
ALTER TABLE t_activity ADD COLUMN images TEXT DEFAULT NULL COMMENT '多图JSON数组，首张为封面' AFTER cover_image;
