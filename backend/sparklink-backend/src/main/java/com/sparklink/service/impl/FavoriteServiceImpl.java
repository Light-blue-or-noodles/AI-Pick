package com.sparklink.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sparklink.common.BusinessException;
import com.sparklink.entity.Activity;
import com.sparklink.entity.Favorite;
import com.sparklink.entity.Partner;
import com.sparklink.mapper.ActivityMapper;
import com.sparklink.mapper.FavoriteMapper;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.service.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务实现
 *
 * @author AI-Pick
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ActivityMapper activityMapper;
    private final PartnerMapper partnerMapper;

    public FavoriteServiceImpl(FavoriteMapper favoriteMapper, ActivityMapper activityMapper, PartnerMapper partnerMapper) {
        this.favoriteMapper = favoriteMapper;
        this.activityMapper = activityMapper;
        this.partnerMapper = partnerMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteActivity(Long userId, Long activityId) {
        // 检查活动是否存在
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("活动不存在");
        }

        // 检查是否已收藏
        if (hasFavoritedActivity(userId, activityId)) {
            throw new BusinessException("您已收藏过此活动");
        }

        // 创建收藏记录
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(1); // 1-活动
        favorite.setTargetId(activityId);
        favoriteMapper.insert(favorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteActivity(Long userId, Long activityId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, 1)
                .eq(Favorite::getTargetId, activityId);
        favoriteMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoritePartner(Long userId, Long partnerId) {
        // 检查搭子是否存在
        Partner partner = partnerMapper.selectById(partnerId);
        if (partner == null) {
            throw new BusinessException("搭子不存在");
        }

        // 检查是否已收藏
        if (hasFavoritedPartner(userId, partnerId)) {
            throw new BusinessException("您已收藏过此搭子");
        }

        // 创建收藏记录
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setTargetType(2); // 2-搭子
        favorite.setTargetId(partnerId);
        favoriteMapper.insert(favorite);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoritePartner(Long userId, Long partnerId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, 2)
                .eq(Favorite::getTargetId, partnerId);
        favoriteMapper.delete(wrapper);
    }

    @Override
    public IPage<Activity> getFavoriteActivities(Long userId, int pageNum, int pageSize) {
        // 获取收藏的活动ID列表
        List<Long> activityIds = favoriteMapper.selectFavoriteActivityIds(userId);
        if (activityIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        // 查询活动详情
        Page<Activity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Activity::getId, activityIds)
                .ne(Activity::getStatus, 4) // 排除已取消的活动
                .orderByDesc(Activity::getCreateTime);
        return activityMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<Partner> getFavoritePartners(Long userId, int pageNum, int pageSize) {
        // 获取收藏的搭子ID列表
        List<Long> partnerIds = favoriteMapper.selectFavoritePartnerIds(userId);
        if (partnerIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        // 查询搭子详情
        Page<Partner> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Partner> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Partner::getId, partnerIds)
                .orderByDesc(Partner::getCreateTime);
        return partnerMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean hasFavoritedActivity(Long userId, Long activityId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, 1)
                .eq(Favorite::getTargetId, activityId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean hasFavoritedPartner(Long userId, Long partnerId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, 2)
                .eq(Favorite::getTargetId, partnerId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Long> batchCheckActivityFavorites(Long userId, List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, 1)
                .in(Favorite::getTargetId, activityIds);
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);
        return favorites.stream().map(Favorite::getTargetId).collect(Collectors.toList());
    }

    @Override
    public List<Long> batchCheckPartnerFavorites(Long userId, List<Long> partnerIds) {
        if (partnerIds == null || partnerIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId)
                .eq(Favorite::getTargetType, 2)
                .in(Favorite::getTargetId, partnerIds);
        List<Favorite> favorites = favoriteMapper.selectList(wrapper);
        return favorites.stream().map(Favorite::getTargetId).collect(Collectors.toList());
    }
}
