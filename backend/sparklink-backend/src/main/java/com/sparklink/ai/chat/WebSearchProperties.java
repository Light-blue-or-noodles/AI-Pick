package com.sparklink.ai.chat;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 联网搜索配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.chat.web-search")
public class WebSearchProperties {

    /** 是否启用联网搜索能力 */
    private boolean enabled = true;

    /** 调用超时（毫秒） */
    private long timeoutMs = 5_000L;

    /** 最大返回结果数 */
    private int maxResults = 5;

    /** 域名白名单（空表示不过滤） */
    private List<String> domainWhitelist = new ArrayList<>();

    /** 域名黑名单 */
    private List<String> domainBlacklist = new ArrayList<>();
}
