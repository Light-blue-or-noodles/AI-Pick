package com.sparklink.memory.model;

import java.util.Set;

public final class MemoryContext {

    public static final Set<String> SENSITIVE_PROFILE_FIELDS = Set.of(
            "民族",
            "性别",
            "年龄",
            "家庭成员"
    );

    public static final Set<String> PROMPT_PROFILE_WHITELIST_FIELDS = Set.of(
            "年龄段",
            "居住地",
            "社会关系（朋友圈）",
            "宠物",
            "人生理想/目标",
            "人生理想",
            "人生目标",
            "饮食习惯",
            "爱好",
            "内容偏好"
    );

    private MemoryContext() {
    }
}
