package com.sparklink.memory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Memory Library 配置。
 *
 * @author AI-Pick
 */
@Configuration
@ConfigurationProperties(prefix = "memory.library")
public class MemoryLibraryProperties {

    /**
     * 是否启用记忆库能力。
     */
    private boolean enabled = true;

    /**
     * 记忆库 ID。
     */
    private String knowledgebaseId = "5eb944ee565c400d8fb71e6ab119dfe3";

    /**
     * 阿里云 API Key。
     */
    private String apiKey = "";

    /**
     * 记忆库 API 基础地址。
     */
    private String baseUrl = "https://dashscope.aliyuncs.com/api/v2/apps/memory";

    /**
     * 相似度阈值。
     */
    private Double similarityThreshold = 0.6D;

    /**
     * 最大返回条数。
     */
    private Integer maxResults = 8;

    /**
     * 记忆身份环境标识（例如 dev、prod）。
     */
    private String environmentTag = "";

    /**
     * 是否启用消费者轮询。
     */
    private boolean consumerEnabled = true;

    /**
     * 轮询间隔毫秒。
     */
    private Long consumerPollIntervalMs = 2000L;

    /**
     * 单次轮询批量上限。
     */
    private Integer consumerBatchSize = 50;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKnowledgebaseId() {
        return knowledgebaseId;
    }

    public void setKnowledgebaseId(String knowledgebaseId) {
        this.knowledgebaseId = knowledgebaseId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(Double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(Integer maxResults) {
        this.maxResults = maxResults;
    }

    public String getEnvironmentTag() {
        return environmentTag;
    }

    public void setEnvironmentTag(String environmentTag) {
        this.environmentTag = environmentTag;
    }

    public boolean isConsumerEnabled() {
        return consumerEnabled;
    }

    public void setConsumerEnabled(boolean consumerEnabled) {
        this.consumerEnabled = consumerEnabled;
    }

    public Long getConsumerPollIntervalMs() {
        return consumerPollIntervalMs;
    }

    public void setConsumerPollIntervalMs(Long consumerPollIntervalMs) {
        this.consumerPollIntervalMs = consumerPollIntervalMs;
    }

    public Integer getConsumerBatchSize() {
        return consumerBatchSize;
    }

    public void setConsumerBatchSize(Integer consumerBatchSize) {
        this.consumerBatchSize = consumerBatchSize;
    }
}
