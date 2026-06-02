package com.sparklink.ai.chat;

import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatContractWebSearchTest {

    @Test
    void chatResponseShouldKeepBackwardCompatibility() {
        ChatResponse response = new ChatResponse("s1", "ok");
        assertNotNull(response.getRecommends());
        assertTrue(response.getRecommends().isEmpty());

        assertNotNull(response.getCitations());
        assertTrue(response.getCitations().isEmpty());

        assertNotNull(response.getSearchMeta());
        assertFalse(response.getSearchMeta().isTriggered());
        assertEquals("not_evaluated", response.getSearchMeta().getReason());
        assertEquals(0, response.getSearchMeta().getFilteredCount());
    }

    @Test
    void chatRequestShouldParseWebSearchMode() {
        ChatRequest req = new ChatRequest();
        req.setMessage("今天北京天气");
        assertEquals("auto", req.getWebSearchMode());

        req.setWebSearchMode("on");
        assertEquals("on", req.getWebSearchMode());
    }

    @Test
    void chatResponseConstructorWithRecommendsShouldRemainCompatible() {
        List<ChatRecommendItem> recommends = List.of(new ChatRecommendItem());
        ChatResponse response = new ChatResponse("s1", "ok", recommends);

        assertSame(recommends, response.getRecommends());
        assertTrue(response.getCitations().isEmpty());
        assertFalse(response.getSearchMeta().isTriggered());
    }
}
