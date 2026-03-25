package com.aipick.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.aipick.common.BaseEntity;

import java.io.Serializable;

/**
 * 关注实体
 *
 * @author AI-Pick
 */
@TableName("t_follow")
public class Follow extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关注者ID */
    private Long userId;

    /** 被关注者ID */
    private Long followUserId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getFollowUserId() {
        return followUserId;
    }

    public void setFollowUserId(Long followUserId) {
        this.followUserId = followUserId;
    }
}
