-- 搭子：偏好、可见范围（公开/同事/校友）
ALTER TABLE t_partner ADD COLUMN preference VARCHAR(100) DEFAULT NULL COMMENT '搭子偏好' AFTER content;
ALTER TABLE t_partner ADD COLUMN scope TINYINT NOT NULL DEFAULT 1 COMMENT '可见范围 1公开 2同事 3校友' AFTER preference;
