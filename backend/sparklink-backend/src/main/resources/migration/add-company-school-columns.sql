-- 添加公司名称和学校名称字段到用户表
-- 执行时间：2026-03-10
-- 说明：修复用户信息接口返回缺少 companyName 和 schoolName 字段的问题

USE sparklink;

-- 添加 company_name 字段
ALTER TABLE t_user 
ADD COLUMN IF NOT EXISTS company_name VARCHAR(100) DEFAULT NULL COMMENT '公司名称' AFTER openid;

-- 添加 school_name 字段
ALTER TABLE t_user 
ADD COLUMN IF NOT EXISTS school_name VARCHAR(100) DEFAULT NULL COMMENT '学校名称' AFTER company_name;

-- 验证添加结果
DESCRIBE t_user;
