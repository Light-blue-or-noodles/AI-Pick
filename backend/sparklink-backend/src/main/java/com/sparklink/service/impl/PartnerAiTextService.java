package com.sparklink.service.impl;

import com.sparklink.dto.PartnerAiRequest;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.service.MemoryFacade;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 搭子详情文案 AI 润色（≤300 字）
 */
@Service
@Slf4j
public class PartnerAiTextService {

    private final ChatModel chatModel;
    private final MemoryFacade memoryFacade;

    public PartnerAiTextService(ChatModel chatModel, MemoryFacade memoryFacade) {
        this.chatModel = chatModel;
        this.memoryFacade = memoryFacade;
    }

    public String enhanceDescription(PartnerAiRequest req) {
        Long userId = resolveUserId();
        String userInput = buildUserInput(req);
        MemoryContext memoryContext = safeRecall(userId, userInput);
        StringBuilder system = new StringBuilder();
        system.append("你是帮用户写「找搭子」详情介绍的助手。要求：");
        system.append("1）根据标题、类型、偏好扩展为一段自然、真诚的说明；");
        system.append("2）不要编造具体地点、时间、人数，除非用户初稿已写；");
        system.append("3）语气友好，避免夸张营销；");
        system.append("4）只输出正文，不要标题或解释。");

        StringBuilder userContent = new StringBuilder();
        userContent.append("请用中文写一段搭子详情介绍，严格控制在 280 字以内：\n");
        userContent.append("标题：").append(nullToEmpty(req.getTitle())).append("\n");
        userContent.append("搭子类型：").append(nullToEmpty(req.getTypeName())).append("\n");
        userContent.append("偏好：").append(nullToEmpty(req.getPreference())).append("\n");
        if (req.getPlanTimeHint() != null && !req.getPlanTimeHint().isBlank()) {
            userContent.append("计划/集合时间：").append(req.getPlanTimeHint().trim()).append("\n");
        }
        if (req.getLocationHint() != null && !req.getLocationHint().isBlank()) {
            userContent.append("地点：").append(req.getLocationHint().trim()).append("\n");
        }
        if (req.getCurrentDesc() != null && !req.getCurrentDesc().isBlank()) {
            userContent.append("用户初稿（请润色、可补充，勿改变核心意图）：\n");
            userContent.append(req.getCurrentDesc().trim()).append("\n");
        }
        userContent.append("输出一段即可，不要分段标题。");

        String fullPrompt = system + "\n\n用户输入：\n" + userContent;
        String mergedPrompt = safeMerge(fullPrompt, memoryContext, userId);
        String text = chatModel.call(mergedPrompt);
        if (text == null) {
            safeEnqueue(userId, userInput, "");
            return "";
        }
        text = text.trim();
        if (text.length() > 300) {
            String truncated = text.substring(0, 300);
            safeEnqueue(userId, userInput, truncated);
            return truncated;
        }
        safeEnqueue(userId, userInput, text);
        return text;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String buildUserInput(PartnerAiRequest req) {
        return "搭子文案优化: title=" + nullToEmpty(req.getTitle()).trim()
                + ", type=" + nullToEmpty(req.getTypeName()).trim()
                + ", preference=" + nullToEmpty(req.getPreference()).trim()
                + ", draft=" + nullToEmpty(req.getCurrentDesc()).trim();
    }

    private static Long resolveUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private MemoryContext safeRecall(Long userId, String userInput) {
        try {
            return memoryFacade.recallForPrompt(userId, userInput);
        } catch (Exception ex) {
            log.warn("memory recall failed in partner-ai, userId={}, reason={}", userId, ex.getMessage());
            return null;
        }
    }

    private String safeMerge(String prompt, MemoryContext memoryContext, Long userId) {
        try {
            return memoryFacade.mergePrompt(prompt, memoryContext);
        } catch (Exception ex) {
            log.warn("memory merge failed in partner-ai, userId={}, reason={}", userId, ex.getMessage());
            return prompt;
        }
    }

    private void safeEnqueue(Long userId, String userInput, String modelOutput) {
        try {
            memoryFacade.enqueueConversation(userId, userInput, modelOutput);
        } catch (Exception ex) {
            log.warn("memory enqueue failed in partner-ai, userId={}, reason={}", userId, ex.getMessage());
        }
    }
}
