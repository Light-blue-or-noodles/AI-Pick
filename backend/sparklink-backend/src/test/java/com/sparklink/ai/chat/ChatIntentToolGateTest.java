package com.sparklink.ai.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatIntentToolGateTest {

    private ChatIntentToolGate gate;

    @BeforeEach
    void setUp() {
        gate = new ChatIntentToolGate();
    }

    @Test
    void classifyPartnerIntent() {
        assertEquals(ChatIntent.PARTNER, gate.classify("帮我找游戏搭子"));
    }

    @Test
    void classifyActivityIntent() {
        assertEquals(ChatIntent.ACTIVITY, gate.classify("附近有什么活动"));
    }

    @Test
    void classifyMixedIntent() {
        assertEquals(ChatIntent.MIXED, gate.classify("想找运动搭子和活动"));
    }

    @Test
    void classifyProfileIntent() {
        assertEquals(ChatIntent.PROFILE, gate.classify("怎么优化个人资料"));
    }

    @Test
    void allowedToolsForPartner() {
        Set<String> tools = gate.allowedToolNames(ChatIntent.PARTNER);
        assertTrue(tools.contains(ChatIntentToolGate.TOOL_SEARCH_PARTNERS));
        assertTrue(tools.contains(ChatIntentToolGate.TOOL_GET_USER_CONTEXT));
        assertFalse(tools.contains(ChatIntentToolGate.TOOL_SEARCH_ACTIVITIES));
    }

    @Test
    void allowedToolsForGeneralIsEmpty() {
        assertTrue(gate.allowedToolNames(ChatIntent.GENERAL).isEmpty());
    }

    @Test
    void requiresRetrieval() {
        assertTrue(gate.requiresRetrieval(ChatIntent.MIXED));
        assertFalse(gate.requiresRetrieval(ChatIntent.GENERAL));
    }
}
