package com.aipick.service.impl;

import com.aipick.config.AliyunContentSecurityConfig;
import com.aipick.service.ContentModerationProvider;
import com.aipick.service.ContentSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一内容审核服务实现
 * 整合本地敏感词和阿里云审核能力
 * 支持链式审核策略
 *
 * @author AI-Pick
 */
@Slf4j
@Service
public class ContentModerationServiceImpl implements ContentSecurityService {

    private final LocalSensitiveWordProvider localProvider;
    private final AliyunContentModerationProvider aliyunProvider;
    private final AliyunContentSecurityConfig config;

    public ContentModerationServiceImpl(LocalSensitiveWordProvider localProvider,
                                        AliyunContentModerationProvider aliyunProvider,
                                        AliyunContentSecurityConfig config) {
        this.localProvider = localProvider;
        this.aliyunProvider = aliyunProvider;
        this.config = config;
    }

    @Override
    public ContentCheckResult checkText(String text) {
        // 第一步：本地敏感词检测
        ContentModerationProvider.TextModerationRequest localRequest =
                new ContentModerationProvider.TextModerationRequest(text);
        ContentModerationProvider.ModerationResult localResult = localProvider.moderateText(localRequest);

        if (!localResult.isPassed()) {
            // 本地检测未通过
            log.warn("文本审核未通过 - 本地敏感词检测: {}", localResult.getMessage());
            return ContentCheckResult.failBySensitiveWord(
                    SensitiveWordResult.fail(localResult.getLabel())
            );
        }

        // 第二步：阿里云复核（如果启用）
        if (config.isEnabled() && config.isTextReviewOnSuspicious()) {
            ContentModerationProvider.TextModerationRequest aliyunRequest =
                    new ContentModerationProvider.TextModerationRequest(text);
            ContentModerationProvider.ModerationResult aliyunResult = aliyunProvider.moderateText(aliyunRequest);

            if (!aliyunResult.isPassed()) {
                log.warn("文本审核未通过 - 阿里云复核: {}", aliyunResult.getMessage());
                return ContentCheckResult.failByModeration(
                        ContentModerationResult.fail(
                                aliyunResult.getSuggestion().name().toLowerCase(),
                                aliyunResult.getLabel(),
                                aliyunResult.getConfidence()
                        )
                );
            }

            return ContentCheckResult.pass("aliyun", ContentModerationResult.pass());
        }

        // 本地检测通过且未启用阿里云复核
        return ContentCheckResult.pass("sensitive", null);
    }

    @Override
    public ContentModerationResult checkImage(String imageUrl) {
        // 图片审核直接走阿里云
        if (!config.isEnabled()) {
            log.warn("图片审核功能未启用");
            return ContentModerationResult.pass();
        }

        ContentModerationProvider.ImageModerationRequest request =
                new ContentModerationProvider.ImageModerationRequest(imageUrl);
        ContentModerationProvider.ModerationResult result = aliyunProvider.moderateImage(request);

        if (!result.isSuccess()) {
            log.error("图片审核失败: {}", result.getMessage());
            return ContentModerationResult.pass(); // 失败时默认通过
        }

        if (!result.isPassed()) {
            return ContentModerationResult.fail(
                    result.getSuggestion().name().toLowerCase(),
                    result.getLabel(),
                    result.getConfidence()
            );
        }

        return ContentModerationResult.pass();
    }

    @Override
    public SensitiveWordResult checkSensitiveWords(String text) {
        ContentModerationProvider.TextModerationRequest request =
                new ContentModerationProvider.TextModerationRequest(text);
        ContentModerationProvider.ModerationResult result = localProvider.moderateText(request);

        if (!result.isPassed()) {
            return SensitiveWordResult.fail(result.getLabel());
        }

        return SensitiveWordResult.pass();
    }

    @Override
    public List<Integer> batchCheckSensitiveWords(List<String> texts) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            SensitiveWordResult result = checkSensitiveWords(texts.get(i));
            if (result.isContainsSensitiveWord()) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * 获取审核能力状态
     */
    public ModerationCapabilities getCapabilities() {
        ModerationCapabilities capabilities = new ModerationCapabilities();
        capabilities.setLocalSensitiveWordAvailable(localProvider.isAvailable());
        capabilities.setAliyunContentSecurityAvailable(aliyunProvider.isAvailable());
        capabilities.setTextReviewStrategy(config.isTextReviewOnSuspicious() ? "本地+阿里云复核" : "仅本地");
        capabilities.setImageReviewStrategy(config.isEnabled() ? "阿里云" : "未启用");
        return capabilities;
    }

    /**
     * 审核能力状态
     */
    public static class ModerationCapabilities {
        private boolean localSensitiveWordAvailable;
        private boolean aliyunContentSecurityAvailable;
        private String textReviewStrategy;
        private String imageReviewStrategy;

        // Getters and Setters
        public boolean isLocalSensitiveWordAvailable() { return localSensitiveWordAvailable; }
        public void setLocalSensitiveWordAvailable(boolean localSensitiveWordAvailable) {
            this.localSensitiveWordAvailable = localSensitiveWordAvailable;
        }
        public boolean isAliyunContentSecurityAvailable() { return aliyunContentSecurityAvailable; }
        public void setAliyunContentSecurityAvailable(boolean aliyunContentSecurityAvailable) {
            this.aliyunContentSecurityAvailable = aliyunContentSecurityAvailable;
        }
        public String getTextReviewStrategy() { return textReviewStrategy; }
        public void setTextReviewStrategy(String textReviewStrategy) {
            this.textReviewStrategy = textReviewStrategy;
        }
        public String getImageReviewStrategy() { return imageReviewStrategy; }
        public void setImageReviewStrategy(String imageReviewStrategy) {
            this.imageReviewStrategy = imageReviewStrategy;
        }
    }
}
