package com.sparklink.ai.chat;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.dto.PageRequest;
import com.sparklink.entity.Activity;
import com.sparklink.service.ActivityService;
import com.sparklink.service.PartnerService;
import com.sparklink.vo.PartnerVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRecommendFallbackServiceTest {

    @Mock
    private PartnerService partnerService;

    @Mock
    private ActivityService activityService;

    private ChatRecommendFallbackService fallbackService;

    @BeforeEach
    void setUp() {
        fallbackService = new ChatRecommendFallbackService(partnerService, activityService);
    }

    @Test
    void recallFromMessageReturnsEmptyForBlank() {
        assertTrue(fallbackService.recallFromMessage("  ").isEmpty());
    }

    @Test
    void recallGamePartners() {
        PartnerVO partner = new PartnerVO();
        partner.setId(1L);
        partner.setTitle("开黑");

        Page<PartnerVO> page = new Page<>(1, 6);
        page.setRecords(List.of(partner));
        when(partnerService.getPartnerList(ArgumentMatchers.any(PageRequest.class), ArgumentMatchers.eq(13),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull())).thenReturn(page);

        List<ChatRecommendItem> items = fallbackService.recallFromMessage("帮我找游戏搭子");
        assertEquals(1, items.size());
        assertEquals("partner", items.get(0).getType());
        assertEquals(1L, items.get(0).getId());
    }

    @Test
    void recallActivitiesForNearbyKeyword() {
        Activity activity = new Activity();
        activity.setId(9L);
        activity.setTitle("周末羽毛球");

        Page<Activity> page = new Page<>(1, 6);
        page.setRecords(List.of(activity));
        when(activityService.getActivityList(ArgumentMatchers.any(PageRequest.class), ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(), ArgumentMatchers.isNull())).thenReturn(page);

        List<ChatRecommendItem> items = fallbackService.recallFromMessage("附近有什么活动");
        assertEquals(1, items.size());
        assertEquals("activity", items.get(0).getType());
    }
}
