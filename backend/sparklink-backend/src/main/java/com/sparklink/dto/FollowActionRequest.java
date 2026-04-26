package com.sparklink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 关注/取消关注请求DTO
 *
 * @author AI-Pick
 */
@Data
public class FollowActionRequest {

    /**
     * 操作类型
     * follow - 关注
     * cancel - 取消关注
     */
    @NotBlank(message = "操作类型不能为空")
    @Pattern(regexp = "^(follow|cancel)$", message = "操作类型只能是 follow 或 cancel")
    private String action;
}
