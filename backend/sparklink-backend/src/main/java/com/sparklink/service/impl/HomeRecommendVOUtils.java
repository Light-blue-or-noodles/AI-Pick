package com.sparklink.service.impl;

import com.sparklink.entity.Activity;
import com.sparklink.entity.Partner;
import com.sparklink.entity.User;
import com.sparklink.mapper.UserMapper;
import com.sparklink.vo.ActivityVO;
import com.sparklink.vo.PartnerVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页推荐VO转换工具类
 *
 * @author AI-Pick
 */
public class HomeRecommendVOUtils {

    private static UserMapper userMapper;

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        HomeRecommendVOUtils.userMapper = userMapper;
    }

    public static List<PartnerVO> convertPartners(List<Partner> partners) {
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
        if (userMapper != null && !userIds.isEmpty()) {
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
                vo.setAvatar(com.sparklink.util.AvatarUtil.sanitizeForResponse(user.getAvatar()));
            }
            
            result.add(vo);
        }
        return result;
    }

    public static List<ActivityVO> convertActivities(List<Activity> activities) {
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