package com.sparklink.dto;

/**
 * 对话联网搜索元信息
 *
 * @author AI-Pick
 */
public class ChatSearchMeta {

    /** 本次是否触发联网搜索 */
    private boolean triggered;

    /** 触发/未触发原因 */
    private String reason;

    /** 被过滤掉的结果条数 */
    private int filteredCount;

    public ChatSearchMeta() {
    }

    public ChatSearchMeta(boolean triggered, String reason, int filteredCount) {
        this.triggered = triggered;
        this.reason = reason;
        this.filteredCount = filteredCount;
    }

    public static ChatSearchMeta triggered(String reason, int filteredCount) {
        return new ChatSearchMeta(true, reason, filteredCount);
    }

    public static ChatSearchMeta notTriggered(String reason, int filteredCount) {
        return new ChatSearchMeta(false, reason, filteredCount);
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public int getFilteredCount() {
        return filteredCount;
    }

    public void setFilteredCount(int filteredCount) {
        this.filteredCount = filteredCount;
    }
}
