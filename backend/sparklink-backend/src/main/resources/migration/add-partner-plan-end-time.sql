-- 搭子活动结束时间（与 plan_time 成对展示时间范围，可为空）
ALTER TABLE t_partner
    ADD COLUMN plan_end_time DATETIME DEFAULT NULL COMMENT '计划/活动结束时间' AFTER plan_time;
