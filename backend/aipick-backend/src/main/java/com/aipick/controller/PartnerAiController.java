package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.dto.PartnerAiRequest;
import com.aipick.service.impl.PartnerAiTextService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搭子详情 AI 润色
 */
@RestController
@RequestMapping("/partner/ai")
public class PartnerAiController {

    private final PartnerAiTextService partnerAiTextService;

    public PartnerAiController(PartnerAiTextService partnerAiTextService) {
        this.partnerAiTextService = partnerAiTextService;
    }

    @PostMapping("/description")
    public Result<String> enhanceDescription(@Valid @RequestBody PartnerAiRequest request) {
        String text = partnerAiTextService.enhanceDescription(request);
        return Result.success("生成成功", text);
    }
}
