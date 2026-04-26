package com.sparklink.vo;

import lombok.Data;

import java.util.List;

/**
 * 某日的活动列表（日历视图单日）
 *
 * @author AI-Pick
 */
@Data
public class DayActivitiesVO {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 该日的活动列表（简要信息） */
    private List<ActivityVO> activities;
}
