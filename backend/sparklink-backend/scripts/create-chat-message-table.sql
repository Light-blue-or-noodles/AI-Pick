-- 创建用户间私信消息表
CREATE TABLE IF NOT EXISTS t_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT NOT NULL COMMENT '发送者ID',
  receiver_id BIGINT NOT NULL COMMENT '接收者ID',
  content VARCHAR(500) NOT NULL COMMENT '消息内容',
  is_read TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);

-- 添加索引优化查询性能
CREATE INDEX idx_sender_id ON t_chat_message(sender_id);
CREATE INDEX idx_receiver_id ON t_chat_message(receiver_id);
CREATE INDEX idx_sender_receiver ON t_chat_message(sender_id, receiver_id);
CREATE INDEX idx_receiver_is_read ON t_chat_message(receiver_id, is_read);
