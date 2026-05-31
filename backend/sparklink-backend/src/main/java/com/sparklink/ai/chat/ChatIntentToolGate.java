package com.sparklink.ai.chat;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 受控编排：根据用户消息判定意图，并决定本次允许模型调用的工具白名单。
 */
@Component
public class ChatIntentToolGate {

    public static final String TOOL_SEARCH_PARTNERS = "searchPartners";
    public static final String TOOL_SEARCH_ACTIVITIES = "searchActivities";
    public static final String TOOL_GET_USER_CONTEXT = "getUserContext";

    /**
     * 基于关键词的规则意图分类（确定性，便于测试与灰度）。
     */
    public ChatIntent classify(String message) {
        if (!StringUtils.hasText(message)) {
            return ChatIntent.GENERAL;
        }
        String text = message.trim().toLowerCase(Locale.ROOT);
        boolean partner = text.contains("搭子") || text.contains("找伴") || text.contains("开黑")
                || text.contains("游戏搭子");
        boolean activity = text.contains("活动") || text.contains("附近") || text.contains("动态")
                || text.contains("线下") || text.contains("聚会");
        boolean profile = text.contains("资料") || text.contains("头像") || text.contains("昵称")
                || text.contains("个人") || text.contains("简介");

        if (profile && !partner && !activity) {
            return ChatIntent.PROFILE;
        }
        if (partner && activity) {
            return ChatIntent.MIXED;
        }
        if (partner) {
            return ChatIntent.PARTNER;
        }
        if (activity) {
            return ChatIntent.ACTIVITY;
        }
        if (text.contains("运动") || text.contains("游戏")) {
            return ChatIntent.MIXED;
        }
        return ChatIntent.GENERAL;
    }

    /**
     * 返回本次会话允许注册给模型的工具名（有序、去重）。
     */
    public Set<String> allowedToolNames(ChatIntent intent) {
        if (intent == null) {
            return Collections.emptySet();
        }
        Set<String> names = new LinkedHashSet<>();
        switch (intent) {
            case PARTNER -> {
                names.add(TOOL_SEARCH_PARTNERS);
                names.add(TOOL_GET_USER_CONTEXT);
            }
            case ACTIVITY -> {
                names.add(TOOL_SEARCH_ACTIVITIES);
                names.add(TOOL_GET_USER_CONTEXT);
            }
            case MIXED -> {
                names.add(TOOL_SEARCH_PARTNERS);
                names.add(TOOL_SEARCH_ACTIVITIES);
                names.add(TOOL_GET_USER_CONTEXT);
            }
            case PROFILE -> names.add(TOOL_GET_USER_CONTEXT);
            case GENERAL -> {
                // 通用对话不开放检索工具，避免模型乱查
            }
            default -> {
            }
        }
        return names;
    }

    public boolean requiresRetrieval(ChatIntent intent) {
        return intent != null && EnumSet.of(ChatIntent.PARTNER, ChatIntent.ACTIVITY, ChatIntent.MIXED).contains(intent);
    }
}
