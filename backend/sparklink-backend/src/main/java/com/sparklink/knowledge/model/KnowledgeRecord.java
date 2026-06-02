package com.sparklink.knowledge.model;

/**
 * 知识记录项。
 */
public class KnowledgeRecord {

    /**
     * 片段 ID。
     */
    private String segmentId;

    /**
     * 文档 ID。
     */
    private String documentId;

    /**
     * 文本内容。
     */
    private String content;

    /**
     * 相似度分数。
     */
    private Double score;

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
