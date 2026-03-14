package com.aipick.service.impl;

import com.aipick.common.AiConstants;
import com.aipick.dto.AiRecommendRequest;
import com.aipick.dto.ChatRequest;
import com.aipick.dto.ChatResponse;
import com.aipick.entity.Activity;
import com.aipick.entity.Partner;
import com.aipick.entity.User;
import com.aipick.mapper.ActivityMapper;
import com.aipick.mapper.PartnerMapper;
import com.aipick.mapper.UserMapper;
import com.aipick.service.AiService;
import com.aipick.vo.ActivityVO;
import com.aipick.vo.AiRecommendVO;
import com.aipick.vo.PartnerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 推荐服务实现：基于规则与兴趣匹配的智能推荐（MVP 阶段无外部大模型）
 *
 * @author AI-Pick
 */
@Service
public class AiServiceImpl implements AiService {

    private final PartnerMapper partnerMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    
    // 随机因子生成器，用于匹配度算法防预测
    private static final Random RANDOM = new Random();
    
    // 匹配度随机波动范围（±5 分）
    private static final int MATCH_SCORE_RANDOM_RANGE = 5;

    public AiServiceImpl(PartnerMapper partnerMapper, ActivityMapper activityMapper, UserMapper userMapper) {
        this.partnerMapper = partnerMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
    }

    @Override
    public AiRecommendVO recommend(AiRecommendRequest request) {
        int partnerLimit = request.getPartnerLimit() != null ? request.getPartnerLimit() : AiConstants.DEFAULT_PARTNER_LIMIT;
        int activityLimit = request.getActivityLimit() != null ? request.getActivityLimit() : AiConstants.DEFAULT_ACTIVITY_LIMIT;

        AiRecommendVO vo = new AiRecommendVO();

        // 推荐搭子：状态待应征，按类型匹配 + 计划时间临近度排序，并计算匹配度
        LambdaQueryWrapper<Partner> partnerWrapper = new LambdaQueryWrapper<>();
        partnerWrapper.eq(Partner::getStatus, 0)
                .orderByDesc(Partner::getCreateTime);
        if (request.getInterestTypes() != null && !request.getInterestTypes().isEmpty()) {
            partnerWrapper.in(Partner::getType, request.getInterestTypes());
        }
        partnerWrapper.last("LIMIT " + AiConstants.MAX_QUERY_LIMIT);
        List<Partner> partners = partnerMapper.selectList(partnerWrapper);

        List<PartnerVO> partnerVOList = buildPartnerVOWithScore(partners, request);
        partnerVOList = partnerVOList.stream()
                .sorted(Comparator.comparing(PartnerVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(partnerLimit)
                .collect(Collectors.toList());
        vo.setPartners(partnerVOList);

        // 推荐活动：报名中/进行中，按分类匹配 + 开始时间排序，并计算匹配度
        LambdaQueryWrapper<Activity> activityWrapper = new LambdaQueryWrapper<>();
        activityWrapper.in(Activity::getStatus, 0, 1, 2)
                .orderByAsc(Activity::getStartTime)
                .orderByDesc(Activity::getCreateTime);
        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            activityWrapper.eq(Activity::getCategory, request.getCategory());
        }
        activityWrapper.last("LIMIT " + AiConstants.MAX_QUERY_LIMIT);
        List<Activity> activities = activityMapper.selectList(activityWrapper);

        List<ActivityVO> activityVOList = buildActivityVOWithScore(activities, request);
        activityVOList = activityVOList.stream()
                .sorted(Comparator.comparing(ActivityVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(activityLimit)
                .collect(Collectors.toList());
        vo.setActivities(activityVOList);

        return vo;
    }

    private List<PartnerVO> buildPartnerVOWithScore(List<Partner> partners, AiRecommendRequest request) {
        if (partners == null || partners.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量查询所有用户信息，避免 N+1 问题
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
        
        // 转换为 VO
        List<PartnerVO> list = new ArrayList<>();
        for (Partner p : partners) {
            PartnerVO vo = new PartnerVO();
            vo.setId(p.getId());
            vo.setUserId(p.getUserId());
            vo.setTitle(p.getTitle());
            vo.setDescription(p.getContent());
            vo.setMaxParticipants(p.getTargetCount());
            vo.setCurrentParticipants(p.getCurrentCount());
            vo.setAddress(p.getLocation());
            vo.setCreateTime(p.getCreateTime());
            vo.setMatchScore(calculatePartnerMatchScore(p, request));
            
            // 从 Map 中获取用户信息
            User user = userMap.get(p.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
                vo.setGender(user.getGender());
            }
            list.add(vo);
        }
        return list;
    }

    private Integer calculatePartnerMatchScore(Partner p, AiRecommendRequest request) {
        int score = AiConstants.BASE_MATCH_SCORE;
        if (request.getInterestTypes() != null && request.getInterestTypes().contains(p.getType())) {
            score += AiConstants.INTEREST_TYPE_MATCH_BONUS;
        }
        if (p.getPlanTime() != null && p.getPlanTime().isAfter(LocalDateTime.now())) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), p.getPlanTime());
            if (days <= AiConstants.TIME_NEAR_DAYS) {
                score += AiConstants.TIME_NEAR_BONUS;
            } else if (days <= AiConstants.TIME_MODERATE_DAYS) {
                score += AiConstants.TIME_MODERATE_BONUS;
            }
        }
        // 添加随机因子（±5 分波动），防止算法被完全预测
        int randomFactor = RANDOM.nextInt(MATCH_SCORE_RANDOM_RANGE * 2 + 1) - MATCH_SCORE_RANDOM_RANGE;
        score += randomFactor;
        return Math.min(AiConstants.MAX_MATCH_SCORE, Math.max(AiConstants.BASE_MATCH_SCORE, score));
    }

    private List<ActivityVO> buildActivityVOWithScore(List<Activity> activities, AiRecommendRequest request) {
        List<ActivityVO> list = new ArrayList<>();
        for (Activity a : activities) {
            ActivityVO vo = new ActivityVO();
            vo.setId(a.getId());
            vo.setUserId(a.getUserId());
            vo.setTitle(a.getTitle());
            vo.setDescription(a.getDescription());
            vo.setMaxParticipants(a.getMaxParticipants());
            vo.setCurrentParticipants(a.getCurrentParticipants());
            vo.setFee(a.getFee() != null ? a.getFee().doubleValue() : 0.0);
            vo.setAddress(a.getLocation());
            vo.setStatus(a.getStatus());
            vo.setCreateTime(a.getCreateTime());
            vo.setEventTime(a.getStartTime());
            vo.setMatchScore(calculateActivityMatchScore(a, request));
            list.add(vo);
        }
        return list;
    }

    private Integer calculateActivityMatchScore(Activity a, AiRecommendRequest request) {
        int score = AiConstants.BASE_MATCH_SCORE;
        if (request.getCategory() != null && request.getCategory().equals(a.getCategory())) {
            score += AiConstants.INTEREST_TYPE_MATCH_BONUS;
        }
        if (a.getStartTime() != null && a.getStartTime().isAfter(LocalDateTime.now())) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), a.getStartTime());
            if (days <= AiConstants.TIME_NEAR_DAYS) {
                score += AiConstants.TIME_NEAR_BONUS;
            } else if (days <= AiConstants.TIME_MODERATE_DAYS) {
                score += AiConstants.TIME_MODERATE_BONUS;
            }
        }
        // 添加随机因子（±5 分波动），防止算法被完全预测
        int randomFactor = RANDOM.nextInt(MATCH_SCORE_RANDOM_RANGE * 2 + 1) - MATCH_SCORE_RANDOM_RANGE;
        score += randomFactor;
        return Math.min(AiConstants.MAX_MATCH_SCORE, Math.max(AiConstants.BASE_MATCH_SCORE, score));
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString().replace("-", "");
        }
        String message = request.getMessage() != null ? request.getMessage().trim().toLowerCase() : "";
        String reply = buildRuleBasedReply(message);
        return new ChatResponse(sessionId, reply);
    }

    /**
     * MVP 阶段基于规则的简单回复，后续可接入大模型
     */
    private String buildRuleBasedReply(String message) {
        if (message.contains("搭子") || message.contains("找伴") || message.contains("约")) {
            return "想找志同道合的搭子？可以在「找搭子」页按兴趣类型筛选，发布你的需求或浏览他人的征搭子帖，匹配度高的会优先展示哦～";
        }
        if (message.contains("活动") || message.contains("线下") || message.contains("聚会")) {
            return "发现有趣活动：打开「发现活动」页，按分类、时间筛选，报名感兴趣的活动即可。有想法的也可以自己发起活动～";
        }
        if (message.contains("资料") || message.contains("头像") || message.contains("昵称") || message.contains("个人")) {
            return "优化个人资料能提高匹配率：完善昵称、头像、兴趣标签和简介，让他人更容易找到你。在「我的」-「个人资料」里即可编辑。";
        }
        if (message.contains("你好") || message.contains("hi") || message.contains("在吗")) {
            return "你好～我是社交小助手。可以问我：怎么找搭子、怎么发现活动、怎么优化个人资料，有问必答～";
        }
        if (message.contains("帮助") || message.contains("怎么") || message.contains("如何")) {
            return "我可以帮你：1）找志同道合的搭子 2）发现有趣的活动 3）优化个人资料。直接说你想了解哪一块即可～";
        }
        return "暂时还不懂这句～你可以问我：怎么找搭子、怎么发现活动、怎么优化个人资料，我会尽力帮你～";
    }
}
