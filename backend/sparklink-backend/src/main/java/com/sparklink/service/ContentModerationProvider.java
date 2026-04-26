package com.sparklink.service;

import java.util.List;

/**
 * 内容审核能力提供者接口
 * 统一封装不同审核能力（文本、图片、视频等）的底层实现
 * 便于后期升级和替换具体实现
 *
 * @author AI-Pick
 */
public interface ContentModerationProvider {

    /**
     * 获取提供者名称
     */
    String getProviderName();

    /**
     * 审核文本内容
     */
    ModerationResult moderateText(TextModerationRequest request);

    /**
     * 批量审核文本内容
     */
    List<ModerationResult> moderateTextBatch(List<TextModerationRequest> requests);

    /**
     * 审核图片内容
     */
    ModerationResult moderateImage(ImageModerationRequest request);

    /**
     * 批量审核图片内容
     */
    List<ModerationResult> moderateImageBatch(List<ImageModerationRequest> requests);

    /**
     * 检查提供者是否可用
     */
    boolean isAvailable();

    // ==================== 统一数据模型 ====================

    enum ContentType { TEXT, IMAGE, VIDEO, AUDIO }

    enum Suggestion { PASS, REVIEW, BLOCK }

    enum RiskLevel { NONE, LOW, MEDIUM, HIGH }

    class TextModerationRequest {
        private String contentId;
        private String text;
        private String scene;
        private String dataId;

        public TextModerationRequest(String text) {
            this.text = text;
            this.dataId = java.util.UUID.randomUUID().toString();
        }

        public String getContentId() { return contentId; }
        public void setContentId(String contentId) { this.contentId = contentId; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getScene() { return scene; }
        public void setScene(String scene) { this.scene = scene; }
        public String getDataId() { return dataId; }
        public void setDataId(String dataId) { this.dataId = dataId; }
    }

    class ImageModerationRequest {
        private String contentId;
        private String imageUrl;
        private String imageData;
        private String scene;
        private String dataId;

        public ImageModerationRequest(String imageUrl) {
            this.imageUrl = imageUrl;
            this.dataId = java.util.UUID.randomUUID().toString();
        }

        public String getContentId() { return contentId; }
        public void setContentId(String contentId) { this.contentId = contentId; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getImageData() { return imageData; }
        public void setImageData(String imageData) { this.imageData = imageData; }
        public String getScene() { return scene; }
        public void setScene(String scene) { this.scene = scene; }
        public String getDataId() { return dataId; }
        public void setDataId(String dataId) { this.dataId = dataId; }
    }

    class ModerationResult {
        private boolean success;
        private String providerName;
        private ContentType contentType;
        private Suggestion suggestion;
        private RiskLevel riskLevel;
        private float confidence;
        private String label;
        private String message;
        private String dataId;

        public static ModerationResult pass(String providerName, ContentType contentType) {
            ModerationResult r = new ModerationResult();
            r.success = true;
            r.providerName = providerName;
            r.contentType = contentType;
            r.suggestion = Suggestion.PASS;
            r.riskLevel = RiskLevel.NONE;
            r.confidence = 0f;
            r.message = "审核通过";
            return r;
        }

        public static ModerationResult block(String providerName, ContentType contentType,
                                              String label, float confidence, String message) {
            ModerationResult r = new ModerationResult();
            r.success = true;
            r.providerName = providerName;
            r.contentType = contentType;
            r.suggestion = Suggestion.BLOCK;
            r.riskLevel = RiskLevel.HIGH;
            r.confidence = confidence;
            r.label = label;
            r.message = message;
            return r;
        }

        public static ModerationResult error(String providerName, String message) {
            ModerationResult r = new ModerationResult();
            r.success = false;
            r.providerName = providerName;
            r.message = message;
            return r;
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getProviderName() { return providerName; }
        public void setProviderName(String providerName) { this.providerName = providerName; }
        public ContentType getContentType() { return contentType; }
        public void setContentType(ContentType contentType) { this.contentType = contentType; }
        public Suggestion getSuggestion() { return suggestion; }
        public void setSuggestion(Suggestion suggestion) { this.suggestion = suggestion; }
        public RiskLevel getRiskLevel() { return riskLevel; }
        public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getDataId() { return dataId; }
        public void setDataId(String dataId) { this.dataId = dataId; }

        public boolean isPassed() {
            return success && suggestion == Suggestion.PASS;
        }
    }
}
