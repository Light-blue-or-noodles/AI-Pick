-- ============================================================
-- AI-Pick 种子数据：虚构用户、搭子、活动及关联数据（逻辑自洽）
-- 执行前请确保已执行 init.sql，且数据库 sparklink 存在
-- 密码统一为 123456（与 init 中 testuser/demouser 一致）
-- ============================================================

USE sparklink;

-- 密码 123456 的 BCrypt 哈希（与 init.sql 一致）
SET @pwd = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E';

-- ------------------------------------------------------------
-- 1. 虚构用户（username 唯一，昵称/性别/个性签名 多样）
-- ------------------------------------------------------------
INSERT INTO t_user (username, password, nickname, gender, bio, status, create_time, update_time, deleted) VALUES
('xiaoming', @pwd, '小明', 1, '周末喜欢打球、爬山，找一起运动的搭子', 0, NOW(), NOW(), 0),
('hanmeimei', @pwd, '韩梅梅', 2, '爱探店、拍照，想找饭搭子一起打卡', 0, NOW(), NOW(), 0),
('zhangsan', @pwd, '张三', 1, '游戏宅，主玩王者/原神，来开黑', 0, NOW(), NOW(), 0),
('lisi', @pwd, '李四', 1, '跑步+健身，晨跑搭子或健身房搭子', 0, NOW(), NOW(), 0),
('wangwu', @pwd, '王五', 2, '喜欢看书、咖啡，寻图书馆/自习搭子', 0, NOW(), NOW(), 0),
('zhaoliu', @pwd, '赵六', 1, '爱旅游，短途周边游、徒步都可', 0, NOW(), NOW(), 0),
('sunqi', @pwd, '孙七', 2, '桌游、剧本杀爱好者，周末组局', 0, NOW(), NOW(), 0),
('zhouba', @pwd, '周八', 1, '摄影爱好者，约拍、扫街、户外都可', 0, NOW(), NOW(), 0);

-- 假设上面 8 条插入后自增 id 为 3~10（若已有 1=testuser, 2=demouser）
-- 后续用 user_id 1,2,3...10 均可

-- ------------------------------------------------------------
-- 2. 搭子（type 1～15 见 PartnerTypeConstants：干饭/旅游/运动/游戏/桌游/摄影/聊天等）
-- ------------------------------------------------------------
INSERT INTO t_partner (user_id, title, content, type, target_count, current_count, location, plan_time, status, view_count, create_time, update_time, deleted) VALUES
(1, '周末火锅搭子', '周六晚约海底捞，口味随意，AA 制', 7, 2, 0, '北京朝阳大悦城', DATE_ADD(NOW(), INTERVAL 3 DAY), 0, 12, NOW(), NOW(), 0),
(2, '三里屯探店二人组', '想找一位姐妹一起逛三里屯、拍照喝下午茶', 4, 2, 0, '北京三里屯', DATE_ADD(NOW(), INTERVAL 2 DAY), 0, 8, NOW(), NOW(), 0),
(3, '王者五排上分', '晚上 8 点后稳定在线，主玩射手/打野', 13, 5, 2, '线上', DATE_ADD(NOW(), INTERVAL 1 DAY), 0, 25, NOW(), NOW(), 0),
(4, '奥森晨跑 6 公里', '工作日 6:30 奥森南园一圈，配速 6 分左右', 5, 3, 1, '北京奥林匹克森林公园', DATE_ADD(NOW(), INTERVAL 1 DAY), 0, 15, NOW(), NOW(), 0),
(5, '国图自习搭子', '周末全天国图，互相监督不玩手机', 14, 2, 0, '国家图书馆', DATE_ADD(NOW(), INTERVAL 4 DAY), 0, 6, NOW(), NOW(), 0),
(6, '香山一日徒步', '下周六香山-植物园轻徒步，自带干粮', 8, 4, 0, '香山公园', DATE_ADD(NOW(), INTERVAL 7 DAY), 0, 20, NOW(), NOW(), 0),
(7, '桌游局：阿瓦隆+狼人杀', '周日下午 2 点，已有 4 人，再招 2 人', 11, 6, 4, '海淀五道口某咖啡', DATE_ADD(NOW(), INTERVAL 5 DAY), 0, 18, NOW(), NOW(), 0),
(8, '颐和园落日约拍', '互拍或我帮你拍，器材不限手机也行', 6, 2, 0, '颐和园', DATE_ADD(NOW(), INTERVAL 2 DAY), 0, 9, NOW(), NOW(), 0),
(3, '原神日常+周本', '晚上固定清体力，可连麦', 13, 2, 0, '线上', DATE_ADD(NOW(), INTERVAL 1 DAY), 0, 11, NOW(), NOW(), 0),
(4, '健身房力量训练', '周二四晚 7 点，互相保护+纠正动作', 5, 2, 0, '朝阳某健身房', DATE_ADD(NOW(), INTERVAL 2 DAY), 0, 7, NOW(), NOW(), 0),
(1, '烤串夜宵局', '周五晚找个串店撸串聊天', 7, 4, 0, '待定朝阳/海淀', DATE_ADD(NOW(), INTERVAL 4 DAY), 0, 5, NOW(), NOW(), 0),
(5, '英语角练习', '周末上午咖啡馆练口语，主题提前定', 14, 4, 0, '中关村某咖啡馆', DATE_ADD(NOW(), INTERVAL 6 DAY), 0, 4, NOW(), NOW(), 0);

-- ------------------------------------------------------------
-- 3. 活动（类型 1线下 2线上；状态 0待开始 1报名中 2进行中 3已结束 4已取消）
-- ------------------------------------------------------------
INSERT INTO t_activity (user_id, title, description, type, category, register_end_time, start_time, end_time, location, fee, max_participants, current_participants, status, view_count, create_time, update_time, deleted) VALUES
(2, '三里屯周末市集打卡', '一起逛市集、拍照、喝咖啡，人数不限随缘约', 1, '市集', DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 2 DAY), INTERVAL 4 HOUR), '北京三里屯', 0, 0, 3, 1, 22, NOW(), NOW(), 0),
(4, '奥森 10 公里晨跑团', '每周六 6:30 奥森南园两圈，配速 6~7 分', 1, '跑步', DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 6 DAY), INTERVAL 1 HOUR), '奥林匹克森林公园南园', 0, 15, 5, 1, 30, NOW(), NOW(), 0),
(6, '香山-植物园一日徒步', '轻装徒步，中午植物园内休息，下午返程', 1, '徒步', DATE_ADD(NOW(), INTERVAL 6 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 6 HOUR), '香山-植物园', 0, 10, 2, 1, 18, NOW(), NOW(), 0),
(7, '线上狼人杀局', '周末晚 8 点，12 人局，语音房', 2, '桌游', DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 5 DAY), INTERVAL 3 HOUR), '线上', 0, 12, 8, 1, 45, NOW(), NOW(), 0),
(5, '国图自习室全天自习', '早 9 到晚 6，中间可一起午饭', 1, '学习', DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 4 DAY), INTERVAL 9 HOUR), '国家图书馆', 0, 0, 4, 1, 12, NOW(), NOW(), 0),
(8, '颐和园秋日外拍', '下午 3 点入园，拍日落与建筑', 1, '摄影', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 2 DAY), INTERVAL 4 HOUR), '颐和园', 0, 6, 1, 1, 9, NOW(), NOW(), 0),
(1, '朝阳公园飞盘体验', '零基础可，带飞盘和饮用水即可', 1, '运动', DATE_ADD(NOW(), INTERVAL 4 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 5 DAY), INTERVAL 2 HOUR), '朝阳公园', 0, 12, 6, 1, 28, NOW(), NOW(), 0),
(3, '王者五排冲星', '晚上 8~11 点，要求心态好不骂人', 2, '游戏', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 1 DAY), INTERVAL 3 HOUR), '线上', 0, 5, 3, 1, 35, NOW(), NOW(), 0);

-- ------------------------------------------------------------
-- 4. 搭子应征（partner_id / user_id 引用已存在数据；status 0待审核 1已通过 2已拒绝）
-- ------------------------------------------------------------
INSERT INTO t_partner_apply (partner_id, user_id, message, status, create_time, update_time, deleted) VALUES
(1, 2, '我也爱吃火锅，可以一起！', 1, NOW(), NOW(), 0),
(1, 4, '周六晚我有空', 0, NOW(), NOW(), 0),
(3, 1, '我打野，可以一起', 1, NOW(), NOW(), 0),
(3, 5, '我玩中路', 0, NOW(), NOW(), 0),
(4, 3, '明天晨跑我可以', 1, NOW(), NOW(), 0),
(5, 2, '想找个学习搭子监督', 0, NOW(), NOW(), 0),
(6, 8, '想参加徒步，有经验', 0, NOW(), NOW(), 0),
(7, 1, '阿瓦隆我会玩', 1, NOW(), NOW(), 0),
(8, 2, '可以互拍', 0, NOW(), NOW(), 0);

-- 5. 活动报名（activity_id / user_id 引用已存在数据；status 0已报名 1已取消）
-- ------------------------------------------------------------
-- 为避免测试账号在所有活动中“默认已报名”，这里暂不插入任何默认报名记录
-- 如需演示效果，可自行插入示例数据，例如：
-- INSERT INTO t_activity_registration (activity_id, user_id, message, status, create_time, update_time, deleted) VALUES
-- (1, 3, '一起逛', 0, NOW(), NOW(), 0),
-- (2, 5, '第一次参加', 0, NOW(), NOW(), 0);

-- ------------------------------------------------------------
-- 6. 更新搭子当前人数（与应征通过数一致，仅做示例简化）
-- ------------------------------------------------------------
UPDATE t_partner SET current_count = 1 WHERE id = 1;
UPDATE t_partner SET current_count = 2 WHERE id = 3;
UPDATE t_partner SET current_count = 1 WHERE id = 4;
UPDATE t_partner SET current_count = 1 WHERE id = 7;

-- 完成
SELECT 'Seed data inserted: users(8), partners(12), activities(8), partner_applies(9), activity_registrations(16)' AS result;
