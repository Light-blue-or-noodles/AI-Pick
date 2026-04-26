package com.sparklink.common;

/**
 * 推荐反馈类型与目标类型常量
 *
 * @author AI-Pick
 */
public final class RecommendFeedbackConstants {

    private RecommendFeedbackConstants() {
    }

    /** 反馈：跳过 */
    public static final int FEEDBACK_SKIP = 1;

    /** 反馈：聊聊（正向） */
    public static final int FEEDBACK_CHAT = 2;

    /** 反馈：举报 */
    public static final int FEEDBACK_REPORT = 3;

    /** 反馈：不合（负向，与跳过类似用于降权/过滤） */
    public static final int FEEDBACK_NOT_COMPATIBLE = 4;

    /** 目标：搭子帖 */
    public static final int TARGET_PARTNER = 1;

    /** 目标：活动 */
    public static final int TARGET_ACTIVITY = 2;
}
