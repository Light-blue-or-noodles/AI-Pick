package com.sparklink.knowledge.model;

/**
 * 知识检索请求。
 */
public class KnowledgeRetrieveRequest {

    /**
     * 用户 ID。
     */
    private Long userId;

    /**
     * 用户查询文本。
     */
    private String query;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
