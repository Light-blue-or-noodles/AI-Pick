package com.aipick.service;

import com.aipick.dto.CreateActivityRequest;
import com.aipick.dto.PageRequest;
import com.aipick.dto.RegisterActivityRequest;
import com.aipick.entity.Activity;
import com.aipick.entity.ActivityRegistration;
import com.aipick.vo.ActivityCalendarVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 活动服务接口
 *
 * @author AI-Pick
 */
public interface ActivityService {

    /**
     * 发布活动
     *
     * @param userId  用户ID
     * @param request 发布请求
     * @return 活动信息
     */
    Activity createActivity(Long userId, CreateActivityRequest request);

    /**
     * 分页查询活动列表
     *
     * @param pageRequest 分页请求
     * @param type        活动类型
     * @param category    分类
     * @return 活动分页列表
     */
    IPage<Activity> getActivityList(PageRequest pageRequest, Integer type, String category);

    /**
     * 获取活动详情
     *
     * @param activityId 活动ID
     * @return 活动详情
     */
    Activity getActivityDetail(Long activityId);

    /**
     * 报名活动
     *
     * @param userId    用户ID
     * @param activityId 活动ID
     * @param request   报名请求
     * @return 报名记录
     */
    ActivityRegistration registerActivity(Long userId, Long activityId, RegisterActivityRequest request);

    /**
     * 取消报名
     *
     * @param userId    用户ID
     * @param activityId 活动ID
     */
    void cancelRegistration(Long userId, Long activityId);

    /**
     * 获取活动报名列表
     *
     * @param activityId 活动ID
     * @return 报名列表
     */
    java.util.List<ActivityRegistration> getRegistrationList(Long activityId);

    /**
     * 检查用户是否已报名
     *
     * @param userId    用户ID
     * @param activityId 活动ID
     * @return 是否已报名
     */
    boolean hasRegistered(Long userId, Long activityId);

    /**
     * 签到
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     */
    void checkIn(Long userId, Long activityId);

    /**
     * 获取我的活动
     *
     * @param userId 用户ID
     * @param type   类型：joined-我参加的，created-我发布的
     * @return 活动列表
     */
    List<Activity> getMyActivities(Long userId, String type);

    /**
     * 活动日历视图：按月份返回每日活动列表
     *
     * @param year  年
     * @param month 月（1-12）
     * @return 日历数据，按日期分组的活动
     */
    ActivityCalendarVO getCalendarView(int year, int month);
}