package com.sparklink.ai.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSearchPolicyServiceTest {

    private WebSearchPolicyService policyService;
    private WebSearchProperties properties;

    @BeforeEach
    void setUp() {
        properties = new WebSearchProperties();
        properties.setEnabled(true);
        policyService = new WebSearchPolicyService(properties);
    }

    @Test
    void shouldForceOnWhenModeOn() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("on", "你好");
        assertTrue(decision.triggered());
        assertEquals("mode_on", decision.reason());
    }

    @Test
    void shouldDisableWhenModeOff() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("off", "今天新闻");
        assertFalse(decision.triggered());
        assertEquals("mode_off", decision.reason());
    }

    @Test
    void shouldTriggerByRecencyKeywordInAuto() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("auto", "今天北京天气怎么样");
        assertTrue(decision.triggered());
        assertEquals("auto_keyword", decision.reason());
    }

    @Test
    void shouldTriggerByMarketQuoteKeywordInAuto() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("auto", "查询一下美团的股价");
        assertTrue(decision.triggered());
        assertEquals("auto_keyword", decision.reason());
    }

    @Test
    void shouldNotTriggerForSmallTalkInAuto() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("auto", "你好呀");
        assertFalse(decision.triggered());
        assertEquals("auto_skip", decision.reason());
    }

    @Test
    void shouldReturnConfigDisabledWhenFeatureOff() {
        properties.setEnabled(false);
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("on", "今天新闻");
        assertFalse(decision.triggered());
        assertEquals("config_disabled", decision.reason());
    }

    @Test
    void shouldFallbackToAutoWhenModeIsNull() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch(null, "今天新闻");
        assertTrue(decision.triggered());
        assertEquals("auto_keyword", decision.reason());
    }

    @Test
    void shouldFallbackToAutoWhenModeIsBlank() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("   ", "今天新闻");
        assertTrue(decision.triggered());
        assertEquals("auto_keyword", decision.reason());
    }

    @Test
    void shouldFallbackToAutoWhenModeIsUnknown() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("maybe", "今天新闻");
        assertTrue(decision.triggered());
        assertEquals("auto_keyword", decision.reason());
    }

    @Test
    void shouldHandleMixedCaseMode() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("On", "你好");
        assertTrue(decision.triggered());
        assertEquals("mode_on", decision.reason());
    }

    @Test
    void shouldBeStableWhenMessageIsNullInAuto() {
        WebSearchPolicyService.Decision decision = policyService.shouldSearch("auto", null);
        assertFalse(decision.triggered());
        assertEquals("auto_skip", decision.reason());
    }
}
