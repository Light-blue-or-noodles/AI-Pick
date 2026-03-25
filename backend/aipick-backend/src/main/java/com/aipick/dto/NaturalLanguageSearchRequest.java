package com.aipick.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 自然语言搜索请求
 *
 * @author AI-Pick
 */
@Data
public class NaturalLanguageSearchRequest {

    /** 当前用户 ID（可选，用于匹配度等；与 /search 接口共用） */
    private Long userId;

    /** 用户自然语言，如：帮我找北京周末的羽毛球活动 */
    @NotBlank(message = "query 不能为空")
    private String query;

    /** 每类结果最大条数（活动、搭子分别截断） */
    @Min(1)
    @Max(50)
    private Integer limit = 15;
}
