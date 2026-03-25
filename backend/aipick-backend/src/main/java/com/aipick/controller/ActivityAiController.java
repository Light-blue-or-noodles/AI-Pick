package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.dto.ActivityAiRequest;
import com.aipick.service.impl.ActivityAiTextService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动文案 AI 相关接口
 *
 * 提供基于 Spring AI Alibaba 的真实 AI 生成功能。
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/activity/ai")
public class ActivityAiController {

    private final ActivityAiTextService aiTextService;

    public ActivityAiController(ActivityAiTextService aiTextService) {
        this.aiTextService = aiTextService;
    }

    /**
     * 生成/优化活动详情文案。
     *
     * @param request 活动信息与用户初稿
     * @return 优化后的描述文本
     */
    @PostMapping("/description")
    public Result<String> enhanceDescription(@Valid @RequestBody ActivityAiRequest request) {
        String text = aiTextService.enhanceDescription(request);
        return Result.success("生成成功", text);
    }
}

