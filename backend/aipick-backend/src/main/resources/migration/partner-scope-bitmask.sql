-- 搭子可见范围由单选枚举改为位掩码后，历史数据 scope=3（旧「校友」枚举）→ 4（校友位）
UPDATE t_partner SET scope = 4 WHERE scope = 3;
