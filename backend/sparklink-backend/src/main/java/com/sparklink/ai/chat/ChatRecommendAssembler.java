package com.sparklink.ai.chat;

import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.entity.Activity;
import com.sparklink.util.MediaPathUtil;
import com.sparklink.vo.PartnerVO;

/**
 * 将业务实体映射为对话推荐卡片。
 */
public final class ChatRecommendAssembler {

    public static final int MAX_RECOMMENDS = 6;

    private ChatRecommendAssembler() {
    }

    public static ChatRecommendItem fromPartner(PartnerVO p, Integer match) {
        if (p == null || p.getId() == null) {
            return null;
        }
        return new ChatRecommendItem(
                "partner",
                p.getId(),
                p.getTitle(),
                truncate(p.getDescription(), 50),
                toCoverPath(p.getCoverImage(), "partner"),
                match);
    }

    public static ChatRecommendItem fromActivity(Activity a, Integer match) {
        if (a == null || a.getId() == null) {
            return null;
        }
        return new ChatRecommendItem(
                "activity",
                a.getId(),
                a.getTitle(),
                truncate(a.getDescription(), 50),
                toCoverPath(a.getCoverImage(), "activity"),
                match);
    }

    public static String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "…";
    }

    public static String toCoverPath(String url, String kind) {
        if (url == null || url.isBlank()) {
            return "partner".equalsIgnoreCase(kind)
                    ? "/static/covers/partner-default.png"
                    : "/static/covers/activity-default.png";
        }
        String n = MediaPathUtil.normalizeForResponse(url);
        return n != null ? n : url;
    }

    public static String buildMatchContextString(java.util.List<ChatRecommendItem> recommends) {
        if (recommends == null || recommends.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【系统内已有数据，请仅基于以下内容用一两句话推荐，不要介绍网络或通用知识】\n");
        for (ChatRecommendItem r : recommends) {
            sb.append("- ").append("partner".equals(r.getType()) ? "搭子" : "活动")
                    .append("：").append(r.getName()).append(" ").append(r.getDesc()).append("\n");
        }
        return sb.toString();
    }
}
