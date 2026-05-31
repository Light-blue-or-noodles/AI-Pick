package com.sparklink.ai.chat;

import com.sparklink.common.AiConstants;
import com.sparklink.dto.ChatRecommendItem;
import com.sparklink.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 受控 Spring AI Function Calling 编排：意图门禁 → 白名单工具 → 生成回复并收集推荐卡片。
 */
@Slf4j
@Service
public class ChatToolOrchestrator {

    private static final String SYSTEM_PROMPT = """
            你是 SparkLink 社交小助手，帮助用户找搭子、发现活动、优化资料。
            规则：
            1）需要推荐时，必须先调用已提供的工具检索系统内真实数据，禁止编造不存在的搭子或活动；
            2）工具无结果时如实说明，并给出下一步建议；
            3）回复简洁友好，1-3 段，不要输出 JSON；
            4）仅基于工具返回内容推荐，不要引用外部平台或虚构活动。
            """;

    private final ChatIntentToolGate intentToolGate;
    private final ChatTools chatTools;
    private final ChatClient chatClient;
    private final ChatAiProperties chatAiProperties;

    public ChatToolOrchestrator(ChatIntentToolGate intentToolGate,
                                ChatTools chatTools,
                                ObjectProvider<ChatClient> chatClientProvider,
                                ChatAiProperties chatAiProperties) {
        this.intentToolGate = intentToolGate;
        this.chatTools = chatTools;
        this.chatClient = chatClientProvider.getIfAvailable();
        this.chatAiProperties = chatAiProperties;
    }

    /**
     * @return 编排结果；{@code usedTools=false} 表示应走 fallback
     */
    public OrchestrationResult orchestrate(Long userId, String currentMessage, List<ChatMessage> history) {
        if (!chatAiProperties.isToolsEnabled() || chatClient == null || !StringUtils.hasText(currentMessage)) {
            return OrchestrationResult.skip();
        }

        ChatIntent intent = intentToolGate.classify(currentMessage);
        Set<String> allowedTools = intentToolGate.allowedToolNames(intent);
        if (allowedTools.isEmpty()) {
            log.debug("[ChatOrchestrator] intent={} no tools allowed, skip tools path", intent);
            return OrchestrationResult.skip();
        }

        ChatToolExecutionContext.begin();
        try {
            Map<String, Object> toolContext = new HashMap<>();
            if (userId != null) {
                toolContext.put("userId", userId);
            }

            long timeoutMs = Math.max(1_000L, chatAiProperties.getToolsTimeoutMs());
            String reply = CompletableFuture.supplyAsync(() -> chatClient.prompt()
                            .system(SYSTEM_PROMPT)
                            .messages(toSpringAiMessages(history))
                            .user(currentMessage)
                            .tools(chatTools)
                            .toolNames(allowedTools.toArray(new String[0]))
                            .toolContext(toolContext)
                            .call()
                            .content())
                    .get(timeoutMs, TimeUnit.MILLISECONDS);

            List<ChatRecommendItem> recommends = dedupeRecommends(
                    ChatToolExecutionContext.current() != null
                            ? ChatToolExecutionContext.current().getRecommendsSnapshot()
                            : List.of());

            if (!StringUtils.hasText(reply)) {
                log.warn("[ChatOrchestrator] empty reply from ChatClient, fallback");
                return OrchestrationResult.skip();
            }

            log.info("[ChatOrchestrator] intent={} tools={} recommends={}", intent, allowedTools, recommends.size());
            return OrchestrationResult.success(reply.trim(), recommends);
        } catch (TimeoutException e) {
            log.warn("[ChatOrchestrator] tools path timeout after {}ms, fallback",
                    chatAiProperties.getToolsTimeoutMs());
            return OrchestrationResult.skip();
        } catch (Exception e) {
            log.warn("[ChatOrchestrator] tools path failed, fallback: {}", e.getMessage());
            return OrchestrationResult.skip();
        } finally {
            ChatToolExecutionContext.clear();
        }
    }

    private static List<Message> toSpringAiMessages(List<ChatMessage> history) {
        List<Message> messages = new ArrayList<>();
        if (history == null) {
            return messages;
        }
        int from = Math.max(0, history.size() - AiConstants.DEFAULT_HISTORY_MESSAGE_COUNT);
        for (ChatMessage msg : history.subList(from, history.size())) {
            String content = msg.getContent() == null ? "" : msg.getContent();
            if (msg.getType() != null && msg.getType() == 1) {
                messages.add(new UserMessage(content));
            } else {
                messages.add(new AssistantMessage(content));
            }
        }
        return messages;
    }

    private static List<ChatRecommendItem> dedupeRecommends(List<ChatRecommendItem> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new LinkedHashSet<>();
        List<ChatRecommendItem> out = new ArrayList<>();
        for (ChatRecommendItem item : raw) {
            if (item == null || item.getId() == null) {
                continue;
            }
            String key = item.getType() + ":" + item.getId();
            if (seen.add(key)) {
                out.add(item);
            }
            if (out.size() >= ChatRecommendAssembler.MAX_RECOMMENDS) {
                break;
            }
        }
        return out;
    }

    public record OrchestrationResult(boolean usedTools, String reply, List<ChatRecommendItem> recommends) {

        static OrchestrationResult skip() {
            return new OrchestrationResult(false, null, List.of());
        }

        static OrchestrationResult success(String reply, List<ChatRecommendItem> recommends) {
            return new OrchestrationResult(true, reply, recommends != null ? recommends : List.of());
        }
    }
}
