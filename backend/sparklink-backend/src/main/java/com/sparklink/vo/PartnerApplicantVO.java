package com.sparklink.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搭子应征者视图对象
 *
 * @author AI-Pick
 */
@Data
public class PartnerApplicantVO {

    /** 应征ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 应征消息 */
    private String message;

    /** 匹配度 */
    private Integer matchScore;

    /** 状态 0-待处理 1-已接受 2-已拒绝 */
    private Integer status;

    /** 应征时间 */
    private LocalDateTime createTime;
}