-- aipick 数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS aipick DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE aipick;

-- 用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    gender TINYINT DEFAULT 0 COMMENT '性别 0-未知 1-男 2-女',
    bio VARCHAR(500) DEFAULT NULL COMMENT '个性签名',
    status TINYINT DEFAULT 0 COMMENT '状态 0-正常 1-禁用',
    openid VARCHAR(100) DEFAULT NULL COMMENT '微信openid',
    company_name VARCHAR(100) DEFAULT NULL COMMENT '公司名称',
    school_name VARCHAR(100) DEFAULT NULL COMMENT '学校名称',
    birthday VARCHAR(20) DEFAULT NULL COMMENT '生日 yyyy-MM-dd',
    tags VARCHAR(500) DEFAULT NULL COMMENT '兴趣标签 JSON 数组',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 搭子表
CREATE TABLE IF NOT EXISTS t_partner (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    title VARCHAR(100) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容描述',
    preference VARCHAR(512) DEFAULT NULL COMMENT '搭子偏好，逗号分隔标签',
    scope TINYINT NOT NULL DEFAULT 1 COMMENT '可见范围 1公开 2同事 3校友',
    type TINYINT NOT NULL COMMENT '搭子类型 1-15 见 PartnerTypeConstants',
    target_count INT DEFAULT 2 COMMENT '目标人数',
    current_count INT DEFAULT 0 COMMENT '当前人数',
    location VARCHAR(200) DEFAULT NULL COMMENT '活动地点展示文案',
    latitude DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度 GCJ-02',
    longitude DECIMAL(11, 7) DEFAULT NULL COMMENT '经度 GCJ-02',
    plan_time DATETIME DEFAULT NULL COMMENT '计划/集合时间',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图片',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待应征 1-已满 2-已结束',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子表';

-- 搭子应征表
CREATE TABLE IF NOT EXISTS t_partner_apply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    partner_id BIGINT NOT NULL COMMENT '搭子ID',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    message VARCHAR(500) DEFAULT NULL COMMENT '申请留言',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待审核 1-已通过 2-已拒绝',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_partner_id (partner_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搭子应征表';

-- 活动表
CREATE TABLE IF NOT EXISTS t_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    title VARCHAR(100) NOT NULL COMMENT '活动标题',
    description TEXT NOT NULL COMMENT '活动描述',
    type TINYINT NOT NULL COMMENT '活动类型 1-线下 2-线上',
    category VARCHAR(50) DEFAULT NULL COMMENT '分类',
    register_end_time DATETIME DEFAULT NULL COMMENT '报名截止时间',
    start_time DATETIME DEFAULT NULL COMMENT '活动开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '活动结束时间',
    location VARCHAR(200) DEFAULT NULL COMMENT '活动地点',
    latitude DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
    longitude DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
    fee DECIMAL(10, 2) DEFAULT 0 COMMENT '费用 0-免费',
    max_participants INT DEFAULT 0 COMMENT '人数上限 0-不限',
    current_participants INT DEFAULT 0 COMMENT '当前报名人数',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图片',
    status TINYINT DEFAULT 0 COMMENT '状态 0-待开始 1-报名中 2-进行中 3-已结束 4-已取消',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- 活动报名表
CREATE TABLE IF NOT EXISTS t_activity_registration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '报名人ID',
    message VARCHAR(500) DEFAULT NULL COMMENT '报名留言',
    status TINYINT DEFAULT 0 COMMENT '状态 0-已报名 1-已取消',
    check_in_time DATETIME DEFAULT NULL COMMENT '签到时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_activity_id (activity_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名表';

-- AI对话消息表
CREATE TABLE IF NOT EXISTS t_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    session_id VARCHAR(50) NOT NULL COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '消息类型 1-用户 2-AI',
    content TEXT NOT NULL COMMENT '消息内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息表';

-- 用户会话表（用于私信）
CREATE TABLE IF NOT EXISTS t_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id1 BIGINT NOT NULL COMMENT '用户1 ID',
    user_id2 BIGINT NOT NULL COMMENT '用户2 ID',
    last_message_id BIGINT DEFAULT NULL COMMENT '最后一条消息ID',
    unread_count1 INT DEFAULT 0 COMMENT '未读数量（用户1）',
    unread_count2 INT DEFAULT 0 COMMENT '未读数量（用户2）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_user_id1 (user_id1),
    INDEX idx_user_id2 (user_id2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户会话表';

-- 用户私信消息表
CREATE TABLE IF NOT EXISTS t_user_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    type VARCHAR(20) DEFAULT 'text' COMMENT '消息类型 text/image/location',
    content TEXT NOT NULL COMMENT '消息内容',
    latitude DECIMAL(10, 7) DEFAULT NULL COMMENT '纬度',
    longitude DECIMAL(10, 7) DEFAULT NULL COMMENT '经度',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户私信消息表';

-- 活动提醒表
CREATE TABLE IF NOT EXISTS t_activity_remind (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    activity_id BIGINT NOT NULL COMMENT '活动ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    remind_time DATETIME NOT NULL COMMENT '提醒时间',
    is_reminded TINYINT DEFAULT 0 COMMENT '是否已提醒 0-未提醒 1-已提醒',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0-正常 1-删除',
    INDEX idx_activity_id (activity_id),
    INDEX idx_user_id (user_id),
    INDEX idx_remind_time (remind_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动提醒表';

-- 插入测试用户 (密码: 123456)
INSERT INTO t_user (username, password, nickname, avatar, status) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '测试用户', NULL, 0),
('demouser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E', '演示用户', NULL, 0);