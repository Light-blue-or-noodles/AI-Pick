package com.sparklink.service;

import com.sparklink.entity.Activity;
import com.sparklink.entity.Partner;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 收藏服务接口
 *
 * @author AI-Pick
 */
public interface FavoriteService {

    /**
     * 收藏活动
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     */
    void favoriteActivity(Long userId, Long activityId);

    /**
     * 取消收藏活动
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     */
    void unfavoriteActivity(Long userId, Long activityId);

    /**
     * 收藏搭子
     *
     * @param userId    用户ID
     * @param partnerId 搭子ID
     */
    void favoritePartner(Long userId, Long partnerId);

    /**
     * 取消收藏搭子
     *
     * @param userId    用户ID
     * @param partnerId 搭子ID
     */
    void unfavoritePartner(Long userId, Long partnerId);

    /**
     * 获取用户收藏的活动列表
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 活动分页列表
     */
    IPage<Activity> getFavoriteActivities(Long userId, int pageNum, int pageSize);

    /**
     * 获取用户收藏的搭子列表
     *
     * @param userId   用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 搭子分页列表
     */
    IPage<Partner> getFavoritePartners(Long userId, int pageNum, int pageSize);

    /**
     * 检查用户是否已收藏活动
     *
     * @param userId     用户ID
     * @param activityId 活动ID
     * @return 是否已收藏
     */
    boolean hasFavoritedActivity(Long userId, Long activityId);

    /**
     * 检查用户是否已收藏搭子
     *
     * @param userId    用户ID
     * @param partnerId 搭子ID
     * @return 是否已收藏
     */
    boolean hasFavoritedPartner(Long userId, Long partnerId);

    /**
     * 批量检查活动收藏状态
     *
     * @param userId      用户ID
     * @param activityIds 活动ID列表
     * @return 已收藏的活动ID列表
     */
    List<Long> batchCheckActivityFavorites(Long userId, List<Long> activityIds);

    /**
     * 批量检查搭子收藏状态
     *
     * @param userId     用户ID
     * @param partnerIds 搭子ID列表
     * @return 已收藏的搭子ID列表
     */
    List<Long> batchCheckPartnerFavorites(Long userId, List<Long> partnerIds);
}
