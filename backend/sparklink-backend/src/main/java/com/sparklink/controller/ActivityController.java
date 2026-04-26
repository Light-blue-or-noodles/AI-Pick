package com.sparklink.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.sparklink.common.Result;
import com.sparklink.dto.CreateActivityRequest;
import com.sparklink.dto.PageRequest;
import com.sparklink.dto.RegisterActivityRequest;
import com.sparklink.entity.Activity;
import com.sparklink.entity.ActivityRegistration;
import com.sparklink.entity.User;
import com.sparklink.service.ActivityService;
import com.sparklink.service.UserService;
import com.sparklink.vo.ActivityCalendarVO;
import com.sparklink.common.BusinessException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.sparklink.storage.ImageStorageService;
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

    private static final long IMAGE_MAX_SIZE = 5 * 1024 * 1024; // 5MB
    private static final String[] IMAGE_ALLOWED = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    private final ActivityService activityService;
    private final UserService userService;
    private final ImageStorageService imageStorageService;

    public ActivityController(ActivityService activityService, UserService userService,
                                ImageStorageService imageStorageService) {
        this.activityService = activityService;
        this.userService = userService;
        this.imageStorageService = imageStorageService;
    }

    /**
     * 上传活动图片（多图时多次调用，发布时传 imageUrls）
     */
    @PostMapping("/upload-image")
    public Result<Map<String, String>> uploadImage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "file", required = false) MultipartFile filePart,
            @RequestParam(value = "image", required = false) MultipartFile imagePart) {
        MultipartFile file = (filePart != null && !filePart.isEmpty()) ? filePart : (imagePart != null && !imagePart.isEmpty() ? imagePart : null);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !java.util.Arrays.asList(IMAGE_ALLOWED).contains(contentType)) {
            throw new BusinessException("仅支持 JPG/PNG/GIF/WEBP");
        }
        if (file.getSize() > IMAGE_MAX_SIZE) {
            throw new BusinessException("图片大小不能超过 5MB");
        }
        Map<String, String> stored = imageStorageService.storeActivityImage(file, userId);
        String urlPath = stored.get("url");
        return Result.success("上传成功", Map.of("url", urlPath));
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
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        IPage<Activity> list = activityService.getActivityList(pageRequest, type, category, keyword);
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

        // 主办方信息（发起人头像、昵称）
        if (activity.getUserId() != null) {
            try {
                User organizer = userService.getUserInfo(activity.getUserId());
                if (organizer != null) {
                    Map<String, Object> organizerMap = new HashMap<>();
                    organizerMap.put("id", organizer.getId());
                    organizerMap.put("nickname", organizer.getNickname() != null ? organizer.getNickname() : "发起人");
                    organizerMap.put("avatar", com.sparklink.util.AvatarUtil.sanitizeForResponse(organizer.getAvatar()));
                    result.put("organizer", organizerMap);
                }
            } catch (Exception ignored) {
                // 无主办方信息时前端使用占位
            }
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