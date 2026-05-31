package com.sparklink.service.impl;

import com.sparklink.dto.ActivityAiRequest;
import com.sparklink.memory.model.MemoryContext;
import com.sparklink.memory.service.MemoryFacade;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 活动文案 AI 生成与优化服务
 *
 * 使用 Spring AI Alibaba 集成的 ChatModel 调用阿里云百炼模型，
 * 根据用户填写的活动信息生成更完整的活动介绍。
 *
 * @author AI-Pick
 */
@Service
public class ActivityAiTextService {

    private final ChatModel chatModel;
    private final MemoryFacade memoryFacade;

    public ActivityAiTextService(ChatModel chatModel, MemoryFacade memoryFacade) {
        this.chatModel = chatModel;
        this.memoryFacade = memoryFacade;
    }

    /**
     * 根据活动信息生成/优化活动描述文案。
     *
     * @param req 活动 AI 请求
     * @return 优化后的描述
     */
    public String enhanceDescription(ActivityAiRequest req) {
        Long userId = resolveUserId();
        String userInput = buildUserInput(req);
        MemoryContext memoryContext = memoryFacade.recallForPrompt(userId, userInput);
        // System 提示：告知模型角色与约束
        StringBuilder system = new StringBuilder();
        system.append("你是一个帮用户润色线下活动介绍文案的助手，要求：");
        system.append("1）保持活动核心信息不变，例如运动类型、项目（如羽毛球）不能被改写成其它类型；");
        system.append("2）语气友好、真实，避免夸张营销；");
        system.append("3）仅输出最终正文，不要输出标题或多余解释。");

        StringBuilder userContent = new StringBuilder();
        userContent.append("请根据下面的活动信息，用中文生成一段 80~200 字的活动介绍文案：\n");
        String category = orEmpty(req.getCategory());
        String title = orEmpty(req.getTitle());
        String lowerTitle = title.toLowerCase();
        // 按类型/标题微调风格提示
        if (category.contains("羽毛球") || lowerTitle.contains("羽毛球")) {
            userContent.append("这是一个羽毛球运动类活动，请突出：轻松运动、挥汗放松、结识同好、适合不同水平的球友，避免提到桌游、聚餐等无关内容。\n");
        } else if (category.contains("运动")) {
            userContent.append("这是一个线下运动类活动，请突出：运动强度、安全提示、适合人群和氛围感，可适当加入热身、拉伸等信息。\n");
        } else if (category.contains("桌游") || lowerTitle.contains("桌游")) {
            userContent.append("这是一个桌游/聚会类活动，请突出：桌游类型、社交氛围、轻松交友，但不要说成剧本杀或其它不相干活动。\n");
        } else if (category.contains("露营") || lowerTitle.contains("露营")) {
            userContent.append("这是一个露营/户外类活动，请强调：自然风景、轻松氛围、安全与装备提示，避免过度宣传极限运动。\n");
        } else if (category.contains("美食") || lowerTitle.contains("美食")) {
            userContent.append("这是一个美食类活动，请多描述食物特色、氛围与交流，但不要写成运动或户外项目。\n");
        }
        userContent.append("活动信息如下：\n");
        userContent.append("标题：").append(title).append("\n");
        userContent.append("类型/分类：").append(category).append("\n");
        if (req.getLocation() != null && !req.getLocation().isBlank()) {
            userContent.append("地点：").append(req.getLocation()).append("\n");
        }
        if (req.getTimeText() != null && !req.getTimeText().isBlank()) {
            userContent.append("时间：").append(req.getTimeText()).append("\n");
        }
        if (req.getCurrentDesc() != null && !req.getCurrentDesc().isBlank()) {
            userContent.append("下面是用户自己写的初稿，请在尊重原有含义的前提下进行润色和适当补充，不要改变活动大类别，例如羽毛球活动不能改成桌游：\n");
            userContent.append(req.getCurrentDesc()).append("\n");
        }
        userContent.append("请控制在 80 到 200 字之间，不要包含标题，只输出最终的正文内容。");

        // 使用 ChatModel 简化接口：system 提示 + 用户内容拼成一个消息
        String fullPrompt = system + "\n\n用户输入：\n" + userContent;
        String mergedPrompt = memoryFacade.mergePrompt(fullPrompt, memoryContext);
        String text = chatModel.call(mergedPrompt);
        String output = text != null ? text.trim() : "";
        memoryFacade.enqueueConversation(userId, userInput, output);
        return output;
    }

    private String orEmpty(String v) {
        return v == null ? "" : v;
    }

    private static String buildUserInput(ActivityAiRequest req) {
        return "活动文案优化: title=" + safe(req.getTitle())
                + ", category=" + safe(req.getCategory())
                + ", draft=" + safe(req.getCurrentDesc());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
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
}

