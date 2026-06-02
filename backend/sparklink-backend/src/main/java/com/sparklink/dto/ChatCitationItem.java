package com.sparklink.dto;

/**
 * 对话联网引用来源项
 *
 * @author AI-Pick
 */
public class ChatCitationItem {

    /** 来源标题 */
    private String title;

    /** 来源链接 */
    private String url;

    /** 来源域名 */
    private String domain;

    /** 来源摘要 */
    private String snippet;

    /** 发布时间（可选） */
    private String publishedAt;

    public ChatCitationItem() {
    }

    public ChatCitationItem(String title, String url, String domain, String snippet, String publishedAt) {
        this.title = title;
        this.url = url;
        this.domain = domain;
        this.snippet = snippet;
        this.publishedAt = publishedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }
}
