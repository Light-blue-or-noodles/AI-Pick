package com.sparklink.ai.chat;

import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.dto.PageRequest;
import com.sparklink.entity.Activity;
import com.sparklink.service.ActivityService;
import com.sparklink.service.PartnerService;
import com.sparklink.vo.PartnerVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 规则召回 fallback：关键词匹配系统内搭子/活动（与原 ChatServiceImpl 逻辑一致）。
 */
@Service
public class ChatRecommendFallbackService {

    private final PartnerService partnerService;
    private final ActivityService activityService;

    public ChatRecommendFallbackService(PartnerService partnerService, ActivityService activityService) {
        this.partnerService = partnerService;
        this.activityService = activityService;
    }

    /**
     * 根据用户消息在系统内匹配搭子与活动，返回推荐卡片列表。
     */
    public List<ChatRecommendItem> recallFromMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String text = message.trim().toLowerCase(Locale.ROOT);
        List<ChatRecommendItem> list = new ArrayList<>();

        PageRequest page = new PageRequest();
        page.setPageNum(1);
        page.setPageSize(ChatRecommendAssembler.MAX_RECOMMENDS);

        if (text.contains("游戏") || text.contains("游戏搭子")) {
            IPage<PartnerVO> partners = partnerService.getPartnerList(page, 13, null, null);
            for (PartnerVO p : partners.getRecords()) {
                list.add(ChatRecommendAssembler.fromPartner(p, 85));
            }
        }
        if (text.contains("运动") || text.contains("运动活动")) {
            IPage<PartnerVO> partners = partnerService.getPartnerList(page, 5, null, null);
            for (PartnerVO p : partners.getRecords()) {
                if (list.size() >= ChatRecommendAssembler.MAX_RECOMMENDS) {
                    break;
                }
                list.add(ChatRecommendAssembler.fromPartner(p, 85));
            }
            IPage<Activity> activities = activityService.getActivityList(page, null, "运动", null);
            for (Activity a : activities.getRecords()) {
                if (list.size() >= ChatRecommendAssembler.MAX_RECOMMENDS) {
                    break;
                }
                list.add(ChatRecommendAssembler.fromActivity(a, 80));
            }
        }
        if (text.contains("附近") || text.contains("活动") || text.contains("动态")) {
            IPage<Activity> activities = activityService.getActivityList(page, null, null, null);
            for (Activity a : activities.getRecords()) {
                if (list.size() >= ChatRecommendAssembler.MAX_RECOMMENDS) {
                    break;
                }
                list.add(ChatRecommendAssembler.fromActivity(a, null));
            }
        }
        if (text.contains("搭子") && list.isEmpty()) {
            IPage<PartnerVO> partners = partnerService.getPartnerList(page, null, null, null);
            for (PartnerVO p : partners.getRecords()) {
                if (list.size() >= ChatRecommendAssembler.MAX_RECOMMENDS) {
                    break;
                }
                list.add(ChatRecommendAssembler.fromPartner(p, 80));
            }
        }

        if (list.size() > ChatRecommendAssembler.MAX_RECOMMENDS) {
            return new ArrayList<>(list.subList(0, ChatRecommendAssembler.MAX_RECOMMENDS));
        }
        return list;
    }
}
