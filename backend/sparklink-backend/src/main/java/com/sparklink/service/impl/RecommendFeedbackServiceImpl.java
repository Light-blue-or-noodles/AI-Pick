package com.sparklink.service.impl;

import com.sparklink.common.RecommendFeedbackConstants;
import com.sparklink.entity.Activity;
import com.sparklink.entity.Partner;
import com.sparklink.entity.RecommendFeedback;
import com.sparklink.mapper.ActivityMapper;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.RecommendFeedbackMapper;
import com.sparklink.service.RecommendFeedbackService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐反馈服务实现
 * 
 * @author AI-Pick
 */
@Service
public class RecommendFeedbackServiceImpl implements RecommendFeedbackService {

    private static final List<Integer> NEGATIVE_FEEDBACK_TYPES = Arrays.asList(
            RecommendFeedbackConstants.FEEDBACK_SKIP,
            RecommendFeedbackConstants.FEEDBACK_NOT_COMPATIBLE
    );

    private final RecommendFeedbackMapper feedbackMapper;
    private final PartnerMapper partnerMapper;
    private final ActivityMapper activityMapper;

    public RecommendFeedbackServiceImpl(RecommendFeedbackMapper feedbackMapper,
                                        PartnerMapper partnerMapper,
                                        ActivityMapper activityMapper) {
        this.feedbackMapper = feedbackMapper;
        this.partnerMapper = partnerMapper;
        this.activityMapper = activityMapper;
    }

    @Override
    public boolean recordFeedback(Long userId, Integer targetType, Long targetId, 
                                  Integer feedbackType, Integer matchScore) {
        if (userId == null || targetType == null || targetId == null || feedbackType == null) {
            return false;
        }

        RecommendFeedback feedback = new RecommendFeedback();
        feedback.setUserId(userId);
        feedback.setTargetType(targetType);
        feedback.setTargetId(targetId);
        feedback.setFeedbackType(feedbackType);
        feedback.setMatchScore(matchScore);
        feedback.setFeedbackTime(LocalDateTime.now());

        return feedbackMapper.insert(feedback) > 0;
    }

    @Override
    public int getFeedbackCount(Long userId, Integer targetType, Long targetId) {
        if (userId == null || targetType == null || targetId == null) {
            return 0;
        }

        LambdaQueryWrapper<RecommendFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendFeedback::getUserId, userId)
               .eq(RecommendFeedback::getTargetType, targetType)
               .eq(RecommendFeedback::getTargetId, targetId);

        return Math.toIntExact(feedbackMapper.selectCount(wrapper));
    }

    @Override
    public int getSkipCount(Long userId, Integer targetType) {
        if (userId == null || targetType == null) {
            return 0;
        }

        LambdaQueryWrapper<RecommendFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendFeedback::getUserId, userId)
               .eq(RecommendFeedback::getTargetType, targetType)
               .eq(RecommendFeedback::getFeedbackType, RecommendFeedbackConstants.FEEDBACK_SKIP);

        return Math.toIntExact(feedbackMapper.selectCount(wrapper));
    }

    @Override
    public int getNegativeFeedbackCount(Long userId, Integer targetType) {
        if (userId == null || targetType == null) {
            return 0;
        }
        LambdaQueryWrapper<RecommendFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getTargetType, targetType)
                .in(RecommendFeedback::getFeedbackType, NEGATIVE_FEEDBACK_TYPES);
        return Math.toIntExact(feedbackMapper.selectCount(wrapper));
    }

    @Override
    public Set<Long> findNegativeTargetIds(Long userId, Integer targetType) {
        if (userId == null || targetType == null) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<RecommendFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getTargetType, targetType)
                .in(RecommendFeedback::getFeedbackType, NEGATIVE_FEEDBACK_TYPES);
        List<RecommendFeedback> list = feedbackMapper.selectList(wrapper);
        return list.stream()
                .map(RecommendFeedback::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public Map<Integer, Integer> countNegativeByPartnerType(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<RecommendFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getTargetType, RecommendFeedbackConstants.TARGET_PARTNER)
                .in(RecommendFeedback::getFeedbackType, NEGATIVE_FEEDBACK_TYPES);
        List<RecommendFeedback> list = feedbackMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = list.stream()
                .map(RecommendFeedback::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Partner> partners = partnerMapper.selectBatchIds(ids);
        Map<Long, Partner> byId = partners.stream()
                .collect(Collectors.toMap(Partner::getId, p -> p, (a, b) -> a));
        Map<Integer, Integer> out = new HashMap<>();
        for (RecommendFeedback fb : list) {
            Partner p = byId.get(fb.getTargetId());
            if (p != null && p.getType() != null) {
                out.merge(p.getType(), 1, Integer::sum);
            }
        }
        return out;
    }

    @Override
    public Map<String, Integer> countNegativeByActivityCategory(Long userId) {
        if (userId == null) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<RecommendFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendFeedback::getUserId, userId)
                .eq(RecommendFeedback::getTargetType, RecommendFeedbackConstants.TARGET_ACTIVITY)
                .in(RecommendFeedback::getFeedbackType, NEGATIVE_FEEDBACK_TYPES);
        List<RecommendFeedback> list = feedbackMapper.selectList(wrapper);
        if (list.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = list.stream()
                .map(RecommendFeedback::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Activity> activities = activityMapper.selectBatchIds(ids);
        Map<Long, Activity> byId = activities.stream()
                .collect(Collectors.toMap(Activity::getId, a -> a, (x, y) -> x));
        Map<String, Integer> out = new HashMap<>();
        for (RecommendFeedback fb : list) {
            Activity a = byId.get(fb.getTargetId());
            if (a == null) {
                continue;
            }
            String cat = StringUtils.hasText(a.getCategory()) ? a.getCategory().trim() : "__uncategorized__";
            out.merge(cat, 1, Integer::sum);
        }
        return out;
    }
}