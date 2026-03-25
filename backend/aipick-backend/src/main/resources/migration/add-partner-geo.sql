-- 搭子表：地图选点坐标（GCJ-02），与 init.sql 新装库一致；存量库可单独执行
ALTER TABLE t_partner ADD COLUMN latitude DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度 GCJ-02' AFTER location;
ALTER TABLE t_partner ADD COLUMN longitude DECIMAL(11, 7) DEFAULT NULL COMMENT '经度 GCJ-02' AFTER latitude;
