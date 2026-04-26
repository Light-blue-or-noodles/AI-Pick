package com.sparklink.vo;

import lombok.Data;

/**
 * 主办方视图对象
 *
 * @author AI-Pick
 */
@Data
public class OrganizerVO {

    /** 用户ID */
    private Long id;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 星级评分 */
    private Double starRating;
}