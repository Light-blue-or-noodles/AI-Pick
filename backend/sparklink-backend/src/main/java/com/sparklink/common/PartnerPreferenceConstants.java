package com.sparklink.common;

import com.sparklink.common.BusinessException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 搭子偏好：可选标签白名单（与小程序发布页一致），存库为英文逗号分隔。
 */
public final class PartnerPreferenceConstants {

    /**
     * 固定顺序，供展示与校验
     */
    private static final List<String> ORDERED_LABELS = Arrays.asList(
            "金主请客",
            "AA制",
            "省钱局",
            "自驾分摊",
            "自备装备",
            "随缘",
            "欢迎e人",
            "i人友好",
            "拒绝尬聊",
            "扩列",
            "只要同好",
            "小团体群聊",
            "禁加微信",
            "只要男生",
            "只要女生",
            "同龄局",
            "学生局",
            "老乡局",
            "异地勿扰",
            "通宵局",
            "限工作日",
            "周末搭子",
            "长期固定",
            "早鸟局",
            "午休快闪",
            "高端局",
            "轻奢品质",
            "宠物友好",
            "技能交换",
            "旅行计划",
            "暴汗局",
            "大咖分享",
            "学习搭子",
            "小饮怡情",
            "玄学",
            "养生局"
    );

    private static final Set<String> ALLOWED = Collections.unmodifiableSet(new LinkedHashSet<>(ORDERED_LABELS));

    private static final int MAX_TAGS = 20;

    private PartnerPreferenceConstants() {
    }

    public static List<String> orderedLabels() {
        return ORDERED_LABELS;
    }

    public static boolean isAllowed(String label) {
        return label != null && ALLOWED.contains(label.trim());
    }

    /**
     * 解析、去重、校验后返回英文逗号拼接串
     */
    public static String normalizeAndValidate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException("请选择搭子偏好");
        }
        String[] parts = raw.split("[,，、\\s]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p == null) {
                continue;
            }
            String t = p.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (!ALLOWED.contains(t)) {
                throw new BusinessException("无效的偏好标签：" + t);
            }
            if (!out.contains(t)) {
                out.add(t);
            }
        }
        if (out.isEmpty()) {
            throw new BusinessException("请选择搭子偏好");
        }
        if (out.size() > MAX_TAGS) {
            throw new BusinessException("最多选择 " + MAX_TAGS + " 个偏好标签");
        }
        return String.join(",", out);
    }
}
