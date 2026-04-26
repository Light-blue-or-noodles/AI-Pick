package com.sparklink.service.impl;

import com.sparklink.dto.AiRecommendRequest;
import com.sparklink.entity.Activity;
import com.sparklink.entity.Partner;
import com.sparklink.entity.User;
import com.sparklink.mapper.ActivityMapper;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.UserMapper;
import com.sparklink.common.PartnerScopeConstants;
import com.sparklink.common.PartnerTypeConstants;
import com.sparklink.service.AiService;
import com.sparklink.service.HomeService;
import com.sparklink.util.MediaPathUtil;
import com.sparklink.vo.ActivityVO;
import com.sparklink.vo.AiRecommendVO;
import com.sparklink.vo.HomeRecommendVO;
import com.sparklink.vo.PartnerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页推荐服务实现：优先走 AI 推荐（含反馈降权），失败时降级为按时间排序列表
 *
 * @author AI-Pick
 */
@Service
public class HomeServiceImpl implements HomeService {

    private static final Logger log = LoggerFactory.getLogger(HomeServiceImpl.class);

    private static final int HOME_PARTNER_LIMIT = 10;

    private static final int HOME_ACTIVITY_LIMIT = 10;

    private final AiService aiService;
    private final PartnerMapper partnerMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;

    public HomeServiceImpl(AiService aiService,
                           PartnerMapper partnerMapper,
                           ActivityMapper activityMapper,
                           UserMapper userMapper) {
        this.aiService = aiService;
        this.partnerMapper = partnerMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
    }

    @Override
    public HomeRecommendVO getRecommend(Long userId, Double latitude, Double longitude) {
        try {
            AiRecommendRequest req = new AiRecommendRequest();
            req.setUserId(userId);
            req.setLatitude(latitude);
            req.setLongitude(longitude);
            req.setPartnerLimit(HOME_PARTNER_LIMIT);
            req.setActivityLimit(HOME_ACTIVITY_LIMIT);
            AiRecommendVO ai = aiService.recommend(req);
            HomeRecommendVO vo = new HomeRecommendVO();
            vo.setPartners(ai.getPartners() != null ? ai.getPartners() : new ArrayList<>());
            vo.setActivities(ai.getActivities() != null ? ai.getActivities() : new ArrayList<>());
            return vo;
        } catch (Exception e) {
            log.warn("首页推荐走 AI 失败，降级为时间序列表", e);
            return fallbackByTimeOrder();
        }
    }

    /**
     * 无 AI 或异常时的兜底：与旧版一致，按创建时间取最新搭子/活动
     */
    private HomeRecommendVO fallbackByTimeOrder() {
        HomeRecommendVO result = new HomeRecommendVO();

        LambdaQueryWrapper<Partner> partnerWrapper = new LambdaQueryWrapper<>();
        partnerWrapper.eq(Partner::getStatus, 0)
                .orderByDesc(Partner::getCreateTime)
                .last("LIMIT 10");
        List<Partner> partners = partnerMapper.selectList(partnerWrapper);
        result.setPartners(convertPartners(partners));

        LambdaQueryWrapper<Activity> activityWrapper = new LambdaQueryWrapper<>();
        activityWrapper.in(Activity::getStatus, 0, 1, 2)
                .orderByDesc(Activity::getCreateTime)
                .last("LIMIT 10");
        List<Activity> activities = activityMapper.selectList(activityWrapper);
        result.setActivities(convertActivities(activities));

        return result;
    }

    private List<PartnerVO> convertPartners(List<Partner> partners) {
        if (partners == null || partners.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = partners.stream()
                .map(Partner::getUserId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(userIds);
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        List<PartnerVO> result = new ArrayList<>();
        for (Partner partner : partners) {
            PartnerVO vo = new PartnerVO();
            vo.setId(partner.getId());
            vo.setUserId(partner.getUserId());
            vo.setTitle(partner.getTitle());
            vo.setDescription(partner.getContent());
            vo.setPreference(partner.getPreference());
            vo.setTypeCode(partner.getType());
            vo.setTypeName(PartnerTypeConstants.labelOf(partner.getType()));
            if (partner.getType() != null) {
                vo.setType(String.valueOf(partner.getType()));
            }
            vo.setScope(partner.getScope());
            vo.setScopeName(PartnerScopeConstants.labelOf(partner.getScope()));
            vo.setCoverImage(MediaPathUtil.normalizeForResponse(partner.getCoverImage()));
            vo.setMaxParticipants(partner.getTargetCount());
            vo.setCurrentParticipants(partner.getCurrentCount());
            vo.setAddress(partner.getLocation());
            vo.setLatitude(partner.getLatitude());
            vo.setLongitude(partner.getLongitude());
            vo.setPlanTime(partner.getPlanTime());
            vo.setPlanEndTime(partner.getPlanEndTime());
            vo.setStatus(partner.getStatus());
            vo.setCreateTime(partner.getCreateTime());

            User user = userMap.get(partner.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(com.sparklink.util.AvatarUtil.sanitizeForResponse(user.getAvatar()));
            }

            result.add(vo);
        }
        return result;
    }

    private List<ActivityVO> convertActivities(List<Activity> activities) {
        List<ActivityVO> result = new ArrayList<>();
        for (Activity activity : activities) {
            ActivityVO vo = new ActivityVO();
            vo.setId(activity.getId());
            vo.setUserId(activity.getUserId());
            vo.setTitle(activity.getTitle());
            vo.setDescription(activity.getDescription());
            vo.setMaxParticipants(activity.getMaxParticipants());
            vo.setCurrentParticipants(activity.getCurrentParticipants());
            vo.setFee(activity.getFee() != null ? activity.getFee().doubleValue() : 0.0);
            vo.setAddress(activity.getLocation());
            vo.setStatus(activity.getStatus());
            vo.setCreateTime(activity.getCreateTime());

            result.add(vo);
        }
        return result;
    }
}
