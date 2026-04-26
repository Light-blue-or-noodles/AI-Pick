# 种子数据说明

## 方式一：直接执行 SQL（推荐，最快）

**前提**：已执行过 `init.sql`（库表存在，且存在测试用户 id 1、2）。

```bash
# MySQL 命令行（按需改 -u / -p / -h）
mysql -u root -p -h 127.0.0.1 aipick < src/main/resources/seed-data.sql
```

或在 MySQL 客户端中：

```sql
source /path/to/backend/aipick-backend/src/main/resources/seed-data.sql;
```

### 数据概览

| 表 | 条数 | 说明 |
|----|------|------|
| t_user | 8 | 虚构用户：小明、韩梅梅、张三等，密码均为 `123456` |
| t_partner | 12 | 搭子：吃饭/旅游/运动/学习/游戏/其他，多地点、多时间 |
| t_activity | 8 | 活动：市集、晨跑、徒步、桌游、自习、摄影、飞盘、五排等 |
| t_partner_apply | 9 | 应征记录，部分已通过 |
| t_activity_registration | 16 | 活动报名记录 |

- 搭子类型：1 吃饭 2 旅游 3 运动 4 学习 5 游戏 6 其他  
- 活动类型：1 线下 2 线上  
- 用户 id 1、2 来自 `init.sql`（testuser、demouser），3～10 来自本种子  
- 所有时间使用 `NOW()` 与 `DATE_ADD`，相对当前时间，逻辑自洽  

### 若重复执行

- 用户使用 `username` 唯一，重复执行会报重复键错误，可先删除种子用户再执行，或只执行一次。

---

## 方式二：通过接口创建（与业务一致）

若希望通过「注册 + 发布搭子/活动」接口造数，可用脚本（需 `curl`、`jq`）模拟：

1. 注册多个用户  
2. 登录拿 token，带 `X-User-Id` 调用  
3. `POST /api/partner`、`POST /api/activity` 创建搭子与活动  

**通过 API 造数**：执行项目内脚本（需后端已启动）：

```bash
cd backend/aipick-backend
chmod +x scripts/seed-via-api.sh
./scripts/seed-via-api.sh
```

会注册用户 `seed_xiaoming`、`seed_hanmeimei` 等（密码 `123456`），并用其发布若干搭子与活动。  
接口定义见：`README.md` 中「发布搭子」「发布活动」接口说明。

---

## 登录测试账号（种子用户）

| 用户名   | 密码   | 昵称   |
|----------|--------|--------|
| testuser | 123456 | 测试用户 |
| demouser | 123456 | 演示用户 |
| xiaoming | 123456 | 小明 |
| hanmeimei | 123456 | 韩梅梅 |
| zhangsan | 123456 | 张三 |
| lisi     | 123456 | 李四 |
| wangwu   | 123456 | 王五 |
| zhaoliu  | 123456 | 赵六 |
| sunqi    | 123456 | 孙七 |
| zhouba   | 123456 | 周八 |

以上账号均可用于小程序「账号密码登录」或后端接口调试。
