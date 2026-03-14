package com.aipick.vo;

import lombok.Data;

import java.util.List;

/**
 * 活动日历视图
 *
 * @author AI-Pick
 */
@Data
public class ActivityCalendarVO {

    /** 年 */
    private Integer year;

    /** 月 */
    private Integer month;

    /** 按日期分组的列表：某日有哪些活动 */
    private List<DayActivitiesVO> days;
}
