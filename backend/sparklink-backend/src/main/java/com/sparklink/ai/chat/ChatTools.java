package com.sparklink.ai.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparklink.common.AiConstants;
import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.dto.PageRequest;
import com.sparklink.entity.Activity;
import com.sparklink.entity.User;
import com.sparklink.service.ActivityService;
import com.sparklink.service.PartnerService;
import com.sparklink.service.UserService;
import com.sparklink.vo.PartnerVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Spring AI 只读工具：供模型在受控白名单内检索搭子/活动与用户上下文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatTools {

    private static final int MAX_TOOL_PAGE_SIZE = ChatRecommendAssembler.MAX_RECOMMENDS;
    private static final int DEFAULT_TOOL_PAGE_SIZE = 4;

    private final PartnerService partnerService;
    private final ActivityService activityService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Tool(name = ChatIntentToolGate.TOOL_SEARCH_PARTNERS,
            description = "在系统内搜索搭子帖子。仅当用户想找搭子、约伴、开黑时使用。返回 JSON 数组摘要。")
    public String searchPartners(
            @ToolParam(description = "搭子类型码，可选：5运动 7饭搭 8旅游 13游戏 14学习", required = false) Integer type,
            @ToolParam(description = "标题或内容关键词，可选", required = false) String keyword,
            @ToolParam(description = "返回条数，1-6，默认4", required = false) Integer limit,
            ToolContext toolContext) {
        Long userId = userIdFrom(toolContext);
        int pageSize = clampLimit(limit);
        log.info("[ChatTool] searchPartners userId={} type={} keyword={} limit={}", userId, type, keyword, pageSize);

        PageRequest page = new PageRequest();
        page.setPageNum(1);
        page.setPageSize(pageSize);
        IPage<PartnerVO> partners = partnerService.getPartnerList(page, type, userId, null);

        List<Map<String, Object>> rows = new ArrayList<>();
        ChatToolExecutionContext ctx = ChatToolExecutionContext.current();
        for (PartnerVO p : partners.getRecords()) {
            if (!matchesKeyword(p.getTitle(), p.getDescription(), keyword)) {
                continue;
            }
            ChatRecommendItem item = ChatRecommendAssembler.fromPartner(p, 80);
            if (ctx != null) {
                ctx.addRecommend(item);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("title", p.getTitle());
            row.put("type", p.getTypeName());
            row.put("desc", ChatRecommendAssembler.truncate(p.getDescription(), 80));
            rows.add(row);
            if (rows.size() >= pageSize) {
                break;
            }
        }
        return toJson(rows);
    }

    @Tool(name = ChatIntentToolGate.TOOL_SEARCH_ACTIVITIES,
            description = "在系统内搜索线下活动。仅当用户想找活动、附近局、聚会时使用。返回 JSON 数组摘要。")
    public String searchActivities(
            @ToolParam(description = "活动分类，如：运动、美食、学习、娱乐", required = false) String category,
            @ToolParam(description = "标题关键词，可选", required = false) String keyword,
            @ToolParam(description = "返回条数，1-6，默认4", required = false) Integer limit,
            ToolContext toolContext) {
        Long userId = userIdFrom(toolContext);
        int pageSize = clampLimit(limit);
        log.info("[ChatTool] searchActivities userId={} category={} keyword={} limit={}", userId, category, keyword, pageSize);

        PageRequest page = new PageRequest();
        page.setPageNum(1);
        page.setPageSize(pageSize);
        IPage<Activity> activities = activityService.getActivityList(page, null, normalizeCategory(category), keyword);

        List<Map<String, Object>> rows = new ArrayList<>();
        ChatToolExecutionContext ctx = ChatToolExecutionContext.current();
        for (Activity a : activities.getRecords()) {
            ChatRecommendItem item = ChatRecommendAssembler.fromActivity(a, 80);
            if (ctx != null) {
                ctx.addRecommend(item);
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("title", a.getTitle());
            row.put("category", a.getCategory());
            row.put("desc", ChatRecommendAssembler.truncate(a.getDescription(), 80));
            rows.add(row);
        }
        return toJson(rows);
    }

    @Tool(name = ChatIntentToolGate.TOOL_GET_USER_CONTEXT,
            description = "获取当前用户画像摘要（昵称、简介、兴趣标签），用于个性化推荐话术。不返回敏感信息。")
    public String getUserContext(ToolContext toolContext) {
        Long userId = userIdFrom(toolContext);
        log.info("[ChatTool] getUserContext userId={}", userId);
        if (userId == null) {
            return "{\"loggedIn\":false}";
        }
        User user = userService.getUserInfo(userId);
        if (user == null) {
            return "{\"loggedIn\":false}";
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("loggedIn", true);
        profile.put("nickname", user.getNickname());
        profile.put("bio", ChatRecommendAssembler.truncate(user.getBio(), 120));
        profile.put("tags", user.getTags());
        return toJson(profile);
    }

    private static Long userIdFrom(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object raw = toolContext.getContext().get("userId");
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof String s && StringUtils.hasText(s)) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static int clampLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_TOOL_PAGE_SIZE;
        }
        return Math.min(limit, MAX_TOOL_PAGE_SIZE);
    }

    private static String normalizeCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        return category.trim();
    }

    private static boolean matchesKeyword(String title, String desc, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String k = keyword.trim().toLowerCase(Locale.ROOT);
        String hay = ((title == null ? "" : title) + " " + (desc == null ? "" : desc)).toLowerCase(Locale.ROOT);
        return hay.contains(k);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
