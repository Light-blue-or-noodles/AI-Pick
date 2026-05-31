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
     * 相似度阈值。
     */
    private Double similarityThreshold = 0.6D;

    /**
     * 最大返回条数。
     */
    private Integer maxResults = 8;

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
}
