package com.sparklink.service.impl;

import com.sparklink.common.AiConstants;
import com.sparklink.common.RecommendFeedbackConstants;
import com.sparklink.dto.AiRecommendRequest;
import com.sparklink.common.PartnerScopeConstants;
import com.sparklink.common.PartnerTypeConstants;
import com.sparklink.entity.Activity;
import com.sparklink.entity.Partner;
import com.sparklink.entity.User;
import com.sparklink.mapper.ActivityMapper;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.UserMapper;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.service.MemoryFacade;
import com.sparklink.service.AiService;
import com.sparklink.service.RecommendFeedbackService;
import com.sparklink.util.AvatarUtil;
import com.sparklink.util.DistanceUtil;
import com.sparklink.util.MediaPathUtil;
import com.sparklink.vo.ActivityVO;
import com.sparklink.vo.AiRecommendVO;
import com.sparklink.vo.PartnerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 推荐服务：用户画像召回 → 规则初排 → 百炼模型精排 → 多样性重排；支持 10% 探索召回
 *
 * @author AI-Pick
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Random RANDOM = new Random();
    private static final int MATCH_SCORE_RANDOM_RANGE = 5;

    /** 用户对某搭子类型负向反馈每条扣分的步长（与 {@link RecommendFeedbackService#countNegativeByPartnerType} 配合） */
    private static final int FEEDBACK_PENALTY_PER_HIT = 8;

    /** 单维度因历史负向反馈最多扣的分，避免一次打穿 */
    private static final int FEEDBACK_PENALTY_CAP = 25;

    private final PartnerMapper partnerMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final RecommendFeedbackService recommendFeedbackService;
    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final MemoryFacade memoryFacade;

    public AiServiceImpl(PartnerMapper partnerMapper,
                         ActivityMapper activityMapper,
                         UserMapper userMapper,
                         RecommendFeedbackService recommendFeedbackService,
                         ObjectProvider<ChatModel> chatModelProvider,
                         MemoryFacade memoryFacade) {
        this.partnerMapper = partnerMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
        this.recommendFeedbackService = recommendFeedbackService;
        this.chatModel = chatModelProvider.getIfAvailable();
        this.objectMapper = new ObjectMapper();
        this.memoryFacade = memoryFacade;
    }

    @Override
    public AiRecommendVO recommend(AiRecommendRequest request) {
        int partnerLimit = request.getPartnerLimit() != null ? request.getPartnerLimit() : AiConstants.DEFAULT_PARTNER_LIMIT;
        int activityLimit = request.getActivityLimit() != null ? request.getActivityLimit() : AiConstants.DEFAULT_ACTIVITY_LIMIT;

        RecommendUserProfile profile = loadUserProfile(request);
        attachFeedbackContext(request.getUserId(), profile);
        boolean explore = RANDOM.nextDouble() < AiConstants.EXPLORE_PROBABILITY;

        List<Partner> partners = recallPartners(profile, explore);
        List<Activity> activities = recallActivities(profile, explore);
        partners = filterNegativeFeedbackPartners(partners, profile);
        activities = filterNegativeFeedbackActivities(activities, profile);

        List<PartnerVO> partnerVOList = buildPartnerVOWithScore(partners, request, profile);
        List<ActivityVO> activityVOList = buildActivityVOWithScore(activities, request, profile);
        String userInput = buildRecommendMemoryInput(request, profile);
        MemoryContext memoryContext = memoryFacade.recallForPrompt(request.getUserId(), userInput);

        partnerVOList.sort(Comparator.comparing(PartnerVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));
        activityVOList.sort(Comparator.comparing(ActivityVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));

        applyAiRerank(partnerVOList, activityVOList, profile, request.getUserId(), userInput, memoryContext);

        partnerVOList.sort(Comparator.comparing(PartnerVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));
        activityVOList.sort(Comparator.comparing(ActivityVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));

        partnerVOList = diversifyPartners(partnerVOList, partnerLimit);
        activityVOList = diversifyActivities(activityVOList, activityLimit);

        AiRecommendVO vo = new AiRecommendVO();
        vo.setPartners(partnerVOList);
        vo.setActivities(activityVOList);
        return vo;
    }

    /**
     * 加载用户画像：请求参数 + 用户表兴趣标签与简介推断
     */
    private RecommendUserProfile loadUserProfile(AiRecommendRequest request) {
        RecommendUserProfile p = new RecommendUserProfile();
        p.userId = request.getUserId();
        if (request.getLatitude() != null && request.getLongitude() != null) {
            p.latitude = request.getLatitude();
            p.longitude = request.getLongitude();
            p.hasLatLon = true;
        }
        if (request.getInterestTypes() != null) {
            p.interestTypes.addAll(request.getInterestTypes());
        }
        if (StringUtils.hasText(request.getCategory())) {
            p.activityCategories.add(request.getCategory().trim());
        }
        if (request.getUserId() == null) {
            return p;
        }
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            return p;
        }
        parseTagsJson(user.getTags(), p);
        inferFromBio(user.getBio(), p);
        return p;
    }

    /**
     * 加载用户对推荐的负向反馈：用于过滤已跳过条目、对同类目降权
     */
    private void attachFeedbackContext(Long userId, RecommendUserProfile profile) {
        if (userId == null) {
            profile.negativePartnerTargetIds = Collections.emptySet();
            profile.negativeActivityTargetIds = Collections.emptySet();
            profile.partnerTypeNegativeCount = Collections.emptyMap();
            profile.activityCategoryNegativeCount = Collections.emptyMap();
            return;
        }
        profile.negativePartnerTargetIds = new HashSet<>(
                recommendFeedbackService.findNegativeTargetIds(userId, RecommendFeedbackConstants.TARGET_PARTNER));
        profile.negativeActivityTargetIds = new HashSet<>(
                recommendFeedbackService.findNegativeTargetIds(userId, RecommendFeedbackConstants.TARGET_ACTIVITY));
        profile.partnerTypeNegativeCount = recommendFeedbackService.countNegativeByPartnerType(userId);
        profile.activityCategoryNegativeCount = recommendFeedbackService.countNegativeByActivityCategory(userId);
    }

    private static List<Partner> filterNegativeFeedbackPartners(List<Partner> partners, RecommendUserProfile profile) {
        if (partners == null || partners.isEmpty() || profile.negativePartnerTargetIds.isEmpty()) {
            return partners;
        }
        return partners.stream()
                .filter(p -> p.getId() == null || !profile.negativePartnerTargetIds.contains(p.getId()))
                .collect(Collectors.toList());
    }

    private static List<Activity> filterNegativeFeedbackActivities(List<Activity> activities, RecommendUserProfile profile) {
        if (activities == null || activities.isEmpty() || profile.negativeActivityTargetIds.isEmpty()) {
            return activities;
        }
        return activities.stream()
                .filter(a -> a.getId() == null || !profile.negativeActivityTargetIds.contains(a.getId()))
                .collect(Collectors.toList());
    }

    private void parseTagsJson(String tagsJson, RecommendUserProfile p) {
        if (!StringUtils.hasText(tagsJson)) {
            return;
        }
        String t = tagsJson.trim();
        try {
            if (t.startsWith("[")) {
                JsonNode arr = objectMapper.readTree(t);
                if (arr.isArray()) {
                    for (JsonNode n : arr) {
                        if (n.isTextual()) {
                            addLabel(n.asText(), p);
                        }
                    }
                }
                return;
            }
        } catch (Exception ignored) {
            // fall through to split
        }
        for (String part : t.split("[,，、]")) {
            addLabel(part, p);
        }
    }

    private void addLabel(String raw, RecommendUserProfile p) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String label = raw.trim();
        if (label.isEmpty()) {
            return;
        }
        p.tagLabels.add(label);
        Integer type = mapLabelToPartnerType(label);
        if (type != null) {
            p.interestTypes.add(type);
        }
        String cat = mapLabelToActivityCategory(label);
        if (cat != null) {
            p.activityCategories.add(cat);
        }
    }

    private void inferFromBio(String bio, RecommendUserProfile p) {
        if (!StringUtils.hasText(bio)) {
            return;
        }
        String b = bio.toLowerCase(Locale.ROOT);
        if (b.contains("周末")) {
            p.prefersWeekend = true;
        }
    }

    private static Integer mapLabelToPartnerType(String label) {
        String s = label.toLowerCase(Locale.ROOT);
        if (s.contains("饭") || s.contains("餐") || s.contains("吃")) {
            return 7;
        }
        if (s.contains("游") || s.contains("旅行")) {
            return 8;
        }
        if (s.contains("运动") || s.contains("健身") || s.contains("球") || s.contains("跑")) {
            return 5;
        }
        if (s.contains("学") || s.contains("书")) {
            return 14;
        }
        if (s.contains("游戏") || s.contains("开黑")) {
            return 13;
        }
        return null;
    }

    private static String mapLabelToActivityCategory(String label) {
        String s = label.toLowerCase(Locale.ROOT);
        if (s.contains("运动") || s.contains("球") || s.contains("跑")) {
            return "运动";
        }
        if (s.contains("吃") || s.contains("美食") || s.contains("餐")) {
            return "美食";
        }
        if (s.contains("学")) {
            return "学习";
        }
        if (s.contains("游戏") || s.contains("桌游")) {
            return "娱乐";
        }
        return null;
    }

    /**
     * 召回搭子：探索模式下优先异质类型；常规模式下按兴趣类型与标签文本匹配
     */
    private List<Partner> recallPartners(RecommendUserProfile profile, boolean explore) {
        LambdaQueryWrapper<Partner> w = new LambdaQueryWrapper<>();
        w.eq(Partner::getStatus, 0);
        w.orderByDesc(Partner::getCreateTime);
        w.last("LIMIT " + AiConstants.MAX_QUERY_LIMIT);
        List<Partner> all = partnerMapper.selectList(w);
        return all.stream()
                .filter(p -> keepPartnerInRecall(p, profile, explore))
                .filter(p -> viewerCanSeePartner(p, profile.userId))
                .sorted(Comparator.comparing(Partner::getPlanTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private boolean viewerCanSeePartner(Partner p, Long viewerUserId) {
        if (p == null) {
            return false;
        }
        if (viewerUserId != null && viewerUserId.equals(p.getUserId())) {
            return true;
        }
        int m = PartnerScopeConstants.normalizeMask(p.getScope());
        if (viewerUserId == null) {
            return (m & PartnerScopeConstants.BIT_PUBLIC) != 0;
        }
        if ((m & PartnerScopeConstants.BIT_PUBLIC) != 0) {
            return true;
        }
        User viewer = userMapper.selectById(viewerUserId);
        User author = p.getUserId() != null ? userMapper.selectById(p.getUserId()) : null;
        if (viewer == null || author == null) {
            return false;
        }
        if ((m & PartnerScopeConstants.BIT_COLLEAGUE) != 0) {
            if (org.springframework.util.StringUtils.hasText(viewer.getCompanyName())
                    && viewer.getCompanyName().equals(author.getCompanyName())) {
                return true;
            }
        }
        if ((m & PartnerScopeConstants.BIT_ALUMNI) != 0) {
            if (org.springframework.util.StringUtils.hasText(viewer.getSchoolName())
                    && viewer.getSchoolName().equals(author.getSchoolName())) {
                return true;
            }
        }
        return false;
    }

    private boolean keepPartnerInRecall(Partner p, RecommendUserProfile profile, boolean explore) {
        if (explore) {
            if (profile.interestTypes.isEmpty()) {
                return true;
            }
            return p.getType() == null || !profile.interestTypes.contains(p.getType());
        }
        if (profile.interestTypes.isEmpty() && profile.tagLabels.isEmpty()) {
            return true;
        }
        if (p.getType() != null && profile.interestTypes.contains(p.getType())) {
            return true;
        }
        return tagMatchPartnerText(p, profile);
    }

    private boolean tagMatchPartnerText(Partner p, RecommendUserProfile profile) {
        String hay = (p.getTitle() + " " + Objects.toString(p.getContent(), "") + " "
                + Objects.toString(p.getPreference(), "")).toLowerCase(Locale.ROOT);
        for (String tag : profile.tagLabels) {
            if (StringUtils.hasText(tag) && hay.contains(tag.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 召回活动：有经纬度时优先地理近邻；常规模式按分类/标签；探索模式排除主分类
     */
    private List<Activity> recallActivities(RecommendUserProfile profile, boolean explore) {
        LambdaQueryWrapper<Activity> w = new LambdaQueryWrapper<>();
        w.in(Activity::getStatus, 0, 1, 2);
        w.orderByAsc(Activity::getStartTime).orderByDesc(Activity::getCreateTime);
        w.last("LIMIT " + AiConstants.MAX_QUERY_LIMIT);
        List<Activity> all = activityMapper.selectList(w);
        List<Activity> filtered = all.stream()
                .filter(a -> keepActivityInRecall(a, profile, explore))
                .collect(Collectors.toCollection(ArrayList::new));
        if (profile.hasGeo()) {
            filtered.sort(Comparator.comparingDouble(a -> activityDistanceKm(a, profile)));
        }
        return filtered;
    }

    private boolean keepActivityInRecall(Activity a, RecommendUserProfile profile, boolean explore) {
        if (explore) {
            if (profile.activityCategories.isEmpty()) {
                return true;
            }
            String cat = a.getCategory();
            return cat == null || !profile.activityCategories.contains(cat);
        }
        if (profile.activityCategories.isEmpty() && profile.tagLabels.isEmpty()) {
            return true;
        }
        if (StringUtils.hasText(a.getCategory()) && profile.activityCategories.contains(a.getCategory())) {
            return true;
        }
        return tagMatchActivityText(a, profile);
    }

    private boolean tagMatchActivityText(Activity a, RecommendUserProfile profile) {
        String hay = (a.getTitle() + " " + Objects.toString(a.getDescription(), "") + " "
                + Objects.toString(a.getCategory(), "")).toLowerCase(Locale.ROOT);
        for (String tag : profile.tagLabels) {
            if (StringUtils.hasText(tag) && hay.contains(tag.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static double activityDistanceKm(Activity a, RecommendUserProfile profile) {
        if (a.getLatitude() == null || a.getLongitude() == null) {
            return Double.POSITIVE_INFINITY;
        }
        double d = DistanceUtil.haversineKm(profile.latitude, profile.longitude,
                a.getLatitude().doubleValue(), a.getLongitude().doubleValue());
        return Double.isFinite(d) ? d : Double.POSITIVE_INFINITY;
    }

    private List<PartnerVO> buildPartnerVOWithScore(List<Partner> partners, AiRecommendRequest request,
                                                    RecommendUserProfile profile) {
        if (partners == null || partners.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> userIds = partners.stream()
                .map(Partner::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (User user : userMapper.selectBatchIds(userIds)) {
                userMap.put(user.getId(), user);
            }
        }
        List<PartnerVO> list = new ArrayList<>();
        for (Partner p : partners) {
            PartnerVO vo = new PartnerVO();
            vo.setId(p.getId());
            vo.setUserId(p.getUserId());
            vo.setTitle(p.getTitle());
            vo.setDescription(p.getContent());
            vo.setPreference(p.getPreference());
            vo.setTypeCode(p.getType());
            vo.setTypeName(PartnerTypeConstants.labelOf(p.getType()));
            if (p.getType() != null) {
                vo.setType(String.valueOf(p.getType()));
            }
            vo.setScope(p.getScope());
            vo.setScopeName(PartnerScopeConstants.labelOf(p.getScope()));
            vo.setCoverImage(MediaPathUtil.normalizeForResponse(p.getCoverImage()));
            vo.setMaxParticipants(p.getTargetCount());
            vo.setCurrentParticipants(p.getCurrentCount());
            vo.setAddress(p.getLocation());
            vo.setCreateTime(p.getCreateTime());
            vo.setMatchScore(calculatePartnerMatchScore(p, request, profile));
            User user = userMap.get(p.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(AvatarUtil.sanitizeForResponse(user.getAvatar()));
                vo.setGender(user.getGender());
                vo.setTags(parseTagsToList(user.getTags()));
                if (StringUtils.hasText(user.getBio())) {
                    vo.setBio(user.getBio());
                }
            }
            list.add(vo);
        }
        return list;
    }

    private List<String> parseTagsToList(String tagsJson) {
        List<String> out = new ArrayList<>();
        if (!StringUtils.hasText(tagsJson)) {
            return out;
        }
        String t = tagsJson.trim();
        try {
            if (t.startsWith("[")) {
                JsonNode arr = objectMapper.readTree(t);
                if (arr.isArray()) {
                    for (JsonNode n : arr) {
                        if (n.isTextual()) {
                            String s = n.asText().trim();
                            if (!s.isEmpty()) {
                                out.add(s);
                            }
                        }
                    }
                }
                return out;
            }
        } catch (Exception ignored) {
            // fall through to split
        }
        for (String part : t.split("[,，、]")) {
            String s = part.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    private List<ActivityVO> buildActivityVOWithScore(List<Activity> activities, AiRecommendRequest request,
                                                      RecommendUserProfile profile) {
        List<ActivityVO> list = new ArrayList<>();
        for (Activity a : activities) {
            ActivityVO vo = new ActivityVO();
            vo.setId(a.getId());
            vo.setUserId(a.getUserId());
            vo.setTitle(a.getTitle());
            vo.setDescription(a.getDescription());
            vo.setCategory(a.getCategory());
            vo.setMaxParticipants(a.getMaxParticipants());
            vo.setCurrentParticipants(a.getCurrentParticipants());
            vo.setFee(a.getFee() != null ? a.getFee().doubleValue() : 0.0);
            vo.setAddress(a.getLocation());
            vo.setStatus(a.getStatus());
            vo.setCreateTime(a.getCreateTime());
            vo.setEventTime(a.getStartTime());
            vo.setMatchScore(calculateActivityMatchScore(a, request, profile));
            vo.setCoverImage(MediaPathUtil.normalizeForResponse(a.getCoverImage()));
            if (a.getLatitude() != null) {
                vo.setLatitude(a.getLatitude().doubleValue());
            }
            if (a.getLongitude() != null) {
                vo.setLongitude(a.getLongitude().doubleValue());
            }
            if (profile.hasGeo() && a.getLatitude() != null && a.getLongitude() != null) {
                double km = DistanceUtil.haversineKm(profile.latitude, profile.longitude,
                        a.getLatitude().doubleValue(), a.getLongitude().doubleValue());
                if (Double.isFinite(km)) {
                    vo.setDistance(km);
                }
            }
            list.add(vo);
        }
        return list;
    }

    private int calculatePartnerMatchScore(Partner p, AiRecommendRequest request, RecommendUserProfile profile) {
        int score = AiConstants.BASE_MATCH_SCORE;
        if (request.getInterestTypes() != null && request.getInterestTypes().contains(p.getType())) {
            score += AiConstants.INTEREST_TYPE_MATCH_BONUS;
        } else if (p.getType() != null && profile.interestTypes.contains(p.getType())) {
            score += AiConstants.INTEREST_TYPE_MATCH_BONUS;
        } else if (tagMatchPartnerText(p, profile)) {
            score += AiConstants.INTEREST_TYPE_MATCH_BONUS / 2;
        }
        if (p.getPlanTime() != null && p.getPlanTime().isAfter(LocalDateTime.now())) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), p.getPlanTime());
            score += timeProximityBonus(days);
            if (profile.prefersWeekend && isWeekend(p.getPlanTime())) {
                score += 6;
            }
        }
        int randomFactor = RANDOM.nextInt(MATCH_SCORE_RANDOM_RANGE * 2 + 1) - MATCH_SCORE_RANDOM_RANGE;
        score += randomFactor;
        score -= partnerTypeFeedbackPenalty(profile, p.getType());
        return Math.min(AiConstants.MAX_MATCH_SCORE, Math.max(AiConstants.BASE_MATCH_SCORE, score));
    }

    private int calculateActivityMatchScore(Activity a, AiRecommendRequest request, RecommendUserProfile profile) {
        int score = AiConstants.BASE_MATCH_SCORE;
        if (request.getCategory() != null && request.getCategory().equals(a.getCategory())) {
            score += AiConstants.INTEREST_TYPE_MATCH_BONUS;
        } else if (StringUtils.hasText(a.getCategory()) && profile.activityCategories.contains(a.getCategory())) {
            score += AiConstants.INTEREST_TYPE_MATCH_BONUS;
        } else if (tagMatchActivityText(a, profile)) {
            score += AiConstants.INTEREST_TYPE_MATCH_BONUS / 2;
        }
        if (profile.hasGeo() && a.getLatitude() != null && a.getLongitude() != null) {
            double km = DistanceUtil.haversineKm(profile.latitude, profile.longitude,
                    a.getLatitude().doubleValue(), a.getLongitude().doubleValue());
            if (km <= 5) {
                score += 10;
            } else if (km <= 20) {
                score += 5;
            }
        }
        if (a.getStartTime() != null && a.getStartTime().isAfter(LocalDateTime.now())) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), a.getStartTime());
            score += timeProximityBonus(days);
            if (profile.prefersWeekend && isWeekend(a.getStartTime())) {
                score += 6;
            }
        }
        int randomFactor = RANDOM.nextInt(MATCH_SCORE_RANDOM_RANGE * 2 + 1) - MATCH_SCORE_RANDOM_RANGE;
        score += randomFactor;
        score -= activityCategoryFeedbackPenalty(profile, a);
        return Math.min(AiConstants.MAX_MATCH_SCORE, Math.max(AiConstants.BASE_MATCH_SCORE, score));
    }

    private static int partnerTypeFeedbackPenalty(RecommendUserProfile profile, Integer partnerType) {
        if (partnerType == null || profile.partnerTypeNegativeCount == null || profile.partnerTypeNegativeCount.isEmpty()) {
            return 0;
        }
        int c = profile.partnerTypeNegativeCount.getOrDefault(partnerType, 0);
        return Math.min(FEEDBACK_PENALTY_CAP, c * FEEDBACK_PENALTY_PER_HIT);
    }

    private static int activityCategoryFeedbackPenalty(RecommendUserProfile profile, Activity a) {
        if (profile.activityCategoryNegativeCount == null || profile.activityCategoryNegativeCount.isEmpty()) {
            return 0;
        }
        String cat = StringUtils.hasText(a.getCategory()) ? a.getCategory().trim() : "__uncategorized__";
        int c = profile.activityCategoryNegativeCount.getOrDefault(cat, 0);
        return Math.min(FEEDBACK_PENALTY_CAP, c * FEEDBACK_PENALTY_PER_HIT);
    }

    private static int timeProximityBonus(long days) {
        if (days <= AiConstants.TIME_NEAR_DAYS) {
            return AiConstants.TIME_NEAR_BONUS;
        }
        if (days <= AiConstants.TIME_MODERATE_DAYS) {
            return AiConstants.TIME_MODERATE_BONUS;
        }
        return 0;
    }

    private static boolean isWeekend(LocalDateTime t) {
        DayOfWeek d = t.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    private static String buildRecommendMemoryInput(AiRecommendRequest request, RecommendUserProfile profile) {
        String category = request.getCategory() != null ? request.getCategory().trim() : "";
        String interests = profile.interestTypes.isEmpty() ? "未指定" : profile.interestTypes.toString();
        return "推荐请求: category=" + (category.isEmpty() ? "未指定" : category) + ", interests=" + interests;
    }

    /**
     * 百炼模型对 Top 候选重打分；失败则保留规则分
     */
    private void applyAiRerank(List<PartnerVO> partners,
                               List<ActivityVO> activities,
                               RecommendUserProfile profile,
                               Long userId,
                               String userInput,
                               MemoryContext memoryContext) {
        if (chatModel == null) {
            return;
        }
        List<PartnerVO> pTop = partners.stream()
                .sorted(Comparator.comparing(PartnerVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(AiConstants.AI_RANK_MAX_CANDIDATES)
                .collect(Collectors.toList());
        List<ActivityVO> aTop = activities.stream()
                .sorted(Comparator.comparing(ActivityVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(AiConstants.AI_RANK_MAX_CANDIDATES)
                .collect(Collectors.toList());
        if (pTop.isEmpty() && aTop.isEmpty()) {
            return;
        }
        String prompt = buildRankPrompt(profile, pTop, aTop);
        String mergedPrompt = memoryFacade.mergePrompt(prompt, memoryContext);
        try {
            String raw = chatModel.call(mergedPrompt);
            Map<String, Integer> scoreMap = parseAiScoreMap(raw);
            applyPartnerAiScores(partners, scoreMap);
            applyActivityAiScores(activities, scoreMap);
            memoryFacade.enqueueConversation(userId, userInput, raw);
        } catch (Exception ignored) {
            // 保留规则分
        }
    }

    private String buildRankPrompt(RecommendUserProfile profile, List<PartnerVO> pTop, List<ActivityVO> aTop) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是推荐排序助手。根据用户画像，为每个候选给出 0-100 的整数相关分。");
        sb.append("只输出 JSON 数组，元素格式 {\"kind\":\"partner\"或\"activity\",\"id\":数字,\"score\":整数}，不要其它文字。\n");
        sb.append("用户画像：\n");
        sb.append("- 搭子兴趣类型码偏好：").append(profile.interestTypes.isEmpty() ? "未指定" : profile.interestTypes.toString()).append('\n');
        sb.append("- 活动分类偏好：").append(profile.activityCategories.isEmpty() ? "未指定" : profile.activityCategories.toString()).append('\n');
        sb.append("- 兴趣标签：").append(profile.tagLabels.isEmpty() ? "无" : String.join(",", profile.tagLabels)).append('\n');
        sb.append("- 位置：").append(profile.hasGeo() ? "有经纬度（可偏好近距离活动）" : "未提供").append('\n');
        sb.append("- 时间偏好：").append(profile.prefersWeekend ? "偏好周末" : "未特别强调").append('\n');
        sb.append("候选列表（每行一条）：\n");
        for (PartnerVO vo : pTop) {
            sb.append("partner|").append(vo.getId()).append("|type=").append(vo.getType()).append("|")
                    .append(truncate(vo.getTitle(), 40)).append("|")
                    .append(truncate(vo.getAddress(), 24)).append("|")
                    .append(truncate(vo.getDescription(), 60)).append('\n');
        }
        for (ActivityVO vo : aTop) {
            sb.append("activity|").append(vo.getId()).append("|cat=")
                    .append(truncate(vo.getCategory(), 12)).append("|")
                    .append(truncate(vo.getTitle(), 40)).append("|")
                    .append(truncate(vo.getAddress(), 24)).append("|")
                    .append(truncate(vo.getDescription(), 50)).append("|")
                    .append(vo.getEventTime() != null ? vo.getEventTime().toString() : "").append('\n');
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }

    private Map<String, Integer> parseAiScoreMap(String raw) throws Exception {
        if (raw == null) {
            return Map.of();
        }
        String json = extractJsonArray(raw.trim());
        JsonNode arr = objectMapper.readTree(json);
        Map<String, Integer> map = new HashMap<>();
        if (!arr.isArray()) {
            return map;
        }
        for (JsonNode el : arr) {
            String kind = el.path("kind").asText("");
            long id = el.path("id").asLong(0L);
            int sc = el.path("score").asInt(-1);
            if (id <= 0 || sc < 0) {
                continue;
            }
            if ("partner".equalsIgnoreCase(kind)) {
                map.put("p:" + id, Math.min(100, sc));
            } else if ("activity".equalsIgnoreCase(kind)) {
                map.put("a:" + id, Math.min(100, sc));
            }
        }
        return map;
    }

    private static String extractJsonArray(String raw) {
        int i = raw.indexOf('[');
        int j = raw.lastIndexOf(']');
        if (i >= 0 && j > i) {
            return raw.substring(i, j + 1);
        }
        return raw;
    }

    private void applyPartnerAiScores(List<PartnerVO> all, Map<String, Integer> scoreMap) {
        for (PartnerVO vo : all) {
            Integer s = scoreMap.get("p:" + vo.getId());
            if (s != null) {
                int merged = (int) Math.round(vo.getMatchScore() * 0.35 + s * 0.65);
                vo.setMatchScore(Math.min(AiConstants.MAX_MATCH_SCORE, Math.max(AiConstants.BASE_MATCH_SCORE, merged)));
            }
        }
    }

    private void applyActivityAiScores(List<ActivityVO> all, Map<String, Integer> scoreMap) {
        for (ActivityVO vo : all) {
            Integer s = scoreMap.get("a:" + vo.getId());
            if (s != null) {
                int merged = (int) Math.round(vo.getMatchScore() * 0.35 + s * 0.65);
                vo.setMatchScore(Math.min(AiConstants.MAX_MATCH_SCORE, Math.max(AiConstants.BASE_MATCH_SCORE, merged)));
            }
        }
    }

    /**
     * 多样性：在分数基础上惩罚同搭子类型重复，并鼓励覆盖多种类型
     */
    private List<PartnerVO> diversifyPartners(List<PartnerVO> sorted, int limit) {
        if (sorted.isEmpty()) {
            return sorted;
        }
        List<PartnerVO> pool = new ArrayList<>(sorted);
        pool.sort(Comparator.comparing(PartnerVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));
        List<PartnerVO> out = new ArrayList<>();
        Map<Integer, Integer> typeCount = new HashMap<>();
        int targetDistinct = Math.min(AiConstants.DIVERSITY_TARGET_PARTNER_TYPES, limit);
        while (out.size() < limit && !pool.isEmpty()) {
            PartnerVO best = null;
            double bestAdj = Double.NEGATIVE_INFINITY;
            for (PartnerVO vo : pool) {
                int t = parsePartnerType(vo);
                int cnt = typeCount.getOrDefault(t, 0);
                double adj = vo.getMatchScore() - AiConstants.DIVERSITY_DUPLICATE_PENALTY * cnt;
                if (out.size() < targetDistinct && cnt == 0) {
                    adj += 5;
                }
                if (adj > bestAdj) {
                    bestAdj = adj;
                    best = vo;
                }
            }
            if (best == null) {
                break;
            }
            pool.remove(best);
            out.add(best);
            typeCount.merge(parsePartnerType(best), 1, Integer::sum);
        }
        return out;
    }

    private List<ActivityVO> diversifyActivities(List<ActivityVO> sorted, int limit) {
        if (sorted.isEmpty()) {
            return sorted;
        }
        List<ActivityVO> pool = new ArrayList<>(sorted);
        pool.sort(Comparator.comparing(ActivityVO::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder())));
        List<ActivityVO> out = new ArrayList<>();
        Map<String, Integer> catCount = new HashMap<>();
        int targetDistinct = Math.min(AiConstants.DIVERSITY_TARGET_ACTIVITY_CATEGORIES, limit);
        while (out.size() < limit && !pool.isEmpty()) {
            ActivityVO best = null;
            double bestAdj = Double.NEGATIVE_INFINITY;
            for (ActivityVO vo : pool) {
                String cat = activityCategoryKey(vo);
                int cnt = catCount.getOrDefault(cat, 0);
                double adj = vo.getMatchScore() - AiConstants.DIVERSITY_DUPLICATE_PENALTY * cnt;
                if (out.size() < targetDistinct && cnt == 0) {
                    adj += 5;
                }
                if (adj > bestAdj) {
                    bestAdj = adj;
                    best = vo;
                }
            }
            if (best == null) {
                break;
            }
            pool.remove(best);
            out.add(best);
            catCount.merge(activityCategoryKey(best), 1, Integer::sum);
        }
        return out;
    }

    private static int parsePartnerType(PartnerVO vo) {
        if (vo.getType() == null) {
            return -1;
        }
        try {
            return Integer.parseInt(vo.getType().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String activityCategoryKey(ActivityVO vo) {
        if (StringUtils.hasText(vo.getCategory())) {
            return vo.getCategory().trim();
        }
        return "__uncategorized__";
    }

    /**
     * 内部用户画像
     */
    private static final class RecommendUserProfile {
        private Long userId;
        private double latitude;
        private double longitude;
        private boolean hasLatLon;
        private final Set<Integer> interestTypes = new LinkedHashSet<>();
        private final Set<String> activityCategories = new LinkedHashSet<>();
        private final List<String> tagLabels = new ArrayList<>();
        private boolean prefersWeekend;

        /** 用户已负向反馈的搭子帖 ID（跳过/不合），不再召回 */
        private Set<Long> negativePartnerTargetIds = Collections.emptySet();

        /** 用户已负向反馈的活动 ID */
        private Set<Long> negativeActivityTargetIds = Collections.emptySet();

        /** 按搭子类型的负向反馈次数，用于同类降权 */
        private Map<Integer, Integer> partnerTypeNegativeCount = Collections.emptyMap();

        /** 按活动分类的负向反馈次数 */
        private Map<String, Integer> activityCategoryNegativeCount = Collections.emptyMap();

        private boolean hasGeo() {
            return hasLatLon;
        }
    }
}
