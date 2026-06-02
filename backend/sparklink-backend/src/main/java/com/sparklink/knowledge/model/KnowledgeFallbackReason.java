package com.sparklink.knowledge.model;

/**
 * 知识检索降级原因。
 */
public enum KnowledgeFallbackReason {

    /**
     * 未触发检索。
     */
    NOT_TRIGGERED,

    /**
     * 检索失败。
     */
    SEARCH_FAILED
}
