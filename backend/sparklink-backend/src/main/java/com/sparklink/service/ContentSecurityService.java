package com.sparklink.service;

import java.util.List;

/**
 * 内容安全审核服务接口
 * 提供敏感词过滤和阿里云内容安全审核功能
 *
 * @author AI-Pick
 */
public interface ContentSecurityService {

    /**
     * 敏感词检测结果
     */
    class SensitiveWordResult {
        private final boolean containsSensitiveWord;
        private final String matchedWord;
        private final String message;

        public SensitiveWordResult(boolean containsSensitiveWord, String matchedWord, String message) {
            this.containsSensitiveWord = containsSensitiveWord;
            this.matchedWord = matchedWord;
            this.message = message;
        }

        public boolean isContainsSensitiveWord() {
            return containsSensitiveWord;
        }

        public String getMatchedWord() {
            return matchedWord;
        }

        public String getMessage() {
            return message;
        }

        public static SensitiveWordResult pass() {
            return new SensitiveWordResult(false, null, "检测通过");
        }

        public static SensitiveWordResult fail(String matchedWord) {
            return new SensitiveWordResult(true, matchedWord, "内容包含敏感词: " + matchedWord);
        }
    }

    /**
     * 阿里云内容安全审核结果
     */
    class ContentModerationResult {
        private final boolean passed;
        private final String suggestion;
        private final String label;
        private final Float confidence;
        private final String message;

        public ContentModerationResult(boolean passed, String suggestion, String label, Float confidence, String message) {
            this.passed = passed;
            this.suggestion = suggestion;
            this.label = label;
            this.confidence = confidence;
            this.message = message;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public String getLabel() {
            return label;
        }

        public Float getConfidence() {
            return confidence;
        }

        public String getMessage() {
            return message;
        }

        public static ContentModerationResult pass() {
            return new ContentModerationResult(true, "pass", null, null, "审核通过");
        }

        public static ContentModerationResult fail(String suggestion, String label, Float confidence) {
            String msg = "内容违规";
            if (label != null) {
                msg += " [" + label + "]";
            }
            return new ContentModerationResult(false, suggestion, label, confidence, msg);
        }
    }

    /**
     * 审核文本内容（敏感词 + 阿里云）
     *
     * @param text 待审核文本
     * @return 审核结果，如果敏感词检测未通过则返回敏感词结果，否则返回阿里云审核结果
     */
    ContentCheckResult checkText(String text);

    /**
     * 审核图片内容（阿里云）
     *
     * @param imageUrl 图片URL
     * @return 审核结果
     */
    ContentModerationResult checkImage(String imageUrl);

    /**
     * 仅检测敏感词
     *
     * @param text 待检测文本
     * @return 敏感词检测结果
     */
    SensitiveWordResult checkSensitiveWords(String text);

    /**
     * 批量检测敏感词
     *
     * @param texts 待检测文本列表
     * @return 包含敏感词的文本索引列表，如果没有则返回空列表
     */
    List<Integer> batchCheckSensitiveWords(List<String> texts);

    /**
     * 内容审核结果（组合敏感词和阿里云审核）
     */
    class ContentCheckResult {
        private final boolean passed;
        private final String checkType; // "sensitive" 或 "aliyun"
        private final SensitiveWordResult sensitiveResult;
        private final ContentModerationResult moderationResult;
        private final String message;

        public ContentCheckResult(boolean passed, String checkType, SensitiveWordResult sensitiveResult,
                                  ContentModerationResult moderationResult, String message) {
            this.passed = passed;
            this.checkType = checkType;
            this.sensitiveResult = sensitiveResult;
            this.moderationResult = moderationResult;
            this.message = message;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getCheckType() {
            return checkType;
        }

        public SensitiveWordResult getSensitiveResult() {
            return sensitiveResult;
        }

        public ContentModerationResult getModerationResult() {
            return moderationResult;
        }

        public String getMessage() {
            return message;
        }

        public static ContentCheckResult pass(String checkType, ContentModerationResult moderationResult) {
            return new ContentCheckResult(true, checkType, null, moderationResult, "审核通过");
        }

        public static ContentCheckResult failBySensitiveWord(SensitiveWordResult sensitiveResult) {
            return new ContentCheckResult(false, "sensitive", sensitiveResult, null, sensitiveResult.getMessage());
        }

        public static ContentCheckResult failByModeration(ContentModerationResult moderationResult) {
            return new ContentCheckResult(false, "aliyun", null, moderationResult, moderationResult.getMessage());
        }
    }
}
