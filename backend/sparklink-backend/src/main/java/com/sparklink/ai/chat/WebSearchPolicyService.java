package com.sparklink.ai.chat;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 联网搜索触发策略（auto/on/off）。
 */
@Service
public class WebSearchPolicyService {

    private static final Set<String> RECENCY_KEYWORDS = Set.of(
            "今天", "今日", "昨天", "刚刚", "现在", "目前", "最新", "最近", "实时", "新闻", "天气"
    );
    private static final Set<String> MARKET_QUOTE_KEYWORDS = Set.of(
            "股价", "股票", "行情", "汇率", "金价", "币价"
    );

    private final WebSearchProperties webSearchProperties;

    public WebSearchPolicyService(WebSearchProperties webSearchProperties) {
        this.webSearchProperties = webSearchProperties;
    }

    public Decision shouldSearch(String rawMode, String message) {
        if (!webSearchProperties.isEnabled()) {
            return Decision.notTriggered("config_disabled");
        }

        WebSearchMode mode = WebSearchMode.from(rawMode);
        if (mode == WebSearchMode.ON) {
            return Decision.triggered("mode_on");
        }
        if (mode == WebSearchMode.OFF) {
            return Decision.notTriggered("mode_off");
        }

        String safeMessage = message == null ? "" : message;
        boolean hit = RECENCY_KEYWORDS.stream().anyMatch(safeMessage::contains)
                || MARKET_QUOTE_KEYWORDS.stream().anyMatch(safeMessage::contains);
        if (hit) {
            return Decision.triggered("auto_keyword");
        }
        return Decision.notTriggered("auto_skip");
    }

    public record Decision(boolean triggered, String reason) {
        public static Decision triggered(String reason) {
            return new Decision(true, reason);
        }

        public static Decision notTriggered(String reason) {
            return new Decision(false, reason);
        }
    }
}
