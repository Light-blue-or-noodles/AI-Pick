package com.sparklink.ai.chat;

import com.sparklink.dto.ChatRecommendItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 单次对话请求内的工具执行上下文：收集工具召回结果供前端卡片展示。
 */
public final class ChatToolExecutionContext {

    private static final ThreadLocal<ChatToolExecutionContext> HOLDER = new ThreadLocal<>();

    private final List<ChatRecommendItem> recommends = new CopyOnWriteArrayList<>();

    private ChatToolExecutionContext() {
    }

    public static ChatToolExecutionContext begin() {
        ChatToolExecutionContext ctx = new ChatToolExecutionContext();
        HOLDER.set(ctx);
        return ctx;
    }

    public static ChatToolExecutionContext current() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public void addRecommend(ChatRecommendItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        recommends.add(item);
    }

    public List<ChatRecommendItem> getRecommendsSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(recommends));
    }
}
