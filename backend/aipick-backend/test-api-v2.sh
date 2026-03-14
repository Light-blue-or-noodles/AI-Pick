#!/bin/bash

# AI-Pick V2.0 接口测试脚本
# 测试 5 个新接口的完整功能

BASE_URL="http://localhost:8080/api"
TEST_USER_ID=1

echo "========================================"
echo "AI-Pick V2.0 接口测试报告"
echo "测试时间：$(date '+%Y-%m-%d %H:%M:%S')"
echo "后端地址：$BASE_URL"
echo "========================================"
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试结果统计
PASS_COUNT=0
FAIL_COUNT=0

# 测试函数
test_api() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    
    echo -e "${YELLOW}测试：$test_name${NC}"
    echo "请求：$method $endpoint"
    
    if [ "$method" == "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    elif [ "$method" == "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -H "X-User-Id: $TEST_USER_ID" \
            -d "$data")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    echo "响应码：$http_code"
    
    if [ "$http_code" == "200" ]; then
        echo -e "${GREEN}✓ 测试通过${NC}"
        ((PASS_COUNT++))
        # 格式化输出 JSON
        echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
    else
        echo -e "${RED}✗ 测试失败${NC}"
        ((FAIL_COUNT++))
        echo "$body"
    fi
    echo ""
}

echo "========================================"
echo "1. AI 首页 + 智能推荐 API 测试"
echo "========================================"
echo ""

# 测试 1.1: 首页推荐（匿名）
test_api "首页推荐 - 匿名访问" "GET" "/home/recommend"

# 测试 1.2: 首页推荐（带用户 ID）
test_api "首页推荐 - 带用户 ID" "GET" "/home/recommend?userId=$TEST_USER_ID"

# 测试 1.3: 首页推荐（带位置信息）
test_api "首页推荐 - 带位置信息" "GET" "/home/recommend?userId=$TEST_USER_ID&latitude=39.9042&longitude=116.4074"

echo "========================================"
echo "2. 匹配度展示 API 测试"
echo "========================================"
echo ""

# 测试 2.1: AI 智能推荐（无参数）
test_api "AI 推荐 - 无参数" "POST" "/ai/recommend" '{}'

# 测试 2.2: AI 智能推荐（带兴趣类型）
test_api "AI 推荐 - 带兴趣类型 (吃饭)" "POST" "/ai/recommend" '{"interestTypes":[1],"partnerLimit":3,"activityLimit":3}'

# 测试 2.3: AI 智能推荐（带活动分类）
test_api "AI 推荐 - 带活动分类 (运动)" "POST" "/ai/recommend" '{"category":"运动","partnerLimit":3,"activityLimit":3}'

# 测试 2.4: AI 智能推荐（带位置和兴趣）
test_api "AI 推荐 - 带位置和兴趣" "POST" "/ai/recommend" '{"interestTypes":[1,3],"category":"运动","latitude":39.9042,"longitude":116.4074,"partnerLimit":5,"activityLimit":5}'

echo "========================================"
echo "3. AI 智能搜索 API 测试"
echo "========================================"
echo ""

# 测试 3.1: 搭子筛选（按类型）
test_api "搭子筛选 - 按类型 (吃饭)" "GET" "/partner/filter?type=1"

# 测试 3.2: 搭子筛选（按位置）
test_api "搭子筛选 - 按位置" "GET" "/partner/filter?location=北京"

# 测试 3.3: 搭子筛选（按性别）
test_api "搭子筛选 - 按性别 (女)" "GET" "/partner/filter?gender=2"

# 测试 3.4: 搭子筛选（综合筛选）
test_api "搭子筛选 - 综合筛选" "GET" "/partner/filter?type=1&gender=1&pageNum=1&pageSize=10"

# 测试 3.5: 搭子列表
test_api "搭子列表 - 全部" "GET" "/partner?pageNum=1&pageSize=10"

# 测试 3.6: 搭子列表（按类型）
test_api "搭子列表 - 按类型" "GET" "/partner?type=1&pageNum=1&pageSize=10"

echo "========================================"
echo "4. 同事圈/校友圈 API 测试"
echo "========================================"
echo ""

# 测试 4.1: 加入公司
test_api "加入公司" "POST" "/user/company" '{"companyName":"测试公司"}'

# 测试 4.2: 加入学校
test_api "加入学校" "POST" "/user/school" '{"schoolName":"测试大学"}'

# 测试 4.3: 获取用户信息（验证公司/学校字段）
test_api "获取用户信息" "GET" "/user/info"

echo "========================================"
echo "5. AI 破冰话题 API 测试"
echo "========================================"
echo ""

# 测试 5.1: AI 对话 - 找搭子话题
test_api "AI 对话 - 找搭子" "POST" "/chat" '{"message":"怎么找搭子？"}'

# 测试 5.2: AI 对话 - 活动话题
test_api "AI 对话 - 活动" "POST" "/chat" '{"message":"有什么好玩的活动？"}'

# 测试 5.3: AI 对话 - 个人资料话题
test_api "AI 对话 - 个人资料" "POST" "/chat" '{"message":"怎么优化个人资料？"}'

# 测试 5.4: AI 对话 - 打招呼
test_api "AI 对话 - 打招呼" "POST" "/chat" '{"message":"你好"}'

# 测试 5.5: AI 对话 - 帮助
test_api "AI 对话 - 帮助" "POST" "/chat" '{"message":"如何使用这个应用？"}'

# 测试 5.6: AI 对话 - 未知问题
test_api "AI 对话 - 未知问题" "POST" "/chat" '{"message":"今天天气怎么样？"}'

# 测试 5.7: 获取会话历史
echo -e "${YELLOW}测试：获取会话历史${NC}"
echo "请求：GET /chat/history/{sessionId}"
# 需要先创建一个会话，这里跳过详细测试
echo "(需要先创建会话，此测试跳过详细验证)"
echo ""

echo "========================================"
echo "测试总结"
echo "========================================"
echo ""
echo -e "通过：${GREEN}$PASS_COUNT${NC}"
echo -e "失败：${RED}$FAIL_COUNT${NC}"
echo "总计：$((PASS_COUNT + FAIL_COUNT))"
echo ""
echo "测试完成时间：$(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"
