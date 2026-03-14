#!/usr/bin/env bash
# 通过后端接口构造种子数据：10 个用户、10 个搭子、10 个活动，以及应征/报名关联
# 使用前请确保后端已启动：mvn spring-boot:run
# 用法：./scripts/seed-via-api.sh  或  BASE_URL=http://localhost:8080/api ./scripts/seed-via-api.sh

set -e
BASE_URL="${BASE_URL:-http://localhost:8080/api}"
PWD="123456"

echo "使用 BASE_URL=$BASE_URL"
echo ""

# 注册用户
register() {
  local username=$1
  local password=$2
  local nickname=$3
  curl -s -X POST "$BASE_URL/user/register" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"$password\",\"nickname\":\"$nickname\"}" > /dev/null || true
}

# 登录，输出 JSON
login() {
  local username=$1
  local password=$2
  curl -s -X POST "$BASE_URL/user/login" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"$username\",\"password\":\"$password\"}"
}

# 从 JSON 中取 data.id（最后一个 "id":数字）
parse_id() {
  echo "$1" | grep -o '"id":[0-9]*' | tail -1 | cut -d':' -f2
}

# 发布搭子，返回完整响应以便解析 id
create_partner() {
  local token=$1
  local userId=$2
  local title=$3
  local content=$4
  local type=$5
  local location=$6
  local planTime=$7
  curl -s -X POST "$BASE_URL/partner" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -H "X-User-Id: $userId" \
    -d "{\"title\":\"$title\",\"content\":\"$content\",\"type\":$type,\"targetCount\":2,\"location\":\"$location\",\"planTime\":\"$planTime\"}"
}

# 发布活动，返回完整响应
create_activity() {
  local token=$1
  local userId=$2
  local title=$3
  local description=$4
  local type=$5
  local category=$6
  local location=$7
  local startTime=$8
  local endTime=$9
  curl -s -X POST "$BASE_URL/activity" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -H "X-User-Id: $userId" \
    -d "{\"title\":\"$title\",\"description\":\"$description\",\"type\":$type,\"category\":\"$category\",\"location\":\"$location\",\"startTime\":\"$startTime\",\"endTime\":\"$endTime\",\"fee\":0,\"maxParticipants\":20}"
}

# 应征搭子
apply_partner() {
  local token=$1
  local userId=$2
  local partnerId=$3
  local message=${4:-"想参加"}
  curl -s -X POST "$BASE_URL/partner/$partnerId/apply" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -H "X-User-Id: $userId" \
    -d "{\"message\":\"$message\"}"
}

# 报名活动
join_activity() {
  local token=$1
  local userId=$2
  local activityId=$3
  local message=${4:-""}
  curl -s -X POST "$BASE_URL/activity/$activityId/join" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -H "X-User-Id: $userId" \
    -d "{\"message\":\"$message\"}"
}

# 时间：未来 2 天
START=$(date -u -v+2d +"%Y-%m-%dT10:00:00" 2>/dev/null || date -u -d "+2 days" +"%Y-%m-%dT10:00:00" 2>/dev/null || echo "2026-04-01T10:00:00")
END=$(date -u -v+2d +"%Y-%m-%dT12:00:00" 2>/dev/null || date -u -d "+2 days" +"%Y-%m-%dT12:00:00" 2>/dev/null || echo "2026-04-01T12:00:00")

echo "1. 注册 6 个用户..."
for i in 1 2 3 4 5 6; do
  register "seed_u$i" "$PWD" "种子用户$i"
done
echo "   完成"
echo ""

echo "2. 登录并创建 10 个搭子..."
PARTNER_IDS=()
for i in 1 2 3 4 5; do
  R=$(login "seed_u$i" "$PWD")
  if ! echo "$R" | grep -q '"code":0'; then continue; fi
  TOKEN=$(echo "$R" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  USER_ID=$(echo "$R" | grep -o '"userId":[0-9]*' | cut -d':' -f2)
  [ -z "$TOKEN" ] || [ -z "$USER_ID" ] && continue

  # 每个用户发 2 条搭子
  if [ $i -eq 1 ]; then
    R1=$(create_partner "$TOKEN" "$USER_ID" "周末火锅搭子" "周六晚约海底捞，AA 制" 1 "北京朝阳大悦城" "$START")
    R2=$(create_partner "$TOKEN" "$USER_ID" "奥森晨跑 6 公里" "工作日 6:30 奥森南园，配速 6 分" 3 "奥林匹克森林公园" "$START")
  elif [ $i -eq 2 ]; then
    R1=$(create_partner "$TOKEN" "$USER_ID" "三里屯探店二人组" "一起逛三里屯、拍照下午茶" 1 "北京三里屯" "$START")
    R2=$(create_partner "$TOKEN" "$USER_ID" "王者五排上分" "晚 8 点后稳定在线，主玩射手" 5 "线上" "$START")
  elif [ $i -eq 3 ]; then
    R1=$(create_partner "$TOKEN" "$USER_ID" "国图自习搭子" "周末全天国图互相监督" 4 "国家图书馆" "$START")
    R2=$(create_partner "$TOKEN" "$USER_ID" "香山一日徒步" "下周六香山-植物园轻徒步" 2 "香山公园" "$START")
  elif [ $i -eq 4 ]; then
    R1=$(create_partner "$TOKEN" "$USER_ID" "桌游局阿瓦隆" "周日下午 2 点，再招 2 人" 6 "海淀五道口" "$START")
    R2=$(create_partner "$TOKEN" "$USER_ID" "原神日常周本" "晚上清体力可连麦" 5 "线上" "$START")
  else
    R1=$(create_partner "$TOKEN" "$USER_ID" "健身房力量训练" "周二四晚 7 点互相保护" 3 "朝阳某健身房" "$START")
    R2=$(create_partner "$TOKEN" "$USER_ID" "英语角练习" "周末上午咖啡馆练口语" 4 "中关村咖啡馆" "$START")
  fi
  ID1=$(parse_id "$R1")
  ID2=$(parse_id "$R2")
  [ -n "$ID1" ] && PARTNER_IDS+=("$ID1")
  [ -n "$ID2" ] && PARTNER_IDS+=("$ID2")
done
echo "   搭子 id 列表: ${PARTNER_IDS[*]}"
echo ""

echo "3. 创建 10 个活动..."
ACTIVITY_IDS=()
for i in 1 2 3 4 5; do
  R=$(login "seed_u$i" "$PWD")
  if ! echo "$R" | grep -q '"code":0'; then continue; fi
  TOKEN=$(echo "$R" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  USER_ID=$(echo "$R" | grep -o '"userId":[0-9]*' | cut -d':' -f2)
  [ -z "$TOKEN" ] || [ -z "$USER_ID" ] && continue

  if [ $i -eq 1 ]; then
    R1=$(create_activity "$TOKEN" "$USER_ID" "朝阳公园飞盘体验" "零基础可，带飞盘即可" 1 "运动" "朝阳公园" "$START" "$END")
    R2=$(create_activity "$TOKEN" "$USER_ID" "奥森 10 公里晨跑团" "每周六 6:30 奥森南园两圈" 1 "跑步" "奥森南园" "$START" "$END")
  elif [ $i -eq 2 ]; then
    R1=$(create_activity "$TOKEN" "$USER_ID" "三里屯周末市集打卡" "逛市集拍照喝咖啡" 1 "市集" "三里屯" "$START" "$END")
    R2=$(create_activity "$TOKEN" "$USER_ID" "线上狼人杀局" "周末晚 8 点 12 人局" 2 "桌游" "线上" "$START" "$END")
  elif [ $i -eq 3 ]; then
    R1=$(create_activity "$TOKEN" "$USER_ID" "香山-植物园一日徒步" "轻装徒步下午返程" 1 "徒步" "香山-植物园" "$START" "$END")
    R2=$(create_activity "$TOKEN" "$USER_ID" "国图自习室全天" "早 9 到晚 6 可一起午饭" 1 "学习" "国家图书馆" "$START" "$END")
  elif [ $i -eq 4 ]; then
    R1=$(create_activity "$TOKEN" "$USER_ID" "颐和园秋日外拍" "下午 3 点入园拍日落" 1 "摄影" "颐和园" "$START" "$END")
    R2=$(create_activity "$TOKEN" "$USER_ID" "王者五排冲星" "晚 8~11 点心态好不骂人" 2 "游戏" "线上" "$START" "$END")
  else
    R1=$(create_activity "$TOKEN" "$USER_ID" "烤串夜宵局" "周五晚撸串聊天" 1 "美食" "待定朝阳" "$START" "$END")
    R2=$(create_activity "$TOKEN" "$USER_ID" "英语角周末场" "上午咖啡馆练口语" 1 "学习" "中关村" "$START" "$END")
  fi
  ID1=$(parse_id "$R1")
  ID2=$(parse_id "$R2")
  [ -n "$ID1" ] && ACTIVITY_IDS+=("$ID1")
  [ -n "$ID2" ] && ACTIVITY_IDS+=("$ID2")
done
echo "   活动 id 列表: ${ACTIVITY_IDS[*]}"
echo ""

echo "4. 应征搭子（其他用户应征前几个搭子）..."
for i in 2 3 4 5 6; do
  R=$(login "seed_u$i" "$PWD")
  if ! echo "$R" | grep -q '"code":0'; then continue; fi
  TOKEN=$(echo "$R" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  USER_ID=$(echo "$R" | grep -o '"userId":[0-9]*' | cut -d':' -f2)
  [ -z "$TOKEN" ] || [ -z "$USER_ID" ] && continue
  for pid in "${PARTNER_IDS[@]:0:5}"; do
    [ -z "$pid" ] && continue
    apply_partner "$TOKEN" "$USER_ID" "$pid" "想参加～" > /dev/null || true
  done
done
echo "   完成"
echo ""

echo "5. 报名活动（多用户报名多场活动）..."
for i in 1 2 3 4 5 6; do
  R=$(login "seed_u$i" "$PWD")
  if ! echo "$R" | grep -q '"code":0'; then continue; fi
  TOKEN=$(echo "$R" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
  USER_ID=$(echo "$R" | grep -o '"userId":[0-9]*' | cut -d':' -f2)
  [ -z "$TOKEN" ] || [ -z "$USER_ID" ] && continue
  for aid in "${ACTIVITY_IDS[@]}"; do
    [ -z "$aid" ] && continue
    join_activity "$TOKEN" "$USER_ID" "$aid" "报名" > /dev/null || true
  done
done
echo "   完成"
echo ""

echo "种子数据（方案 B）已创建："
echo "  - 用户: seed_u1 ~ seed_u6，密码 $PWD"
echo "  - 搭子: 10 条（类型 1 吃饭 2 旅游 3 运动 4 学习 5 游戏 6 其他）"
echo "  - 活动: 10 条（含线下/线上、运动/市集/徒步/桌游等）"
echo "  - 应征: 多用户对前 5 个搭子应征"
echo "  - 报名: 多用户对全部活动报名（重复报名会报错已忽略）"
