package com.sparklink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sparklink.common.BaseEntity;

import java.io.Serializable;

/**
 * 收藏实体
 *
 * @author AI-Pick
 */
@TableName("t_favorite")
public class Favorite extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 目标类型 1-活动 2-搭子 */
    private Integer targetType;

    /** 目标ID（活动ID或搭子ID） */
    private Long targetId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }
}
