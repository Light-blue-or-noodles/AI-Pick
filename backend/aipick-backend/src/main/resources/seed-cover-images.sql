-- 为已有活动和搭子记录按内容分配合适的封面图（持久化在数据库中）
-- 运行方式（在 aipick-backend 目录下）：
--   mysql -u root -p your_db < src/main/resources/seed-cover-images.sql
-- 或在 MySQL 客户端 / 运维脚本中执行以下语句

-- 活动：按分类(category)分配对应配图
UPDATE t_activity
SET cover_image = CASE
    WHEN category = '运动' THEN '/static/covers/activity-sport.png'
    WHEN category = '美食' THEN '/static/covers/activity-food.png'
    WHEN category = '学习' THEN '/static/covers/activity-study.png'
    WHEN category = '娱乐' THEN '/static/covers/activity-party.png'
    WHEN category = '社交' THEN '/static/covers/activity-default.png'
    ELSE '/static/covers/activity-default.png'
END
WHERE (cover_image IS NULL OR cover_image = '');

-- 搭子：按类型(type)分配对应配图
-- type: 1～15 见 PartnerTypeConstants
UPDATE t_partner
SET cover_image = CASE
    WHEN type = 1 THEN '/static/covers/partner-food.png'
    WHEN type = 2 THEN '/static/covers/activity-default.png'
    WHEN type = 3 THEN '/static/covers/partner-sport.png'
    WHEN type = 4 THEN '/static/covers/partner-study.png'
    WHEN type = 5 THEN '/static/covers/activity-party.png'
    WHEN type = 6 THEN '/static/covers/partner-default.png'
    ELSE '/static/covers/partner-default.png'
END
WHERE (cover_image IS NULL OR cover_image = '');
