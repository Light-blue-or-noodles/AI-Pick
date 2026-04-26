package com.sparklink.service.impl;

import com.sparklink.dto.MatchScoreDTO;
import com.sparklink.entity.Partner;
import com.sparklink.entity.User;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.UserMapper;
import com.sparklink.service.MatchScoreService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sparklink.integration.DashScopeCompatClient;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 匹配度计算实现类
 * <p>
 * 有搭子活动 ID 时：活动标签(我的 vs 活动偏好) 30% + 发布者标签(我的 vs 发布者) 10% + 活动位置(我当前 vs 活动坐标/地址) 30% + AI 30%。
 * 无搭子 ID 时：发布者标签 40% + 两用户位置 30% + AI 30%（无活动标签维）。
 * 已移除「时间」规则维；time 字段恒 0 仅作兼容。
 */
@Slf4j
@Service
public class MatchScoreServiceImpl implements MatchScoreService {

    private static final int MAX_SCORE = 100;

    private static final double W_ACTIVITY = 0.3;
    private static final double W_PUBLISHER = 0.1;
    private static final double W_LOCATION = 0.3;
    private static final double W_AI = 0.3;

    /** 无活动上下文时：仅发布者标签 + 位置 + AI */
    private static final double W_PUBLISHER_ONLY = 0.4;
    private static final double W_LOC_FALLBACK = 0.3;
    private static final double W_AI_FALLBACK = 0.3;

    private final UserMapper userMapper;
    private final PartnerMapper partnerMapper;
    private final ObjectMapper objectMapper;
    private final DashScopeCompatClient dashScopeCompatClient;

    public MatchScoreServiceImpl(UserMapper userMapper,
            PartnerMapper partnerMapper,
            ObjectMapper objectMapper,
            DashScopeCompatClient dashScopeCompatClient) {
        this.userMapper = userMapper;
        this.partnerMapper = partnerMapper;
        this.objectMapper = objectMapper;
        this.dashScopeCompatClient = dashScopeCompatClient;
    }

    @Override
    @Transactional(readOnly = true)
    public MatchScoreDTO calculateMatchScore(Long userId, Long targetId, Long partnerId) {
        log.info("[match-score] 开始 userId={} targetId={} partnerId={}", userId, targetId, partnerId);
        User user = userMapper.selectById(userId);
        User target = userMapper.selectById(targetId);
        if (user == null || target == null) {
            return emptyResult(userId, targetId, partnerId, "用户不存在，无法计算匹配度");
        }

        Partner partner = null;
        if (partnerId != null) {
            partner = partnerMapper.selectById(partnerId);
            if (partner != null && target.getId() != null
                    && !partner.getUserId().equals(target.getId())) {
                log.warn("[match-score] partnerId={} 的发布者 userId={} 与 targetId={} 不一致，忽略该活动",
                        partnerId, partner.getUserId(), target.getId());
                partner = null;
            }
        }

        Set<String> myTags = parseTags(user.getTags());
        Set<String> publisherTags = parseTags(target.getTags());

        int activityTagScore;
        int publisherTagScore;
        int locationScore;
        if (partner != null) {
            Set<String> activityTags = parseActivityTagsFromPreference(partner.getPreference());
            activityTagScore = tagOverlapScore(myTags, activityTags);
            publisherTagScore = tagOverlapScore(myTags, publisherTags);
            locationScore = calculateLocationForActivity(user, partner);
        } else {
            activityTagScore = 0;
            publisherTagScore = tagOverlapScore(myTags, publisherTags);
            locationScore = calculateLocationUserToUser(user, target);
        }

        int aiScore = calculateAiScore(user, target, partner, activityTagScore, publisherTagScore, locationScore);

        double totalWeighted;
        if (partner != null) {
            totalWeighted = activityTagScore * W_ACTIVITY
                    + publisherTagScore * W_PUBLISHER
                    + locationScore * W_LOCATION
                    + aiScore * W_AI;
        } else {
            totalWeighted = publisherTagScore * W_PUBLISHER_ONLY
                    + locationScore * W_LOC_FALLBACK
                    + aiScore * W_AI_FALLBACK;
        }
        int totalScore = (int) Math.round(totalWeighted);
        totalScore = Math.min(MAX_SCORE, Math.max(0, totalScore));

        log.info("[match-score] 规则分(0-100) 活动={} 发布者={} 位置={} AI={} 有活动上下文={} 总分={} | 我tags长度={} 发布者tags长度={} 我位置是否空={}",
                activityTagScore,
                publisherTagScore,
                locationScore,
                aiScore,
                partner != null,
                totalScore,
                myTags.size(),
                publisherTags.size(),
                isBlank(user.getLocation()));

        MatchScoreDTO dto = new MatchScoreDTO();
        dto.setUserId(userId);
        dto.setTargetId(targetId);
        dto.setPartnerId(partnerId);
        dto.setActivityTagScore(activityTagScore);
        dto.setPublisherTagScore(publisherTagScore);
        dto.setInterestScore(activityTagScore);
        dto.setLocationScore(locationScore);
        dto.setTimeScore(0);
        dto.setAiScore(aiScore);
        dto.setTotalScore(totalScore);
        dto.setSuggestions(buildSuggestions(activityTagScore, publisherTagScore, locationScore, partner != null));
        dto.setReason(buildReason(dto, partner != null));
        return dto;
    }

    private MatchScoreDTO emptyResult(Long userId, Long targetId, Long partnerId, String reason) {
        MatchScoreDTO dto = new MatchScoreDTO();
        dto.setUserId(userId);
        dto.setTargetId(targetId);
        dto.setPartnerId(partnerId);
        dto.setTotalScore(0);
        dto.setActivityTagScore(0);
        dto.setPublisherTagScore(0);
        dto.setInterestScore(0);
        dto.setLocationScore(0);
        dto.setTimeScore(0);
        dto.setAiScore(0);
        dto.setReason(reason);
        dto.setSuggestions(new ArrayList<>());
        return dto;
    }

    /**
     * 与 PartnerServiceImpl 一致：从 preference 拆出活动侧「标签/关键词」集。
     */
    private Set<String> parseActivityTagsFromPreference(String preference) {
        Set<String> set = new HashSet<>();
        if (preference == null || preference.isBlank()) {
            return set;
        }
        String[] parts = preference.split("[,，、\\s]+");
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                set.add(t);
            }
        }
        return set;
    }

    /**
     * 与旧版「兴趣分」同：交集 / max(|A|,|B|) * 100。
     */
    private int tagOverlapScore(Set<String> myTags, Set<String> otherTags) {
        if (myTags.isEmpty() || otherTags.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(myTags);
        intersection.retainAll(otherTags);
        if (intersection.isEmpty()) {
            return 0;
        }
        int base = Math.max(myTags.size(), otherTags.size());
        if (base <= 0) {
            return 0;
        }
        int score = (int) Math.round(intersection.size() * 1.0 / base * MAX_SCORE);
        return Math.min(MAX_SCORE, Math.max(0, score));
    }

    private Set<String> parseTags(String jsonArray) {
        Set<String> set = new HashSet<>();
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            return set;
        }
        String text = jsonArray.trim();
        try {
            if (text.startsWith("[")) {
                JsonNode node = objectMapper.readTree(text);
                if (node.isArray()) {
                    for (JsonNode n : node) {
                        if (n.isTextual()) {
                            String v = n.asText().trim();
                            if (!v.isEmpty()) {
                                set.add(v);
                            }
                        }
                    }
                }
            } else {
                for (String p : text.split("[,，、\\s]+")) {
                    String v = p.trim();
                    if (!v.isEmpty()) {
                        set.add(v);
                    }
                }
            }
        } catch (Exception e) {
            for (String p : text.split("[,，、\\s]+")) {
                String v = p.trim();
                if (!v.isEmpty()) {
                    set.add(v);
                }
            }
        }
        return set;
    }

    /**
     * 我当前位置（User.location，经纬度或城市文案）与活动位置（活动经纬度或地址文案）匹配。
     */
    private int calculateLocationForActivity(User me, Partner partner) {
        if (me == null || partner == null) {
            return 0;
        }
        String uLoc = me.getLocation();
        if (uLoc == null || uLoc.trim().isEmpty()) {
            log.debug("[match-score] 位置分=0：当前用户 location 为空（请落库用户位置，见 t_user.location）");
            return 0;
        }
        uLoc = uLoc.trim();
        if (partner.getLatitude() != null && partner.getLongitude() != null) {
            int s = scoreUserLocationVsPoint(uLoc, partner.getLatitude(), partner.getLongitude());
            log.debug("[match-score] 位置分(活动有经纬度)={} userLoc={} partnerLatLon={},{}",
                    s, maskLocationForLog(uLoc), partner.getLatitude(), partner.getLongitude());
            return s;
        }
        String aLoc = partner.getLocation();
        if (aLoc == null || aLoc.isEmpty()) {
            log.debug("[match-score] 位置分=0：活动无经纬度且无 location 文案");
            return 0;
        }
        aLoc = aLoc.trim();
        int s = scoreTwoLocationStrings(uLoc, aLoc);
        log.debug("[match-score] 位置分(活动仅地址文案)={}", s);
        return s;
    }

    /** 日志中避免完整暴露用户坐标，仅截断 */
    private static String maskLocationForLog(String loc) {
        if (loc == null) {
            return "";
        }
        if (loc.length() > 24) {
            return loc.substring(0, 8) + "...";
        }
        return loc;
    }

    private int scoreUserLocationVsPoint(String userLocation, double lat, double lon) {
        String[] p1 = userLocation.split(",");
        if (p1.length == 2) {
            try {
                double uLat = Double.parseDouble(p1[0].trim());
                double uLon = Double.parseDouble(p1[1].trim());
                double distKm = haversine(uLat, uLon, lat, lon);
                return mapDistanceToScore(distKm);
            } catch (NumberFormatException ignored) {
                // 用户为城市文案，仅做弱匹配
            }
        }
        return scoreTwoLocationStrings(userLocation, lat + "," + lon);
    }

    private int mapDistanceToScore(double distanceKm) {
        if (distanceKm <= 1) {
            return MAX_SCORE;
        }
        if (distanceKm >= 50) {
            return 0;
        }
        int score = (int) Math.round((1 - distanceKm / 50.0) * MAX_SCORE);
        return Math.min(MAX_SCORE, Math.max(0, score));
    }

    private int calculateLocationUserToUser(User user, User target) {
        String loc1 = user != null ? user.getLocation() : null;
        String loc2 = target != null ? target.getLocation() : null;
        if (loc1 == null || loc2 == null || loc1.isEmpty() || loc2.isEmpty()) {
            return 0;
        }
        return scoreTwoLocationStrings(loc1.trim(), loc2.trim());
    }

    private int scoreTwoLocationStrings(String loc1, String loc2) {
        if (loc1.equalsIgnoreCase(loc2)) {
            return MAX_SCORE;
        }
        String[] p1 = loc1.split(",");
        String[] p2 = loc2.split(",");
        if (p1.length == 2 && p2.length == 2) {
            try {
                double lat1 = Double.parseDouble(p1[0].trim());
                double lon1 = Double.parseDouble(p1[1].trim());
                double lat2 = Double.parseDouble(p2[0].trim());
                double lon2 = Double.parseDouble(p2[1].trim());
                return mapDistanceToScore(haversine(lat1, lon1, lat2, lon2));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        String[] parts1 = loc1.split("[\\s,/]+");
        String[] parts2 = loc2.split("[\\s,/]+");
        List<String> list1 = Arrays.asList(parts1);
        List<String> list2 = Arrays.asList(parts2);
        for (String s1 : list1) {
            if (s1 != null && !s1.isEmpty()) {
                for (String s2 : list2) {
                    if (s2 != null && !s2.isEmpty() && s1.equalsIgnoreCase(s2)) {
                        return 70;
                    }
                }
            }
        }
        return 20;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }

    private int calculateAiScore(User me,
            User publisher,
            Partner activity,
            int activityTagScore,
            int publisherTagScore,
            int locationScore) {
        try {
            String prompt = buildAiPrompt(me, publisher, activity, activityTagScore, publisherTagScore, locationScore);
            String content = dashScopeCompatClient.completeUserOnly(
                    prompt.replace("\\n", "\n"));
            if (content == null || content.isEmpty()) {
                return 0;
            }
            Pattern p = Pattern.compile("(\\d{1,3})");
            Matcher m = p.matcher(content);
            if (m.find()) {
                int v = Integer.parseInt(m.group(1));
                if (v < 0) {
                    v = 0;
                }
                if (v > MAX_SCORE) {
                    v = MAX_SCORE;
                }
                log.info("[match-score] AI 分解析成功 raw(截断)={} -> {}", truncateForLogSimple(content, 80), v);
                return v;
            }
            log.warn("[match-score] AI 分=0：返回中未含数字, snippet={}", truncateForLogSimple(content, 200));
        } catch (Exception e) {
            log.error("[match-score] AI 分计算异常", e);
        }
        return 0;
    }

    private static String truncateForLogSimple(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...(截断)";
    }

    private String buildAiPrompt(User me,
            User publisher,
            Partner activity,
            int activityTagScore,
            int publisherTagScore,
            int locationScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("请作为社交/活动搭子匹配助手，仅根据下述画像与规则分，给出一个 0-100 的「综合契合度」整数，只输出该数字。\\n\\n");
        sb.append("【当前用户(想参与的一方)】\\n");
        appendUserProfileLines(sb, me);
        sb.append("\\n【发布者】\\n");
        appendUserProfileLines(sb, publisher);
        if (activity != null) {
            sb.append("\\n【搭子/活动信息】\\n");
            sb.append("标题: ").append(safe(activity.getTitle())).append("\\n");
            sb.append("描述: ").append(safe(activity.getContent())).append("\\n");
            sb.append("偏好/标签(文本): ").append(safe(activity.getPreference())).append("\\n");
            sb.append("活动位置文案: ").append(safe(activity.getLocation())).append("\\n");
            if (activity.getPlanTime() != null) {
                sb.append("计划时间: ").append(activity.getPlanTime().toString()).append("\\n");
            }
        }
        sb.append("\\n【规则分参考】活动标签匹配(与我vs活动)=").append(activityTagScore);
        sb.append("，发布者标签匹配(与我vs发布者)=").append(publisherTagScore);
        sb.append("，位置匹配=").append(locationScore).append("。");
        sb.append("请综合价值观、内容契合度、时空可行性等，输出 0-100 的整数，不要其他文字。");
        return sb.toString();
    }

    private void appendUserProfileLines(StringBuilder sb, User u) {
        if (u == null) {
            sb.append("(无)\\n");
            return;
        }
        sb.append("昵称: ").append(safe(u.getNickname())).append("\\n");
        sb.append("性别(0未知1男2女): ").append(u.getGender() == null ? "" : String.valueOf(u.getGender()))
                .append("\\n");
        sb.append("生日: ").append(safe(u.getBirthday())).append("\\n");
        sb.append("个人简介: ").append(safe(u.getBio())).append("\\n");
        sb.append("兴趣标签: ").append(safe(u.getTags())).append("\\n");
        sb.append("常驻/当前位置(坐标或城市): ").append(safe(u.getLocation())).append("\\n");
        sb.append("公司: ").append(safe(u.getCompanyName())).append("\\n");
        sb.append("学校: ").append(safe(u.getSchoolName())).append("\\n");
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private List<String> buildSuggestions(
            int activityTagScore,
            int publisherTagScore,
            int locationScore,
            boolean hasActivity) {
        List<String> list = new ArrayList<>();
        if (hasActivity && activityTagScore < 60) {
            list.add("你的兴趣标签与本次活动偏好重合不多，可看看活动描述是否合意或补充自己的标签。");
        }
        if (publisherTagScore < 60) {
            list.add("与发布者兴趣标签可再靠齐：在个人资料里补充、更新兴趣标签。");
        }
        if (locationScore < 60) {
            list.add("完善你当前的常驻位置/定位（与活动地越近匹配越高）。");
        }
        if (list.isEmpty()) {
            list.add("继续保持，现在的资料对本次活动已有不错的匹配基础了。");
        }
        return list;
    }

    private String buildReason(MatchScoreDTO dto, boolean hasActivity) {
        StringBuilder sb = new StringBuilder();
        sb.append("综合匹配度为 ").append(dto.getTotalScore()).append(" 分，");
        if (hasActivity) {
            sb.append("其中活动标签 ").append(n(dto.getActivityTagScore())).append(" 分、");
        }
        sb.append("发布者标签 ").append(n(dto.getPublisherTagScore())).append(" 分，");
        sb.append("位置匹配 ").append(n(dto.getLocationScore())).append(" 分，");
        sb.append("AI 综合评分 ").append(n(dto.getAiScore())).append(" 分。");
        return sb.toString();
    }

    private static int n(Integer i) {
        return i == null ? 0 : i;
    }
}
