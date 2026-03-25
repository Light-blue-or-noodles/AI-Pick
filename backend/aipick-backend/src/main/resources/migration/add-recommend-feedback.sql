-- AI-Pick 推荐反馈表
-- 用于记录用户对推荐的反馈（跳过/聊聊），持续优化推荐算法

CREATE TABLE IF NOT EXISTS `t_recommend_feedback` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `feedback_type` TINYINT NOT NULL COMMENT '反馈类型：1-跳过 2-聊聊 3-举报',
    `target_type` TINYINT NOT NULL COMMENT '目标类型：1-搭子 2-活动',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `match_score` INT COMMENT '推荐时的匹配度分数',
    `feedback_time` DATETIME COMMENT '反馈时间',
    `extra` JSON COMMENT '扩展字段',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_target` (`target_type`, `target_id`),
    INDEX `idx_feedback_time` (`feedback_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推荐反馈表';