package com.sparklink.common;

/**
 * AI 服务常量类
 * 提取魔法数字，便于统一管理和配置
 *
 * @author AI-Pick
 */
public class AiConstants {

    private AiConstants() {
        // 防止实例化
    }

    // ==================== 推荐数量限制 ====================
    /** 默认推荐搭子数量 */
    public static final int DEFAULT_PARTNER_LIMIT = 5;
    
    /** 默认推荐活动数量 */
    public static final int DEFAULT_ACTIVITY_LIMIT = 5;
    
    /** 查询最大限制数量 */
    public static final int MAX_QUERY_LIMIT = 50;

    /** 探索模式：以该概率召回与用户主兴趣不同的类型（增加发现性） */
    public static final double EXPLORE_PROBABILITY = 0.10;

    /** 送入百炼模型打分的最大候选条数（控制 token） */
    public static final int AI_RANK_MAX_CANDIDATES = 36;

    /** 多样性：目标覆盖的不同搭子类型数（上限受推荐条数限制） */
    public static final int DIVERSITY_TARGET_PARTNER_TYPES = 3;

    /** 多样性：目标覆盖的不同活动分类数 */
    public static final int DIVERSITY_TARGET_ACTIVITY_CATEGORIES = 3;

    /** 多样性：已选结果中同类型重复惩罚（加到分数上，越大越不爱重复） */
    public static final int DIVERSITY_DUPLICATE_PENALTY = 12;

    // ==================== 匹配评分基准 ====================
    /** 基础匹配分数 */
    public static final int BASE_MATCH_SCORE = 60;
    
    /** 兴趣类型匹配加分 */
    public static final int INTEREST_TYPE_MATCH_BONUS = 25;
    
    /** 时间临近（3 天内）加分 */
    public static final int TIME_NEAR_BONUS = 15;
    
    /** 时间较近（7 天内）加分 */
    public static final int TIME_MODERATE_BONUS = 8;
    
    /** 最大匹配分数 */
    public static final int MAX_MATCH_SCORE = 99;

    // ==================== 时间阈值（天） ====================
    /** 时间临近阈值 */
    public static final int TIME_NEAR_DAYS = 3;
    
    /** 时间较近阈值 */
    public static final int TIME_MODERATE_DAYS = 7;

    // ==================== AI 对话配置 ====================
    /** 默认历史消息加载数量 */
    public static final int DEFAULT_HISTORY_MESSAGE_COUNT = 10;
    
    /** 最大历史消息加载数量 */
    public static final int MAX_HISTORY_MESSAGE_COUNT = 20;

    /** AI 对话工具调用超时（毫秒） */
    public static final long CHAT_TOOL_TIMEOUT_MS = 25_000L;
}
