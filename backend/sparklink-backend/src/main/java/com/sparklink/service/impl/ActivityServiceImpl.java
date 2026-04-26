package com.sparklink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparklink.common.BusinessException;
import com.sparklink.dto.CreateActivityRequest;
import com.sparklink.dto.PageRequest;
import com.sparklink.dto.RegisterActivityRequest;
import com.sparklink.entity.Activity;
import com.sparklink.entity.ActivityRegistration;
import com.sparklink.mapper.ActivityMapper;
import com.sparklink.mapper.ActivityRegistrationMapper;
import com.sparklink.service.ActivityService;
import com.sparklink.util.MediaPathUtil;
import com.sparklink.vo.ActivityCalendarVO;
import com.sparklink.vo.ActivityVO;
import com.sparklink.vo.DayActivitiesVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 活动服务实现
 *
 * @author AI-Pick
 */
@Service
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ActivityServiceImpl(ActivityMapper activityMapper, ActivityRegistrationMapper registrationMapper) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
    }

    private String buildImagesJson(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(imageUrls);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * 列表/日历展示用封面：cover_image 为空时取 images JSON 首张
     */
    private String effectiveCoverImage(Activity a) {
        String c = a.getCoverImage();
        if (c != null && !c.isBlank()) {
            String n = MediaPathUtil.normalizeForResponse(c);
            return n != null ? n : c;
        }
        String raw = a.getImages();
        if (raw == null || raw.isBlank()) {
            return c;
        }
        try {
            List<String> list = objectMapper.readValue(raw, new TypeReference<List<String>>() {
            });
            if (list != null && !list.isEmpty()) {
                String first = list.get(0);
                if (first != null && !first.isBlank()) {
                    String n = MediaPathUtil.normalizeForResponse(first);
                    return n != null ? n : first;
                }
            }
        } catch (JsonProcessingException ignored) {
            // ignore
        }
        return c;
    }

    /**
     * 出参与落库修正：将历史写入的 http://IP:8080/api/static/... 统一为 /static/...，便于小程序用当前 baseUrl 访问。
     */
    private void normalizeActivityMediaFields(Activity a) {
        if (a == null) {
            return;
        }
        String cover = a.getCoverImage();
        if (cover != null && !cover.isBlank()) {
            String n = MediaPathUtil.normalizeForResponse(cover);
            if (n != null) {
                a.setCoverImage(n);
            }
        }
        String raw = a.getImages();
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            List<String> list = objectMapper.readValue(raw, new TypeReference<List<String>>() {
            });
            if (list == null || list.isEmpty()) {
                return;
            }
            List<String> out = new ArrayList<>();
            for (String u : list) {
                if (u == null || u.isBlank()) {
                    continue;
                }
                String n = MediaPathUtil.normalizeForResponse(u);
                out.add(n != null ? n : u);
            }
            a.setImages(objectMapper.writeValueAsString(out));
        } catch (JsonProcessingException ignored) {
            // keep original images json
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Activity createActivity(Long userId, CreateActivityRequest request) {
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setTitle(request.getTitle());
        activity.setDescription(request.getDescription());
        activity.setType(request.getType());
        activity.setCategory(request.getCategory());
        activity.setRegisterEndTime(request.getRegisterEndTime());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setLocation(request.getLocation());
        activity.setLatitude(request.getLatitude());
        activity.setLongitude(request.getLongitude());
        activity.setFee(request.getFee() != null ? request.getFee() : BigDecimal.ZERO);
        activity.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 0);
        activity.setCurrentParticipants(0);
        List<String> imageUrls = request.getImageUrls();
        String coverImage = request.getCoverImage();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            coverImage = imageUrls.get(0);
            activity.setCoverImage(coverImage);
            activity.setImages(buildImagesJson(imageUrls));
        } else {
            if (coverImage == null || coverImage.isBlank()) {
                coverImage = defaultCoverByCategory(request.getCategory());
            }
            activity.setCoverImage(coverImage);
        }
        activity.setViewCount(0);

        // 设置状态
        LocalDateTime now = LocalDateTime.now();
        if (request.getStartTime() != null && request.getStartTime().isAfter(now)) {
            if (request.getRegisterEndTime() != null && request.getRegisterEndTime().isAfter(now)) {
                activity.setStatus(1); // 报名中
            } else {
                activity.setStatus(0); // 待开始
            }
        } else {
            activity.setStatus(2); // 进行中
        }

        activityMapper.insert(activity);

        // 新建活动不自动给创建人报名，报名人数保持为 0，创建人需自行点击报名

        return activity;
    }

    /** 按活动分类返回默认封面 */
    private static String defaultCoverByCategory(String category) {
        if (category == null || category.isBlank()) {
            return "/static/covers/activity-default.png";
        }
        switch (category) {
            case "运动": return "/static/covers/activity-sport.png";
            case "美食": return "/static/covers/activity-food.png";
            case "学习": return "/static/covers/activity-study.png";
            case "娱乐": return "/static/covers/activity-party.png";
            case "社交": return "/static/covers/activity-default.png";
            default: return "/static/covers/activity-default.png";
        }
    }

    @Override
    public IPage<Activity> getActivityList(PageRequest pageRequest, Integer type, String category, String keyword) {
        Page<Activity> page = new Page<>(pageRequest.getPageNum(), pageRequest.getPageSize());
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        // 只查询非取消的活动
        wrapper.ne(Activity::getStatus, 4);
        if (type != null) {
            wrapper.eq(Activity::getType, type);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(Activity::getCategory, category);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Activity::getTitle, keyword.trim());
        }
        wrapper.orderByDesc(Activity::getCreateTime);

        IPage<Activity> result = activityMapper.selectPage(page, wrapper);
        if (result.getRecords() != null) {
            result.getRecords().forEach(this::normalizeActivityMediaFields);
        }
        return result;
    }

    @Override
    public Activity getActivityDetail(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        normalizeActivityMediaFields(activity);
        // 增加浏览量（可能顺带把封面规范路径写回库）
        activity.setViewCount(activity.getViewCount() + 1);
        activityMapper.updateById(activity);

        return activity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityRegistration registerActivity(Long userId, Long activityId, RegisterActivityRequest request) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 检查是否已报名
        if (hasRegistered(userId, activityId)) {
            throw new BusinessException("您已报名过此活动");
        }

        // 检查人数限制
        if (activity.getMaxParticipants() > 0 && activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            throw new BusinessException("活动报名人数已满");
        }

        // 检查报名截止时间
        if (activity.getRegisterEndTime() != null && LocalDateTime.now().isAfter(activity.getRegisterEndTime())) {
            throw new BusinessException("活动报名已截止");
        }

        // 创建报名记录
        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activityId);
        registration.setUserId(userId);
        if (request != null) {
            registration.setMessage(request.getMessage());
        }
        registration.setStatus(0);
        registrationMapper.insert(registration);

        // 更新报名人数
        activity.setCurrentParticipants(activity.getCurrentParticipants() + 1);
        if (activity.getMaxParticipants() > 0 && activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            activity.setStatus(1); // 报名已满，但仍可查看
        }
        activityMapper.updateById(activity);

        return registration;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelRegistration(Long userId, Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId)
                .eq(ActivityRegistration::getStatus, 0);
        ActivityRegistration registration = registrationMapper.selectOne(wrapper);

        if (registration == null) {
            throw new BusinessException("您未报名此活动");
        }

        // 更新状态
        registration.setStatus(1);
        registrationMapper.updateById(registration);

        // 减少报名人数
        activity.setCurrentParticipants(Math.max(0, activity.getCurrentParticipants() - 1));
        if (activity.getCurrentParticipants() < activity.getMaxParticipants()) {
            activity.setStatus(1); // 恢复报名中
        }
        activityMapper.updateById(activity);
    }

    @Override
    public List<ActivityRegistration> getRegistrationList(Long activityId) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getStatus, 0)
                .orderByDesc(ActivityRegistration::getCreateTime);
        return registrationMapper.selectList(wrapper);
    }

    @Override
    public boolean hasRegistered(Long userId, Long activityId) {
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId)
                .eq(ActivityRegistration::getStatus, 0);
        return registrationMapper.selectCount(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkIn(Long userId, Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 查找报名记录
        LambdaQueryWrapper<ActivityRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivityRegistration::getActivityId, activityId)
                .eq(ActivityRegistration::getUserId, userId)
                .eq(ActivityRegistration::getStatus, 0);
        ActivityRegistration registration = registrationMapper.selectOne(wrapper);

        if (registration == null) {
            throw new BusinessException("您未报名此活动，无法签到");
        }

        if (registration.getCheckInTime() != null) {
            throw new BusinessException("您已签到");
        }

        // 签到
        registration.setCheckInTime(LocalDateTime.now());
        registrationMapper.updateById(registration);
    }

    @Override
    public List<Activity> getMyActivities(Long userId, String type) {
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        
        if ("created".equals(type)) {
            // 我发布的
            wrapper.eq(Activity::getUserId, userId);
        } else if ("joined".equals(type)) {
            // 我参加的
            LambdaQueryWrapper<ActivityRegistration> regWrapper = new LambdaQueryWrapper<>();
            regWrapper.eq(ActivityRegistration::getUserId, userId)
                    .eq(ActivityRegistration::getStatus, 0);
            List<ActivityRegistration> registrations = registrationMapper.selectList(regWrapper);
            if (registrations.isEmpty()) {
                return List.of();
            }
            List<Long> activityIds = registrations.stream().map(ActivityRegistration::getActivityId).toList();
            wrapper.in(Activity::getId, activityIds);
        } else {
            // 默认返回所有
            wrapper.eq(Activity::getUserId, userId);
        }
        
        wrapper.ne(Activity::getStatus, 4);
        wrapper.orderByDesc(Activity::getCreateTime);
        return activityMapper.selectList(wrapper);
    }

    @Override
    public ActivityCalendarVO getCalendarView(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(Activity::getStartTime, start)
                .lt(Activity::getStartTime, end)
                .ne(Activity::getStatus, 4)
                .orderByAsc(Activity::getStartTime);
        List<Activity> activities = activityMapper.selectList(wrapper);

        Map<LocalDate, List<ActivityVO>> map = new LinkedHashMap<>();
        for (Activity a : activities) {
            if (a.getStartTime() == null) {
                continue;
            }
            LocalDate d = a.getStartTime().toLocalDate();
            ActivityVO vo = new ActivityVO();
            vo.setId(a.getId());
            vo.setUserId(a.getUserId());
            vo.setTitle(a.getTitle());
            vo.setDescription(a.getDescription());
            vo.setAddress(a.getLocation());
            vo.setEventTime(a.getStartTime());
            vo.setMaxParticipants(a.getMaxParticipants());
            vo.setCurrentParticipants(a.getCurrentParticipants());
            vo.setFee(a.getFee() != null ? a.getFee().doubleValue() : 0.0);
            vo.setStatus(a.getStatus());
            vo.setCreateTime(a.getCreateTime());
            vo.setCoverImage(effectiveCoverImage(a));
            map.computeIfAbsent(d, k -> new ArrayList<>()).add(vo);
        }

        List<DayActivitiesVO> days = map.entrySet().stream()
                .map(e -> {
                    DayActivitiesVO day = new DayActivitiesVO();
                    day.setDate(e.getKey().toString());
                    day.setActivities(e.getValue());
                    return day;
                })
                .collect(Collectors.toList());

        ActivityCalendarVO result = new ActivityCalendarVO();
        result.setYear(year);
        result.setMonth(month);
        result.setDays(days);
        return result;
    }
}