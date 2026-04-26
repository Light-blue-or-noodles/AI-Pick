-- 历史数据：plan_time 为空的搭子，用 create_time 作为计划开始时间
-- 执行前可预览：SELECT id, title, create_time, plan_time FROM t_partner WHERE plan_time IS NULL AND IFNULL(deleted, 0) = 0;

UPDATE t_partner
SET plan_time = create_time
WHERE plan_time IS NULL
  AND create_time IS NOT NULL
  AND IFNULL(deleted, 0) = 0;

-- 如不需要跳过已删除行，可改为去掉 AND IFNULL(deleted, 0) = 0
