package com.sparklink.memory.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private static final MemoryContext EMPTY = new MemoryContext(Collections.emptyList(), Collections.emptyMap());

    private final List<String> memorySnippets;
    private final Map<String, String> profileAttributes;

    private MemoryContext(List<String> memorySnippets, Map<String, String> profileAttributes) {
        this.memorySnippets = memorySnippets == null ? Collections.emptyList() : List.copyOf(memorySnippets);
        this.profileAttributes = profileAttributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(profileAttributes));
    }

    public static MemoryContext empty() {
        return EMPTY;
    }

    public static MemoryContext of(List<String> memorySnippets, Map<String, String> profileAttributes) {
        if ((memorySnippets == null || memorySnippets.isEmpty())
                && (profileAttributes == null || profileAttributes.isEmpty())) {
            return EMPTY;
        }
        return new MemoryContext(memorySnippets, profileAttributes);
    }

    public List<String> getMemorySnippets() {
        return memorySnippets;
    }

    public Map<String, String> getProfileAttributes() {
        return profileAttributes;
    }

    public boolean isEmpty() {
        return memorySnippets.isEmpty() && profileAttributes.isEmpty();
    }

    public MemoryContext withProfileAttributes(Map<String, String> newProfileAttributes) {
        return of(this.memorySnippets, newProfileAttributes);
    }
}
