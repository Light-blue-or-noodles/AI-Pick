package com.aipick.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话推荐项（搭子/活动卡片）
 *
 * @author AI-Pick
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRecommendItem {

    /** 类型：partner / activity */
    private String type;

    /** 主键 ID */
    private Long id;

    /** 标题/名称 */
    private String name;

    /** 描述（搭子内容摘要/活动描述摘要） */
    private String desc;

    /** 封面/头像 URL（相对路径，前端拼接 baseUrl） */
    private String avatar;

    /** 匹配度 0-100，可选 */
    private Integer match;
}
