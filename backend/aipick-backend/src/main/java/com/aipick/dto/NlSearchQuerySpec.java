package com.aipick.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 自然语言解析后的结构化查询条件（供数据库检索）
 *
 * @author AI-Pick
 */
@Data
public class NlSearchQuerySpec {

    /** 意图：activity / partner / both */
    private String intent = "both";

    /** 地点关键词（模糊匹配 location 字段） */
    private String locationKeyword;

    /** 活动分类（与 t_activity.category 一致时精确匹配） */
    private String category;

    /** 标题/描述关键词 */
    private List<String> keywords = new ArrayList<>();

    /** 是否限定周末（与 startTimeFrom/End 二选一或叠加） */
    private Boolean weekendOnly;

    /** 活动开始时间下限 */
    private LocalDateTime startTimeFrom;

    /** 活动开始时间上限 */
    private LocalDateTime startTimeTo;

    /** 搭子类型 1-6，可为空 */
    private Integer partnerType;

    /** 模型原始 JSON（排错用，可选） */
    private String rawJson;

    /** 解析失败或降级时的说明 */
    private String parseNote;
}
