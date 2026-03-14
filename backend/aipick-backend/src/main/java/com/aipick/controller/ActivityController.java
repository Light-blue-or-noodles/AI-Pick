package com.aipick.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.aipick.common.Result;
import com.aipick.dto.CreateActivityRequest;
import com.aipick.dto.PageRequest;
import com.aipick.dto.RegisterActivityRequest;
import com.aipick.entity.Activity;
import com.aipick.entity.ActivityRegistration;
import com.aipick.service.ActivityService;
import com.aipick.vo.ActivityCalendarVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 活动控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/activity")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /**
     * 发布活动
     */
    @PostMapping
    public Result<Activity> createActivity(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateActivityRequest request) {
        Activity activity = activityService.createActivity(userId, request);
        return Result.success("发布成功", activity);
    }

    /**
     * 活动列表
     */
    @GetMapping
    public Result<IPage<Activity>> getActivityList(
            @ModelAttribute PageRequest pageRequest,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String category) {
        IPage<Activity> list = activityService.getActivityList(pageRequest, type, category);
        return Result.success(list);
    }

    /**
     * 活动详情
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getActivityDetail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        Activity activity = activityService.getActivityDetail(id);

        Map<String, Object> result = new HashMap<>();
        result.put("activity", activity);

        if (userId != null) {
            boolean hasRegistered = activityService.hasRegistered(userId, id);
            result.put("hasRegistered", hasRegistered);
        }

        return Result.success(result);
    }

    /**
     * 报名活动
     */
    @PostMapping("/{id}/join")
    public Result<ActivityRegistration> registerActivity(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestBody(required = false) RegisterActivityRequest request) {
        ActivityRegistration registration = activityService.registerActivity(userId, id, request);
        return Result.success("报名成功", registration);
    }

    /**
     * 取消报名
     */
    @PostMapping("/{id}/cancel")
    public Result<String> cancelRegistration(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        activityService.cancelRegistration(userId, id);
        return Result.success("取消报名成功", "取消报名成功");
    }

    /**
     * 签到
     */
    @PostMapping("/{id}/checkin")
    public Result<Void> checkIn(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        activityService.checkIn(userId, id);
        return Result.success("签到成功", null);
    }

    /**
     * 报名列表
     */
    @GetMapping("/{id}/registrations")
    public Result<List<ActivityRegistration>> getRegistrationList(@PathVariable Long id) {
        List<ActivityRegistration> list = activityService.getRegistrationList(id);
        return Result.success(list);
    }

    /**
     * 我的活动
     */
    @GetMapping("/my")
    public Result<List<Activity>> getMyActivities(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String type) {
        List<Activity> list = activityService.getMyActivities(userId, type);
        return Result.success(list);
    }

    /**
     * 活动日历视图：按年月返回每日活动列表，用于月视图展示
     */
    @GetMapping("/calendar")
    public Result<ActivityCalendarVO> getCalendarView(
            @RequestParam int year,
            @RequestParam int month) {
        if (month < 1 || month > 12) {
            return Result.<ActivityCalendarVO>error(400, "month 需在 1-12 之间");
        }
        return Result.success(activityService.getCalendarView(year, month));
    }
}