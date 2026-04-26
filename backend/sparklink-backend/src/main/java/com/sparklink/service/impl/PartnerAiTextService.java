package com.sparklink.service.impl;

import com.sparklink.dto.PartnerAiRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * 搭子详情文案 AI 润色（≤300 字）
 */
@Service
public class PartnerAiTextService {

    private final ChatModel chatModel;

    public PartnerAiTextService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String enhanceDescription(PartnerAiRequest req) {
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
        String text = chatModel.call(fullPrompt);
        if (text == null) {
            return "";
        }
        text = text.trim();
        if (text.length() > 300) {
            return text.substring(0, 300);
        }
        return text;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
