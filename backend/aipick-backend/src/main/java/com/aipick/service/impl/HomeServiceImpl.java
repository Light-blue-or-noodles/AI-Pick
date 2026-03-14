package com.aipick.service.impl;

import com.aipick.entity.Activity;
import com.aipick.entity.Partner;
import com.aipick.entity.User;
import com.aipick.mapper.ActivityMapper;
import com.aipick.mapper.PartnerMapper;
import com.aipick.mapper.UserMapper;
import com.aipick.service.HomeService;
import com.aipick.vo.ActivityVO;
import com.aipick.vo.HomeRecommendVO;
import com.aipick.vo.PartnerVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页推荐服务实现
 *
 * @author AI-Pick
 */
@Service
public class HomeServiceImpl implements HomeService {

    private final PartnerMapper partnerMapper;
    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;

    public HomeServiceImpl(PartnerMapper partnerMapper, ActivityMapper activityMapper, UserMapper userMapper) {
        this.partnerMapper = partnerMapper;
        this.activityMapper = activityMapper;
        this.userMapper = userMapper;
    }

    @Override
    public HomeRecommendVO getRecommend(Long userId, Double latitude, Double longitude) {
        HomeRecommendVO result = new HomeRecommendVO();

        // 获取推荐的搭子（状态为待应征的）
        LambdaQueryWrapper<Partner> partnerWrapper = new LambdaQueryWrapper<>();
        partnerWrapper.eq(Partner::getStatus, 0)
                .orderByDesc(Partner::getCreateTime)
                .last("LIMIT 10");
        List<Partner> partners = partnerMapper.selectList(partnerWrapper);
        result.setPartners(convertPartners(partners));

        // 获取推荐的活动（状态为报名中或进行中的）
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
        
        // 批量查询所有用户信息，避免 N+1 问题
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
        
        // 转换为 VO
        List<PartnerVO> result = new ArrayList<>();
        for (Partner partner : partners) {
            PartnerVO vo = new PartnerVO();
            vo.setId(partner.getId());
            vo.setUserId(partner.getUserId());
            vo.setTitle(partner.getTitle());
            vo.setDescription(partner.getContent());
            vo.setMaxParticipants(partner.getTargetCount());
            vo.setCurrentParticipants(partner.getCurrentCount());
            vo.setCreateTime(partner.getCreateTime());
            
            // 从 Map 中获取用户信息
            User user = userMap.get(partner.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
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