package com.sparklink.service.impl;

import com.sparklink.dto.NaturalLanguageSearchRequest;
import com.sparklink.dto.SearchCriteriaDTO;
import com.sparklink.entity.Activity;
import com.sparklink.entity.Partner;
import com.sparklink.entity.User;
import com.sparklink.mapper.ActivityMapper;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.UserMapper;
import com.sparklink.service.SearchService;
import com.sparklink.vo.ActivityVO;
import com.sparklink.vo.PartnerVO;
import com.sparklink.service.MatchScoreService;
import com.sparklink.dto.MatchScoreDTO;
import com.sparklink.integration.DashScopeCompatClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 自然语言搜索服务实现
 * 
 * 使用 AI（百炼模型）解析自然语言查询，结合数据库条件搜索
 * 
 * @author AI-Pick
 */
@Service
public class SearchServiceImpl implements SearchService {

    private final ActivityMapper activityMapper;
    private final PartnerMapper partnerMapper;
    private final UserMapper userMapper;
    private final MatchScoreService matchScoreService;
    private final ObjectMapper objectMapper;
    private final DashScopeCompatClient dashScopeCompatClient;

    // 常见地点映射
    private static final Map<String, String> LOCATION_ALIAS = new HashMap<>();
    static {
        LOCATION_ALIAS.put("北京", "北京");
        LOCATION_ALIAS.put("上海", "上海");
        LOCATION_ALIAS.put("广州", "广州");
        LOCATION_ALIAS.put("深圳", "深圳");
        LOCATION_ALIAS.put("杭州", "杭州");
        LOCATION_ALIAS.put("成都", "成都");
        LOCATION_ALIAS.put("重庆", "重庆");
        LOCATION_ALIAS.put("武汉", "武汉");
        LOCATION_ALIAS.put("西安", "西安");
        LOCATION_ALIAS.put("南京", "南京");
    }

    // 常见活动类型关键词映射
    private static final Map<String, String> CATEGORY_KEYWORDS = new HashMap<>();
    static {
        CATEGORY_KEYWORDS.put("羽毛球", "羽毛球");
        CATEGORY_KEYWORDS.put("篮球", "篮球");
        CATEGORY_KEYWORDS.put("足球", "足球");
        CATEGORY_KEYWORDS.put("乒乓球", "乒乓球");
        CATEGORY_KEYWORDS.put("网球", "网球");
        CATEGORY_KEYWORDS.put("游泳", "游泳");
        CATEGORY_KEYWORDS.put("跑步", "跑步");
        CATEGORY_KEYWORDS.put("健身", "健身");
        CATEGORY_KEYWORDS.put("瑜伽", "瑜伽");
        CATEGORY_KEYWORDS.put("登山", "登山");
        CATEGORY_KEYWORDS.put("露营", "露营");
        CATEGORY_KEYWORDS.put("桌游", "桌游");
        CATEGORY_KEYWORDS.put("剧本杀", "剧本杀");
        CATEGORY_KEYWORDS.put("狼人杀", "狼人杀");
        CATEGORY_KEYWORDS.put("唱歌", "唱歌");
        CATEGORY_KEYWORDS.put("聚餐", "聚餐");
        CATEGORY_KEYWORDS.put("美食", "美食");
        CATEGORY_KEYWORDS.put("咖啡", "咖啡");
        CATEGORY_KEYWORDS.put("电影", "电影");
        CATEGORY_KEYWORDS.put("读书", "读书");
        CATEGORY_KEYWORDS.put("学习", "学习");
        CATEGORY_KEYWORDS.put("英语", "英语");
        CATEGORY_KEYWORDS.put("游戏", "游戏");
        CATEGORY_KEYWORDS.put("摄影", "摄影");
    }

    public SearchServiceImpl(ActivityMapper activityMapper, PartnerMapper partnerMapper,
                              UserMapper userMapper, MatchScoreService matchScoreService,
                              ObjectMapper objectMapper, DashScopeCompatClient dashScopeCompatClient) {
        this.activityMapper = activityMapper;
        this.partnerMapper = partnerMapper;
        this.userMapper = userMapper;
        this.matchScoreService = matchScoreService;
        this.objectMapper = objectMapper;
        this.dashScopeCompatClient = dashScopeCompatClient;
    }

    @Override
    public List<ActivityVO> searchActivities(NaturalLanguageSearchRequest request) {
        // 1. 解析查询条件
        SearchCriteriaDTO criteria = parseQuery(request.getQuery());
        
        // 2. 构建数据库查询条件
        LambdaQueryWrapper<Activity> wrapper = buildActivityQueryWrapper(criteria, request);
        
        // 3. 执行查询
        List<Activity> activities = activityMapper.selectList(wrapper);
        
        // 4. 转换为 VO 并计算匹配度
        return buildActivityVOList(activities, criteria, request.getUserId());
    }

    @Override
    public List<PartnerVO> searchPartners(NaturalLanguageSearchRequest request) {
        // 1. 解析查询条件
        SearchCriteriaDTO criteria = parseQuery(request.getQuery());
        
        // 2. 构建数据库查询条件
        LambdaQueryWrapper<Partner> wrapper = buildPartnerQueryWrapper(criteria, request);
        
        // 3. 执行查询
        List<Partner> partners = partnerMapper.selectList(wrapper);
        
        // 4. 转换为 VO 并计算匹配度
        return buildPartnerVOList(partners, criteria, request.getUserId());
    }

    @Override
    public SearchCriteriaDTO parseQuery(String query) {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setOriginalQuery(query);
        
        if (dashScopeCompatClient.isApiKeyConfigured()) {
            // 使用 AI 解析
            try {
                criteria = parseWithAI(query);
            } catch (Exception e) {
                // AI 解析失败，回退到规则解析
                criteria = parseWithRules(query);
            }
        } else {
            // 使用规则解析
            criteria = parseWithRules(query);
        }
        
        return criteria;
    }

    @Override
    public SearchResultVO smartSearch(NaturalLanguageSearchRequest request) {
        SearchResultVO result = new SearchResultVO();
        
        // 解析查询条件
        SearchCriteriaDTO criteria = parseQuery(request.getQuery());
        result.setCriteria(criteria);
        
        // 根据意图类型决定搜索范围
        String intent = criteria.getIntent();
        if (intent == null || "activity".equals(intent)) {
            // 搜索活动
            List<ActivityVO> activities = searchActivities(request);
            result.setActivities(activities);
        }
        
        if (intent == null || "partner".equals(intent)) {
            // 搜索搭子
            List<PartnerVO> partners = searchPartners(request);
            result.setPartners(partners);
        }
        
        return result;
    }

    /**
     * 使用 AI 解析自然语言
     */
    private SearchCriteriaDTO parseWithAI(String query) {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setOriginalQuery(query);
        
        try {
            String prompt = buildParsePrompt(query);
            String content = dashScopeCompatClient.completeUserOnly(prompt);
            if (content == null || content.isEmpty()) {
                return parseWithRules(query);
            }
            criteria = parseAIContent(content);
            criteria.setConfidence(0.9);
        } catch (Exception e) {
            // AI 解析失败，回退到规则
            return parseWithRules(query);
        }

        return criteria;
    }

    private String buildParsePrompt(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("请分析下面的用户查询，提取出搜索条件。要求：");
        sb.append("1. 地点：如果提到城市名，提取出来（如北京、上海）");
        sb.append("2. 时间：如果提到时间（今天、明天、周末、工作日、周几），提取出来");
        sb.append("3. 活动类型：如果提到具体活动（羽毛球、篮球、足球、桌游、聚餐等），提取出来");
        sb.append("4. 意图：判断用户想找活动(ACTIVITY)还是找搭子(PARTNER)，或者是两者都找(BOTH)");
        sb.append("5. 距离范围（可选）：如果提到具体距离（如5公里内、10公里以内），提取数字");
        sb.append("\n\n");
        sb.append("请用 JSON 格式返回，格式如下：");
        sb.append("{\"location\":\"北京\",\"timeKeyword\":\"周末\",\"category\":\"羽毛球\",\"intent\":\"ACTIVITY\",\"distanceKm\":10}");
        sb.append("\n\n");
        sb.append("用户查询：").append(query);
        sb.append("\n\n请只返回 JSON，不要其他文字。");
        return sb.toString();
    }

    private SearchCriteriaDTO parseAIContent(String content) {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        
        try {
            // 尝试提取 JSON 部分
            Pattern jsonPattern = Pattern.compile("\\{.*\\}");
            Matcher matcher = jsonPattern.matcher(content);
            if (matcher.find()) {
                String jsonStr = matcher.group();
                JsonNode node = objectMapper.readTree(jsonStr);
                
                criteria.setLocation(node.path("location").asText(null));
                criteria.setTimeKeyword(node.path("timeKeyword").asText(null));
                criteria.setCategory(node.path("category").asText(null));
                
                String intent = node.path("intent").asText("BOTH");
                criteria.setIntent(intent.toLowerCase());
                
                if (node.has("distanceKm")) {
                    criteria.setDistanceKm(node.path("distanceKm").asDouble());
                }
            }
        } catch (Exception e) {
            // 解析失败
        }
        
        return criteria;
    }

    /**
     * 使用规则解析自然语言（无 AI 时备用）
     */
    private SearchCriteriaDTO parseWithRules(String query) {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setOriginalQuery(query);
        criteria.setConfidence(0.5);
        
        // 解析地点
        for (Map.Entry<String, String> entry : LOCATION_ALIAS.entrySet()) {
            if (query.contains(entry.getKey())) {
                criteria.setLocation(entry.getValue());
                break;
            }
        }
        
        // 解析活动类型
        for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (query.contains(entry.getKey())) {
                criteria.setCategory(entry.getValue());
                break;
            }
        }
        
        // 解析时间关键词
        if (query.contains("今天")) {
            criteria.setTimeKeyword("today");
        } else if (query.contains("明天")) {
            criteria.setTimeKeyword("tomorrow");
        } else if (query.contains("周末")) {
            criteria.setTimeKeyword("weekend");
        } else if (query.contains("工作日")) {
            criteria.setTimeKeyword("weekday");
        } else if (query.contains("周一") || query.contains("周二") || query.contains("周三") ||
                   query.contains("周四") || query.contains("周五") || query.contains("周六") || query.contains("周日")) {
            criteria.setTimeKeyword("specific-day");
        }
        
        // 解析距离
        Pattern distancePattern = Pattern.compile("(\\d+)\\s*[公里Kmkm]+");
        Matcher distanceMatcher = distancePattern.matcher(query);
        if (distanceMatcher.find()) {
            criteria.setDistanceKm(Double.parseDouble(distanceMatcher.group(1)));
        }
        
        // 解析意图
        if (query.contains("活动") || query.contains("聚会") || query.contains("玩")) {
            criteria.setIntent("activity");
        } else if (query.contains("搭子") || query.contains("找伴") || query.contains("一起")) {
            criteria.setIntent("partner");
        } else {
            criteria.setIntent("both");
        }
        
        return criteria;
    }

    private LambdaQueryWrapper<Activity> buildActivityQueryWrapper(SearchCriteriaDTO criteria,
                                                                      NaturalLanguageSearchRequest request) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        
        // 状态：报名中、进行中
        wrapper.in(Activity::getStatus, 0, 1, 2);
        
        // 分类筛选
        if (criteria.getCategory() != null && !criteria.getCategory().isEmpty()) {
            wrapper.like(Activity::getCategory, criteria.getCategory());
        }
        
        // 地点筛选
        if (criteria.getLocation() != null && !criteria.getLocation().isEmpty()) {
            wrapper.like(Activity::getLocation, criteria.getLocation());
        }
        
        // 时间筛选
        if (criteria.getTimeKeyword() != null && !criteria.getTimeKeyword().isEmpty()) {
            LocalDateTime[] timeRange = parseTimeRange(criteria.getTimeKeyword());
            if (timeRange != null) {
                wrapper.between(Activity::getStartTime, timeRange[0], timeRange[1]);
            }
        }
        
        // 排序：开始时间升序，创建时间降序
        wrapper.orderByAsc(Activity::getStartTime).orderByDesc(Activity::getCreateTime);
        
        // 限制数量
        int limit = request.getLimit() != null ? request.getLimit() : 20;
        wrapper.last("LIMIT " + limit);
        
        return wrapper;
    }

    private LambdaQueryWrapper<Partner> buildPartnerQueryWrapper(SearchCriteriaDTO criteria,
                                                                   NaturalLanguageSearchRequest request) {
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<>();
        
        // 状态：待应征
        wrapper.eq(Partner::getStatus, 0);
        
        // 类型筛选
        if (criteria.getCategory() != null && !criteria.getCategory().isEmpty()) {
            wrapper.like(Partner::getContent, criteria.getCategory());
        }
        
        // 地点筛选
        if (criteria.getLocation() != null && !criteria.getLocation().isEmpty()) {
            wrapper.like(Partner::getLocation, criteria.getLocation());
        }
        
        // 时间筛选
        if (criteria.getTimeKeyword() != null && !criteria.getTimeKeyword().isEmpty()) {
            LocalDateTime[] timeRange = parseTimeRange(criteria.getTimeKeyword());
            if (timeRange != null) {
                wrapper.between(Partner::getPlanTime, timeRange[0], timeRange[1]);
            }
        }
        
        // 排序：计划时间升序，创建时间降序
        wrapper.orderByAsc(Partner::getPlanTime).orderByDesc(Partner::getCreateTime);
        
        // 限制数量
        int limit = request.getLimit() != null ? request.getLimit() : 20;
        wrapper.last("LIMIT " + limit);
        
        return wrapper;
    }

    private LocalDateTime[] parseTimeRange(String timeKeyword) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        
        switch (timeKeyword) {
            case "today":
                start = today;
                end = today.plusDays(1);
                break;
            case "tomorrow":
                start = today.plusDays(1);
                end = today.plusDays(2);
                break;
            case "weekend":
                // 找到本周六
                start = today.plusDays(DayOfWeek.SATURDAY.getValue() - today.getDayOfWeek().getValue());
                end = start.plusDays(2);
                break;
            case "weekday":
                start = today;
                end = today.plusDays(5 - today.getDayOfWeek().getValue());
                break;
            case "specific-day":
                // 默认查一周内
                start = today;
                end = today.plusDays(7);
                break;
            default:
                // 尝试解析具体日期
                try {
                    LocalDate date = LocalDate.parse(timeKeyword, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    start = date;
                    end = date.plusDays(1);
                } catch (DateTimeParseException e) {
                    return null;
                }
        }
        
        return new LocalDateTime[]{
            start.atStartOfDay(),
            end.atStartOfDay()
        };
    }

    private List<ActivityVO> buildActivityVOList(List<Activity> activities, SearchCriteriaDTO criteria, Long userId) {
        List<ActivityVO> voList = new ArrayList<>();
        
        for (Activity a : activities) {
            ActivityVO vo = new ActivityVO();
            vo.setId(a.getId());
            vo.setUserId(a.getUserId());
            vo.setTitle(a.getTitle());
            vo.setDescription(a.getDescription());
            vo.setCategory(a.getCategory());
            vo.setAddress(a.getLocation());
            vo.setMaxParticipants(a.getMaxParticipants());
            vo.setCurrentParticipants(a.getCurrentParticipants());
            vo.setFee(a.getFee() != null ? a.getFee().doubleValue() : 0.0);
            vo.setStatus(a.getStatus());
            vo.setCreateTime(a.getCreateTime());
            vo.setEventTime(a.getStartTime());
            vo.setCoverImage(a.getCoverImage());
            
            // 计算匹配度
            int matchScore = calculateActivityMatchScore(a, criteria);
            if (userId != null && a.getUserId() != null) {
                try {
                    MatchScoreDTO scoreDTO = matchScoreService.calculateMatchScore(userId, a.getUserId(), null);
                    if (scoreDTO != null && scoreDTO.getTotalScore() != null) {
                        matchScore = (matchScore + scoreDTO.getTotalScore()) / 2;
                    }
                } catch (Exception e) {
                    // 忽略匹配度计算错误
                }
            }
            vo.setMatchScore(matchScore);
            
            voList.add(vo);
        }
        
        // 按匹配度排序
        return voList.stream()
                .sorted(Comparator.comparing(ActivityVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private List<PartnerVO> buildPartnerVOList(List<Partner> partners, SearchCriteriaDTO criteria, Long userId) {
        // 批量查询用户信息
        List<Long> userIds = partners.stream()
                .map(Partner::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }
        
        List<PartnerVO> voList = new ArrayList<>();
        
        for (Partner p : partners) {
            PartnerVO vo = new PartnerVO();
            vo.setId(p.getId());
            vo.setUserId(p.getUserId());
            vo.setTitle(p.getTitle());
            vo.setDescription(p.getContent());
            vo.setCoverImage(p.getCoverImage());
            vo.setMaxParticipants(p.getTargetCount());
            vo.setCurrentParticipants(p.getCurrentCount());
            vo.setAddress(p.getLocation());
            vo.setCreateTime(p.getCreateTime());
            
            // 用户信息
            User user = userMap.get(p.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
                vo.setGender(user.getGender());
            }
            
            // 计算匹配度
            int matchScore = calculatePartnerMatchScore(p, criteria);
            if (userId != null && p.getUserId() != null) {
                try {
                    MatchScoreDTO scoreDTO = matchScoreService.calculateMatchScore(
                            userId, p.getUserId(), p.getId());
                    if (scoreDTO != null && scoreDTO.getTotalScore() != null) {
                        matchScore = (matchScore + scoreDTO.getTotalScore()) / 2;
                    }
                } catch (Exception e) {
                    // 忽略匹配度计算错误
                }
            }
            vo.setMatchScore(matchScore);
            
            voList.add(vo);
        }
        
        // 按匹配度排序
        return voList.stream()
                .sorted(Comparator.comparing(PartnerVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private int calculateActivityMatchScore(Activity activity, SearchCriteriaDTO criteria) {
        int score = 50; // 基础分
        
        // 分类匹配加分
        if (criteria.getCategory() != null && activity.getCategory() != null) {
            if (activity.getCategory().contains(criteria.getCategory())) {
                score += 30;
            }
        }
        
        // 地点匹配加分
        if (criteria.getLocation() != null && activity.getLocation() != null) {
            if (activity.getLocation().contains(criteria.getLocation())) {
                score += 20;
            }
        }
        
        return Math.min(100, Math.max(0, score));
    }

    private int calculatePartnerMatchScore(Partner partner, SearchCriteriaDTO criteria) {
        int score = 50; // 基础分
        
        // 内容匹配加分
        if (criteria.getCategory() != null && partner.getContent() != null) {
            if (partner.getContent().contains(criteria.getCategory())) {
                score += 30;
            }
        }
        
        // 地点匹配加分
        if (criteria.getLocation() != null && partner.getLocation() != null) {
            if (partner.getLocation().contains(criteria.getLocation())) {
                score += 20;
            }
        }
        
        return Math.min(100, Math.max(0, score));
    }
}