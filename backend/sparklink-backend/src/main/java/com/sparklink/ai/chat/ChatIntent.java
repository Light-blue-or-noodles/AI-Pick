package com.sparklink.ai.chat;

/**
 * AI 对话意图（受控工具编排用）
 */
public enum ChatIntent {

    /** 找搭子 */
    PARTNER,

    /** 找活动 */
    ACTIVITY,

    /** 搭子 + 活动混合诉求 */
    MIXED,

    /** 个人资料 / 优化资料 */
    PROFILE,

    /** 通用闲聊或帮助，不触发检索工具 */
    GENERAL
}
