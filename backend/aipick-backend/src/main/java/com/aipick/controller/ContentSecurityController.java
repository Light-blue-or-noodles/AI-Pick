package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.service.ContentSecurityService;
import com.aipick.service.impl.ContentModerationServiceImpl;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 内容安全审核接口
 * 提供敏感词检测和内容审核功能
 *
 * @author AI-Pick
 */
@Slf4j
@RestController
@RequestMapping("/content-security")
@RequiredArgsConstructor
public class ContentSecurityController {

    private final ContentSecurityService contentSecurityService;
    private final ContentModerationServiceImpl moderationService;

    /**
     * 检测文本内容
     */
    @PostMapping("/check-text")
    public Result<ContentCheckResponse> checkText(@RequestBody TextCheckRequest request) {
        log.info("文本内容审核请求: text length={}",
                request.getText() != null ? request.getText().length() : 0);

        ContentSecurityService.ContentCheckResult result = contentSecurityService.checkText(request.getText());

        ContentCheckResponse response = new ContentCheckResponse();
        response.setPassed(result.isPassed());
        response.setCheckType(result.getCheckType());
        response.setMessage(result.getMessage());

        if (!result.isPassed()) {
            if (result.getSensitiveResult() != null) {
                response.setSensitiveWord(result.getSensitiveResult().getMatchedWord());
            }
            if (result.getModerationResult() != null) {
                response.setLabel(result.getModerationResult().getLabel());
                response.setConfidence(result.getModerationResult().getConfidence());
            }
            log.warn("文本审核未通过: {}", result.getMessage());
        }

        return Result.success(response);
    }

    /**
     * 检测图片内容
     */
    @PostMapping("/check-image")
    public Result<ImageCheckResponse> checkImage(@RequestBody ImageCheckRequest request) {
        log.info("图片内容审核请求: imageUrl={}", request.getImageUrl());

        ContentSecurityService.ContentModerationResult result =
                contentSecurityService.checkImage(request.getImageUrl());

        ImageCheckResponse response = new ImageCheckResponse();
        response.setPassed(result.isPassed());
        response.setSuggestion(result.getSuggestion());
        response.setLabel(result.getLabel());
        response.setConfidence(result.getConfidence());
        response.setMessage(result.getMessage());

        if (!result.isPassed()) {
            log.warn("图片审核未通过: label={}, confidence={}",
                    result.getLabel(), result.getConfidence());
        }

        return Result.success(response);
    }

    /**
     * 批量检测敏感词
     */
    @PostMapping("/batch-check")
    public Result<BatchCheckResponse> batchCheck(@RequestBody BatchCheckRequest request) {
        log.info("批量敏感词检测请求: text count={}",
                request.getTexts() != null ? request.getTexts().size() : 0);

        List<Integer> indices = contentSecurityService.batchCheckSensitiveWords(request.getTexts());

        BatchCheckResponse response = new BatchCheckResponse();
        response.setTotalCount(request.getTexts() != null ? request.getTexts().size() : 0);
        response.setSensitiveIndices(indices);
        response.setSensitiveCount(indices.size());

        return Result.success(response);
    }

    /**
     * 获取内容安全配置状态
     */
    @GetMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        ContentModerationServiceImpl.ModerationCapabilities capabilities =
                moderationService.getCapabilities();

        Map<String, Object> status = Map.of(
                "localSensitiveWordFilter", capabilities.isLocalSensitiveWordAvailable(),
                "aliyunContentSecurity", capabilities.isAliyunContentSecurityAvailable(),
                "textReviewStrategy", capabilities.getTextReviewStrategy(),
                "imageReviewStrategy", capabilities.getImageReviewStrategy()
        );
        return Result.success(status);
    }

    // ========== 请求/响应类 ==========

    @Data
    public static class TextCheckRequest {
        private String text;
    }

    @Data
    public static class ImageCheckRequest {
        private String imageUrl;
    }

    @Data
    public static class BatchCheckRequest {
        private List<String> texts;
    }

    @Data
    public static class ContentCheckResponse {
        private boolean passed;
        private String checkType;
        private String message;
        private String sensitiveWord;
        private String label;
        private Float confidence;
    }

    @Data
    public static class ImageCheckResponse {
        private boolean passed;
        private String suggestion;
        private String label;
        private Float confidence;
        private String message;
    }

    @Data
    public static class BatchCheckResponse {
        private int totalCount;
        private int sensitiveCount;
        private List<Integer> sensitiveIndices;
    }
}
