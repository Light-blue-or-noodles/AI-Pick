package com.aipick.service.impl;

import com.aipick.dto.MatchScoreDTO;
import com.aipick.entity.User;
import com.aipick.mapper.UserMapper;
import com.aipick.service.MatchScoreService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 匹配度计算实现类
 *
 * 维度：
 * - 兴趣标签匹配（40%）
 * - 地理位置匹配（30%）
 * - 时间偏好匹配（20%）
 * - AI 评分（10%）
 *
 * @author AI-Pick
 */
@Service
public class MatchScoreServiceImpl implements MatchScoreService {

    private static final int MAX_SCORE = 100;

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    public MatchScoreServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchScoreDTO calculateMatchScore(Long userId, Long targetId) {
        User user = userMapper.selectById(userId);
        User target = userMapper.selectById(targetId);
        if (user == null || target == null) {
            MatchScoreDTO dto = new MatchScoreDTO();
            dto.setUserId(userId);
            dto.setTargetId(targetId);
            dto.setTotalScore(0);
            dto.setInterestScore(0);
            dto.setLocationScore(0);
            dto.setTimeScore(0);
            dto.setAiScore(0);
            dto.setReason("用户不存在，无法计算匹配度");
            dto.setSuggestions(new ArrayList<>());
            return dto;
        }

        int interestScore = calculateInterestScore(user, target);
        int locationScore = calculateLocationScore(user, target);
        int timeScore = calculateTimeScore(user, target);
        int aiScore = calculateAiScore(user, target, interestScore, locationScore, timeScore);

        int totalScore = (int) Math.round(
                interestScore * 0.4 +
                        locationScore * 0.3 +
                        timeScore * 0.2 +
                        aiScore * 0.1
        );

        MatchScoreDTO dto = new MatchScoreDTO();
        dto.setUserId(userId);
        dto.setTargetId(targetId);
        dto.setInterestScore(interestScore);
        dto.setLocationScore(locationScore);
        dto.setTimeScore(timeScore);
        dto.setAiScore(aiScore);
        dto.setTotalScore(Math.min(MAX_SCORE, Math.max(0, totalScore)));

        List<String> suggestions = buildSuggestions(interestScore, locationScore, timeScore);
        dto.setSuggestions(suggestions);
        dto.setReason(buildReason(dto));

        return dto;
    }

    private int calculateInterestScore(User user, User target) {
        Set<String> userTags = parseTags(user.getTags());
        Set<String> targetTags = parseTags(target.getTags());
        if (userTags.isEmpty() || targetTags.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(userTags);
        intersection.retainAll(targetTags);
        if (intersection.isEmpty()) {
            return 0;
        }

        int common = intersection.size();
        int base = Math.max(userTags.size(), targetTags.size());
        if (base <= 0) {
            return 0;
        }
        int score = (int) Math.round(common * 1.0 / base * MAX_SCORE);
        return Math.min(MAX_SCORE, Math.max(0, score));
    }

    private Set<String> parseTags(String jsonArray) {
        Set<String> set = new HashSet<>();
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            return set;
        }
        String text = jsonArray.trim();
        // 简单兼容：["游戏","运动"] 或 逗号分隔字符串
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
                String[] parts = text.split(",");
                for (String p : parts) {
                    String v = p.trim();
                    if (!v.isEmpty()) {
                        set.add(v);
                    }
                }
            }
        } catch (Exception e) {
            String[] parts = text.split(",");
            for (String p : parts) {
                String v = p.trim();
                if (!v.isEmpty()) {
                    set.add(v);
                }
            }
        }
        return set;
    }

    private int calculateLocationScore(User user, User target) {
        String loc1 = user != null ? user.getLocation() : null;
        String loc2 = target != null ? target.getLocation() : null;
        if (loc1 == null || loc2 == null || loc1.isEmpty() || loc2.isEmpty()) {
            return 0;
        }

        loc1 = loc1.trim();
        loc2 = loc2.trim();
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
                double distanceKm = haversine(lat1, lon1, lat2, lon2);
                if (distanceKm <= 1) {
                    return MAX_SCORE;
                }
                if (distanceKm >= 50) {
                    return 0;
                }
                int score = (int) Math.round((1 - distanceKm / 50.0) * MAX_SCORE);
                return Math.min(MAX_SCORE, Math.max(0, score));
            } catch (NumberFormatException ignored) {
                // fall through to string-based heuristic
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
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private int calculateTimeScore(User user, User target) {
        String t1 = user != null ? user.getActiveTime() : null;
        String t2 = target != null ? target.getActiveTime() : null;
        if (t1 == null || t2 == null || t1.isEmpty() || t2.isEmpty()) {
            return 0;
        }
        TimeRange r1 = parseTimeRange(t1);
        TimeRange r2 = parseTimeRange(t2);
        if (r1 == null || r2 == null) {
            return 0;
        }
        LocalTime start = r1.start.isAfter(r2.start) ? r1.start : r2.start;
        LocalTime end = r1.end.isBefore(r2.end) ? r1.end : r2.end;
        if (!end.isAfter(start)) {
            return 0;
        }
        long overlapMinutes = java.time.Duration.between(start, end).toMinutes();
        long fullDayMinutes = 24 * 60;
        if (overlapMinutes <= 0) {
            return 0;
        }
        int score = (int) Math.round(overlapMinutes * 1.0 / fullDayMinutes * MAX_SCORE);
        return Math.min(MAX_SCORE, Math.max(0, score));
    }

    private TimeRange parseTimeRange(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String startStr = node.path("start").asText(null);
            String endStr = node.path("end").asText(null);
            if (startStr == null || endStr == null) {
                return null;
            }
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = LocalTime.parse(endStr);
            return new TimeRange(start, end);
        } catch (Exception e) {
            return null;
        }
    }

    private int calculateAiScore(User user, User target,
                                 int interestScore,
                                 int locationScore,
                                 int timeScore) {
        if (apiKey == null || apiKey.isEmpty()) {
            return 0;
        }
        try {
            String prompt = buildAiPrompt(user, target, interestScore, locationScore, timeScore);
            List<String> messages = new ArrayList<>();
            String escaped = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            messages.add(String.format("{\"role\":\"user\",\"content\":\"%s\"}", escaped));

            String requestBody = String.format(
                    "{\"model\":\"qwen3.5-plus\",\"input\":{\"messages\":[%s]}}",
                    String.join(",", messages)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
                    entity,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return 0;
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode output = root.path("output");
            String content = output.path("text").asText();
            if (content.isEmpty()) {
                JsonNode choices = output.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode first = choices.get(0);
                    JsonNode msg = first.path("message");
                    content = msg.path("content").asText();
                }
            }
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
                return v;
            }
        } catch (Exception ignored) {
            return 0;
        }
        return 0;
    }

    private String buildAiPrompt(User user, User target,
                                 int interestScore,
                                 int locationScore,
                                 int timeScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("请作为社交匹配助手，根据以下两位用户的信息给出一个 0-100 的匹配度整数评分，只输出数字：").append("\\n\\n");
        sb.append("用户A: 昵称=").append(safe(user.getNickname()))
                .append(", 兴趣标签=").append(safe(user.getTags()))
                .append(", 位置=").append(safe(user.getLocation()))
                .append(", 活跃时间=").append(safe(user.getActiveTime())).append("\\n");
        sb.append("用户B: 昵称=").append(safe(target.getNickname()))
                .append(", 兴趣标签=").append(safe(target.getTags()))
                .append(", 位置=").append(safe(target.getLocation()))
                .append(", 活跃时间=").append(safe(target.getActiveTime())).append("\\n\\n");
        sb.append("系统基于规则的初步打分为：兴趣匹配=").append(interestScore)
                .append(", 位置匹配=").append(locationScore)
                .append(", 时间匹配=").append(timeScore).append("。");
        sb.append("请综合这些信息，给出一个 0-100 的匹配度整数，只输出这个数字，不要附加任何文字。");
        return sb.toString();
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private List<String> buildSuggestions(int interestScore, int locationScore, int timeScore) {
        List<String> list = new ArrayList<>();
        if (interestScore < 60) {
            list.add("多补充一些具体的兴趣标签，提高兴趣匹配度。");
        }
        if (locationScore < 60) {
            list.add("完善或更新你的常驻城市/活动范围，方便匹配同城或临近用户。");
        }
        if (timeScore < 60) {
            list.add("在个人资料中设置更清晰的活跃时间段，方便找到作息相近的搭子。");
        }
        if (list.isEmpty()) {
            list.add("继续保持，现在的资料已经有不错的匹配基础了。");
        }
        return list;
    }

    private String buildReason(MatchScoreDTO dto) {
        StringBuilder sb = new StringBuilder();
        sb.append("综合匹配度为 ").append(dto.getTotalScore()).append(" 分，");
        sb.append("其中兴趣匹配 ").append(dto.getInterestScore()).append(" 分，");
        sb.append("位置匹配 ").append(dto.getLocationScore()).append(" 分，");
        sb.append("时间匹配 ").append(dto.getTimeScore()).append(" 分，");
        sb.append("AI 综合评分 ").append(dto.getAiScore()).append(" 分。");
        return sb.toString();
    }

    private static final class TimeRange {
        private final LocalTime start;
        private final LocalTime end;

        private TimeRange(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
        }
    }
}

