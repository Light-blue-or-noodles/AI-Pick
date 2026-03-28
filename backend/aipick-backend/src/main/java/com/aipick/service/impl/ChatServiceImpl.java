package com.aipick.service.impl;

import cn.hutool.core.util.IdUtil;
import com.aipick.common.AiConstants;
import com.aipick.dto.ChatRecommendItem;
import com.aipick.dto.ChatRequest;
import com.aipick.dto.ChatResponse;
import com.aipick.dto.PageRequest;
import com.aipick.entity.Activity;
import com.aipick.entity.ChatMessage;
import com.aipick.entity.Partner;
import com.aipick.vo.PartnerVO;
import com.aipick.mapper.ChatMessageMapper;
import com.aipick.service.ActivityService;
import com.aipick.service.ChatService;
import com.aipick.service.PartnerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper,
                           PartnerService partnerService,
                           ActivityService activityService) {
        this.chatMessageMapper = chatMessageMapper;
        this.partnerService = partnerService;
        this.activityService = activityService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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
        return url.startsWith("http") ? url : url;
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

        if (apiKey == null || apiKey.isEmpty()) {
            return "AI 服务未配置 API Key，请设置 DASHSCOPE_API_KEY 环境变量或配置 spring.ai.dashscope.api-key";
        }

        try {
            // 构建消息列表（包含历史上下文）
            List<String> messages = new ArrayList<>();

            // 若有系统内匹配数据，先注入系统提示：只基于以下列表推荐，不要网络解释
            if (matchContext != null && !matchContext.isEmpty()) {
                String sysContent = matchContext
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                messages.add(String.format("{\"role\":\"system\",\"content\":\"%s\"}", sysContent));
            }

            // 添加历史消息
            for (ChatMessage msg : history) {
                String role = msg.getType() == 1 ? "user" : "assistant";
                String content = msg.getContent()
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t");
                messages.add(String.format("{\"role\":\"%s\",\"content\":\"%s\"}", role, content));
            }

            // 添加当前消息
            String currentContent = currentMessage
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
            messages.add(String.format("{\"role\":\"user\",\"content\":\"%s\"}", currentContent));

            // 构建请求体
            String requestBody = String.format(
                    "{\"model\":\"qwen-max\",\"input\":{\"messages\":[%s]}}",
                    String.join(",", messages)
            );

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation",
                    entity,
                    String.class
            );

            // 解析响应
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode output = root.path("output");
                // 兼容两种响应格式：新版 output.text，旧版 output.choices[0].message.content
                String content = output.path("text").asText();
                if (content.isEmpty()) {
                    JsonNode choices = output.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode firstChoice = choices.get(0);
                        JsonNode msg = firstChoice.path("message");
                        content = msg.path("content").asText();
                    }
                }
                if (!content.isEmpty()) {
                    return content;
                }
            }

            return "AI 响应格式异常";
        } catch (Exception e) {
            return "调用 AI 服务失败：" + e.getMessage();
        }
    }
}
