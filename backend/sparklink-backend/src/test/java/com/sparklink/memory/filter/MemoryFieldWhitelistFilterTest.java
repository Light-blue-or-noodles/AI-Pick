package com.sparklink.memory.filter;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryFieldWhitelistFilterTest {

    @Test
    void filterProfileForPrompt_withNullInput_returnsEmptyMap() {
        Map<String, String> filtered = MemoryFieldWhitelistFilter.filterProfileForPrompt(null);
        assertTrue(filtered.isEmpty());
    }

    @Test
    void filterProfileForPrompt_withEmptyInput_returnsEmptyMap() {
        Map<String, String> filtered = MemoryFieldWhitelistFilter.filterProfileForPrompt(Map.of());
        assertTrue(filtered.isEmpty());
    }

    @Test
    void filterProfileForPrompt_filtersSensitiveFieldsAndNonWhitelistedFields() {
        Map<String, String> profile = new LinkedHashMap<>();
        profile.put("民族", "汉族");
        profile.put("性别", "女");
        profile.put("年龄", "24");
        profile.put("家庭成员", "父母");
        profile.put("年龄段", "95后");
        profile.put("居住地", "杭州");
        profile.put("社会关系（朋友圈）", "同事为主");
        profile.put("宠物", "猫");
        profile.put("人生理想/目标", "创业");
        profile.put("饮食习惯", "清淡");
        profile.put("爱好", "羽毛球");
        profile.put("内容偏好", "科技");
        profile.put("手机号", "13800000000");

        Map<String, String> filtered = MemoryFieldWhitelistFilter.filterProfileForPrompt(profile);

        assertEquals(8, filtered.size());
        assertEquals("95后", filtered.get("年龄段"));
        assertEquals("杭州", filtered.get("居住地"));
        assertEquals("同事为主", filtered.get("社会关系（朋友圈）"));
        assertEquals("猫", filtered.get("宠物"));
        assertEquals("创业", filtered.get("人生理想/目标"));
        assertEquals("清淡", filtered.get("饮食习惯"));
        assertEquals("羽毛球", filtered.get("爱好"));
        assertEquals("科技", filtered.get("内容偏好"));
        assertTrue(!filtered.containsKey("民族"));
        assertTrue(!filtered.containsKey("性别"));
        assertTrue(!filtered.containsKey("年龄"));
        assertTrue(!filtered.containsKey("家庭成员"));
        assertTrue(!filtered.containsKey("手机号"));
    }
}
