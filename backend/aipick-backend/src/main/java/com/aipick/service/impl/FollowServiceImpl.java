package com.aipick.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aipick.common.BusinessException;
import com.aipick.entity.Follow;
import com.aipick.entity.User;
import com.aipick.mapper.FollowMapper;
import com.aipick.mapper.UserMapper;
import com.aipick.service.FollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 关注服务实现
 *
 * @author AI-Pick
 */
@Service
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    public FollowServiceImpl(FollowMapper followMapper, UserMapper userMapper) {
        this.followMapper = followMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long userId, Long followUserId) {
        // 不能关注自己
        if (userId.equals(followUserId)) {
            throw new BusinessException("不能关注自己");
        }

        // 检查用户是否存在
        User user = userMapper.selectById(followUserId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查是否已关注
        if (isFollowing(userId, followUserId)) {
            throw new BusinessException("您已关注过此用户");
        }

        // 创建关注记录
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        followMapper.insert(follow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long userId, Long followUserId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId);
        followMapper.delete(wrapper);
    }

    @Override
    public IPage<User> getFollowingUsers(Long userId, int pageNum, int pageSize) {
        // 获取关注的用户ID列表
        List<Long> followingUserIds = followMapper.selectFollowingUserIds(userId);
        if (followingUserIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        // 查询用户详情
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getId, followingUserIds)
                .eq(User::getStatus, 0) // 只返回正常状态的用户
                .orderByDesc(User::getCreateTime);
        return userMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<User> getFollowers(Long userId, int pageNum, int pageSize) {
        // 获取粉丝用户ID列表
        List<Long> followerUserIds = followMapper.selectFollowerUserIds(userId);
        if (followerUserIds.isEmpty()) {
            return new Page<>(pageNum, pageSize);
        }

        // 查询用户详情
        Page<User> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(User::getId, followerUserIds)
                .eq(User::getStatus, 0) // 只返回正常状态的用户
                .orderByDesc(User::getCreateTime);
        return userMapper.selectPage(page, wrapper);
    }

    @Override
    public boolean isFollowing(Long userId, Long followUserId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                .eq(Follow::getFollowUserId, followUserId);
        return followMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<Long> batchCheckFollowing(Long userId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getUserId, userId)
                .in(Follow::getFollowUserId, userIds);
        List<Follow> follows = followMapper.selectList(wrapper);
        return follows.stream().map(Follow::getFollowUserId).collect(Collectors.toList());
    }

    @Override
    public int getFollowingCount(Long userId) {
        return followMapper.selectFollowingCount(userId);
    }

    @Override
    public int getFollowerCount(Long userId) {
        return followMapper.selectFollowerCount(userId);
    }
}
