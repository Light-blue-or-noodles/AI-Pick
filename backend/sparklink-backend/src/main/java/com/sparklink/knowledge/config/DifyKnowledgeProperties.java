package com.sparklink.knowledge.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Dify 知识检索配置。
 */
@Configuration
@Validated
@ConfigurationProperties(prefix = "app.dify")
public class DifyKnowledgeProperties {

    /**
     * Dify API 基础地址。
     */
    @NotBlank(message = "app.dify.base-url 不能为空")
    private String baseUrl = "https://api.dify.ai/v1";

    /**
     * Dify API Key。
     */
    private String apiKey = "";

    /**
     * Dify 数据集 ID。
     */
    private String datasetId = "";

    /**
     * HTTP 调用超时时间（毫秒）。
     */
    @NotNull(message = "app.dify.timeout-ms 不能为空")
    @Positive(message = "app.dify.timeout-ms 必须大于 0")
    private Long timeoutMs = 6000L;

    /**
     * 检索策略配置。
     */
    @Valid
    @NotNull(message = "app.dify.retrieve 不能为空")
    private Retrieve retrieve = new Retrieve();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Retrieve getRetrieve() {
        return retrieve;
    }

    public void setRetrieve(Retrieve retrieve) {
        this.retrieve = retrieve;
    }

    /**
     * 检索参数。
     */
    public static class Retrieve {

        /**
         * 检索返回上限。
         */
        @NotNull(message = "app.dify.retrieve.top-k 不能为空")
        @Min(value = 1, message = "app.dify.retrieve.top-k 必须大于等于 1")
        private Integer topK = 6;

        /**
         * 是否启用分数阈值过滤。
         */
        private boolean thresholdEnabled = true;

        /**
         * 分数阈值。
         */
        @NotNull(message = "app.dify.retrieve.score-threshold 不能为空")
        @DecimalMin(value = "0.0", message = "app.dify.retrieve.score-threshold 必须在 [0,1] 区间")
        @DecimalMax(value = "1.0", message = "app.dify.retrieve.score-threshold 必须在 [0,1] 区间")
        private Double scoreThreshold = 0.55D;

        /**
         * Dify 检索方法。
         */
        @NotBlank(message = "app.dify.retrieve.search-method 不能为空")
        private String searchMethod = "hybrid_search";

        /**
         * 是否启用重排。
         */
        private boolean rerankingEnable = false;

        /**
         * 最大注入记录数。
         */
        @NotNull(message = "app.dify.retrieve.max-inject-records 不能为空")
        @Positive(message = "app.dify.retrieve.max-inject-records 必须大于 0")
        private Integer maxInjectRecords = 4;

        /**
         * 单条记录最大注入字符数。
         */
        @NotNull(message = "app.dify.retrieve.max-inject-chars-per-record 不能为空")
        @Positive(message = "app.dify.retrieve.max-inject-chars-per-record 必须大于 0")
        private Integer maxInjectCharsPerRecord = 400;

        public Integer getTopK() {
            return topK;
        }

        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        public boolean isThresholdEnabled() {
            return thresholdEnabled;
        }

        public void setThresholdEnabled(boolean thresholdEnabled) {
            this.thresholdEnabled = thresholdEnabled;
        }

        public Double getScoreThreshold() {
            return scoreThreshold;
        }

        public void setScoreThreshold(Double scoreThreshold) {
            this.scoreThreshold = scoreThreshold;
        }

        public String getSearchMethod() {
            return searchMethod;
        }

        public void setSearchMethod(String searchMethod) {
            this.searchMethod = searchMethod;
        }

        public boolean isRerankingEnable() {
            return rerankingEnable;
        }

        public void setRerankingEnable(boolean rerankingEnable) {
            this.rerankingEnable = rerankingEnable;
        }

        public Integer getMaxInjectRecords() {
            return maxInjectRecords;
        }

        public void setMaxInjectRecords(Integer maxInjectRecords) {
            this.maxInjectRecords = maxInjectRecords;
        }

        public Integer getMaxInjectCharsPerRecord() {
            return maxInjectCharsPerRecord;
        }

        public void setMaxInjectCharsPerRecord(Integer maxInjectCharsPerRecord) {
            this.maxInjectCharsPerRecord = maxInjectCharsPerRecord;
        }
    }
}
