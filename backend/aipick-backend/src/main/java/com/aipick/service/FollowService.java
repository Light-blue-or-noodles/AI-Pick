package com.aipick.service;

import com.aipick.entity.User;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 关注服务接口
 *
 * @author AI-Pick
 */
public interface FollowService {

    /**
     * 关注用户
     *
     * @param userId       当前用户ID
     * @param followUserId 要关注的用户ID
     */
    void followUser(Long userId, Long followUserId);

    /**
     * 取消关注用户
     *
     * @param userId       当前用户ID
     * @param followUserId 要取消关注的用户ID
     */
    void unfollowUser(Long userId, Long followUserId);

    /**
     * 获取我关注的用户列表
     *
     * @param userId   当前用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 用户分页列表
     */
    IPage<User> getFollowingUsers(Long userId, int pageNum, int pageSize);

    /**
     * 获取我的粉丝列表
     *
     * @param userId   当前用户ID
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @return 用户分页列表
     */
    IPage<User> getFollowers(Long userId, int pageNum, int pageSize);

    /**
     * 检查是否已关注用户
     *
     * @param userId       当前用户ID
     * @param followUserId 目标用户ID
     * @return 是否已关注
     */
    boolean isFollowing(Long userId, Long followUserId);

    /**
     * 批量检查关注状态
     *
     * @param userId    当前用户ID
     * @param userIds   用户ID列表
     * @return 已关注的用户ID列表
     */
    List<Long> batchCheckFollowing(Long userId, List<Long> userIds);

    /**
     * 获取关注数
     *
     * @param userId 用户ID
     * @return 关注数
     */
    int getFollowingCount(Long userId);

    /**
     * 获取粉丝数
     *
     * @param userId 用户ID
     * @return 粉丝数
     */
    int getFollowerCount(Long userId);
}
