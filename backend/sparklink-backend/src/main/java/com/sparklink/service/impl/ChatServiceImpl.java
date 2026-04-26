package com.sparklink.service.impl;

import cn.hutool.core.util.IdUtil;
import com.sparklink.common.AiConstants;
import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.dto.ChatRequest;
import com.sparklink.dto.ChatResponse;
import com.sparklink.dto.PageRequest;
import com.sparklink.entity.Activity;
import com.sparklink.entity.ChatMessage;
import com.sparklink.entity.Partner;
import com.sparklink.vo.PartnerVO;
import com.sparklink.mapper.ChatMessageMapper;
import com.sparklink.service.ActivityService;
import com.sparklink.service.ChatService;
import com.sparklink.service.PartnerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sparklink.integration.DashScopeCompatClient;
import com.sparklink.util.MediaPathUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 对话服务实现（阿里云百炼）
 * 支持多轮对话上下文，读取历史消息构建完整对话场景
 *
 * @author AI-Pick
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_RECOMMENDS = 6;

    private final ChatMessageMapper chatMessageMapper;
    private final PartnerService partnerService;
    private final ActivityService activityService;
    private final ObjectMapper objectMapper;
    private final DashScopeCompatClient dashScopeCompatClient;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper,
                           PartnerService partnerService,
                           ActivityService activityService,
                           ObjectMapper objectMapper,
                           DashScopeCompatClient dashScopeCompatClient) {
        this.chatMessageMapper = chatMessageMapper;
        this.partnerService = partnerService;
        this.activityService = activityService;
        this.objectMapper = objectMapper;
        this.dashScopeCompatClient = dashScopeCompatClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatResponse chat(Long userId, ChatRequest request) {
        // 获取或生成会话 ID
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = IdUtil.fastSimpleUUID();
        }

        // 保存用户消息
        ChatMessage userMessage = new ChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setType(1);
        userMessage.setContent(request.getMessage());
        chatMessageMapper.insert(userMessage);

        // 读取历史消息，构建多轮对话上下文
        List<ChatMessage> history = getHistoryMessages(sessionId, userId);

        // 根据用户诉求在系统内匹配搭子/活动，生成推荐列表
        List<ChatRecommendItem> recommends = buildRecommendsFromUserMessage(request.getMessage());
        String matchContext = buildMatchContextString(recommends);

        // 调用阿里云百炼 API（带上下文 + 仅基于系统内数据的推荐话术）
        String reply = generateReplyWithHistory(request.getMessage(), history, matchContext);

        // 保存 AI 消息
        ChatMessage aiMessage = new ChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setUserId(userId);
        aiMessage.setType(2);
        aiMessage.setContent(reply);
        chatMessageMapper.insert(aiMessage);

        return new ChatResponse(sessionId, reply, recommends);
    }

    /**
     * 根据用户消息在系统内匹配搭子与活动，返回推荐卡片列表
     * 关键词：游戏->搭子 type13，运动->搭子 type5+活动 category 运动，活动/附近->活动列表，搭子->搭子列表
     */
    private List<ChatRecommendItem> buildRecommendsFromUserMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String text = message.trim().toLowerCase();
        List<ChatRecommendItem> list = new ArrayList<>();

        PageRequest page = new PageRequest();
        page.setPageNum(1);
        page.setPageSize(MAX_RECOMMENDS);

        // 游戏搭子：partner type=13
        if (text.contains("游戏") || text.contains("游戏搭子")) {
            IPage<PartnerVO> partners = partnerService.getPartnerList(page, 13, null, null);
            for (PartnerVO p : partners.getRecords()) {
                list.add(new ChatRecommendItem("partner", p.getId(), p.getTitle(),
                        truncate(p.getDescription(), 50), toCoverPath(p.getCoverImage(), "partner"), 85));
            }
        }
        // 运动：搭子 type=5 + 活动 category=运动
        if (text.contains("运动") || text.contains("运动活动")) {
            IPage<PartnerVO> partners = partnerService.getPartnerList(page, 5, null, null);
            for (PartnerVO p : partners.getRecords()) {
                if (list.size() >= MAX_RECOMMENDS) {
                    break;
                }
                list.add(new ChatRecommendItem("partner", p.getId(), p.getTitle(),
                        truncate(p.getDescription(), 50), toCoverPath(p.getCoverImage(), "partner"), 85));
            }
            IPage<Activity> activities = activityService.getActivityList(page, null, "运动", null);
            for (Activity a : activities.getRecords()) {
                if (list.size() >= MAX_RECOMMENDS) {
                    break;
                }
                list.add(new ChatRecommendItem("activity", a.getId(), a.getTitle(),
                        truncate(a.getDescription(), 50), toCoverPath(a.getCoverImage(), "activity"), 80));
            }
        }
        // 附近/活动：查活动
        if (text.contains("附近") || text.contains("活动") || text.contains("动态")) {
            IPage<Activity> activities = activityService.getActivityList(page, null, null, null);
            for (Activity a : activities.getRecords()) {
                if (list.size() >= MAX_RECOMMENDS) {
                    break;
                }
                list.add(new ChatRecommendItem("activity", a.getId(), a.getTitle(),
                        truncate(a.getDescription(), 50), toCoverPath(a.getCoverImage(), "activity"), null));
            }
        }
        // 搭子（通用，且上面未匹配到游戏/运动时）：查搭子列表
        if (text.contains("搭子") && list.isEmpty()) {
            IPage<PartnerVO> partners = partnerService.getPartnerList(page, null, null, null);
            for (PartnerVO p : partners.getRecords()) {
                if (list.size() >= MAX_RECOMMENDS) {
                    break;
                }
                list.add(new ChatRecommendItem("partner", p.getId(), p.getTitle(),
                        truncate(p.getDescription(), 50), toCoverPath(p.getCoverImage(), "partner"), 80));
            }
        }

        return list.size() > MAX_RECOMMENDS ? new ArrayList<>(list.subList(0, MAX_RECOMMENDS)) : list;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "…";
    }

    /** 无封面时返回默认图，保证前端推荐卡片能展示图片 */
    private static String toCoverPath(String url, String kind) {
        if (url == null || url.isBlank()) {
            return "partner".equalsIgnoreCase(kind) ? "/static/covers/partner-default.png" : "/static/covers/activity-default.png";
        }
        String n = MediaPathUtil.normalizeForResponse(url);
        return n != null ? n : url;
    }

    private static String buildMatchContextString(List<ChatRecommendItem> recommends) {
        if (recommends == null || recommends.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【系统内已有数据，请仅基于以下内容用一两句话推荐，不要介绍网络或通用知识】\n");
        for (ChatRecommendItem r : recommends) {
            sb.append("- ").append("partner".equals(r.getType()) ? "搭子" : "活动")
                    .append("：").append(r.getName()).append(" ").append(r.getDesc()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public List<ChatMessage> getHistory(String sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper);
    }

    /**
     * 获取历史消息（用于构建多轮对话上下文）
     *
     * @param sessionId 会话 ID
     * @param userId    用户 ID
     * @return 历史消息列表（按时间正序）
     */
    private List<ChatMessage> getHistoryMessages(String sessionId, Long userId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .eq(ChatMessage::getUserId, userId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + AiConstants.DEFAULT_HISTORY_MESSAGE_COUNT);
        List<ChatMessage> history = chatMessageMapper.selectList(wrapper);
        // 反转为正序（旧消息在前，新消息在后）
        Collections.reverse(history);
        return history;
    }

    /**
     * 调用阿里云百炼 API 生成回复（带历史上下文；若有 matchContext 则仅基于系统内数据推荐）
     *
     * @param currentMessage 当前消息
     * @param history        历史消息列表
     * @param matchContext   系统内匹配到的搭子/活动摘要，非空时要求模型只基于此推荐
     * @return AI 回复内容
     */
    private String generateReplyWithHistory(String currentMessage, List<ChatMessage> history, String matchContext) {
        if (currentMessage == null || currentMessage.trim().isEmpty()) {
            return "请告诉我你想聊些什么？";
        }

        try {
            ArrayNode messages = objectMapper.createArrayNode();
            if (matchContext != null && !matchContext.isEmpty()) {
                ObjectNode sys = messages.addObject();
                sys.put("role", "system");
                sys.put("content", matchContext);
            }
            for (ChatMessage msg : history) {
                String role = msg.getType() == 1 ? "user" : "assistant";
                ObjectNode o = messages.addObject();
                o.put("role", role);
                o.put("content", msg.getContent() == null ? "" : msg.getContent());
            }
            ObjectNode userNode = messages.addObject();
            userNode.put("role", "user");
            userNode.put("content", currentMessage);

            String content = dashScopeCompatClient.completeMessages(messages, null);
            if (content != null && !content.isEmpty()) {
                return content;
            }
            return "AI 响应为空，请检查 DASHSCOPE_API_KEY 与百炼限流/额度";
        } catch (Exception e) {
            return "调用 AI 服务失败：" + e.getMessage();
        }
    }
}
