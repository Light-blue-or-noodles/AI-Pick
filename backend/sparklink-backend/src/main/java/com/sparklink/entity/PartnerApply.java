package com.sparklink.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.sparklink.common.BaseEntity;

import java.io.Serializable;

/**
 * 搭子应征记录实体
 *
 * @author AI-Pick
 */
@TableName("t_partner_apply")
public class PartnerApply extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 搭子ID */
    private Long partnerId;

    /** 申请人ID */
    private Long userId;

    /** 申请留言 */
    private String message;

    /** 状态 0-待审核 1-已通过 2-已拒绝 */
    private Integer status;

    public Long getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(Long partnerId) {
        this.partnerId = partnerId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}