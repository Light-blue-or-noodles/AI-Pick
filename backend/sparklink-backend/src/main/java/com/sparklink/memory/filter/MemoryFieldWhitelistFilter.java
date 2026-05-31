package com.sparklink.memory.filter;

import com.sparklink.memory.model.MemoryContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MemoryFieldWhitelistFilter {

    private MemoryFieldWhitelistFilter() {
    }

    public static Map<String, String> filterProfileForPrompt(Map<String, String> profile) {
        if (profile == null || profile.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : profile.entrySet()) {
            String fieldName = entry.getKey();
            if (fieldName == null) {
                continue;
            }
            if (MemoryContext.SENSITIVE_PROFILE_FIELDS.contains(fieldName)) {
                continue;
            }
            if (MemoryContext.PROMPT_PROFILE_WHITELIST_FIELDS.contains(fieldName)) {
                filtered.put(fieldName, entry.getValue());
            }
        }
        return filtered;
    }
}
