package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.dto.PageRequest;
import com.aipick.dto.UserInfoDTO;
import com.aipick.dto.FollowActionRequest;
import com.aipick.entity.User;
import com.aipick.service.FollowService;
import com.aipick.service.UserService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 关注控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/user")
public class FollowController {

    private final FollowService followService;
    private final UserService userService;

    public FollowController(FollowService followService, UserService userService) {
        this.followService = followService;
        this.userService = userService;
    }

    /**
     * 关注用户
     */
    @PostMapping("/follow/{userId}")
    public Result<Void> followUser(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable("userId") Long followUserId) {
        followService.followUser(currentUserId, followUserId);
        return Result.success("关注成功", null);
    }

    /**
     * 取消关注用户
     */
    @DeleteMapping("/follow/{userId}")
    public Result<Void> unfollowUser(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable("userId") Long followUserId) {
        followService.unfollowUser(currentUserId, followUserId);
        return Result.success("取消关注成功", null);
    }

    /**
     * 获取我关注的用户列表
     */
    @GetMapping("/following")
    public Result<IPage<UserInfoDTO>> getFollowingUsers(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @ModelAttribute PageRequest pageRequest) {
        IPage<User> userPage = followService.getFollowingUsers(userId, pageRequest.getPageNum(), pageRequest.getPageSize());
        IPage<UserInfoDTO> dtoPage = userPage.convert(this::toUserInfoDTO);
        return Result.success(dtoPage);
    }

    /**
     * 获取我的粉丝列表
     */
    @GetMapping("/followers")
    public Result<IPage<UserInfoDTO>> getFollowers(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @ModelAttribute PageRequest pageRequest) {
        IPage<User> userPage = followService.getFollowers(userId, pageRequest.getPageNum(), pageRequest.getPageSize());
        IPage<UserInfoDTO> dtoPage = userPage.convert(this::toUserInfoDTO);
        return Result.success(dtoPage);
    }

    /**
     * 获取关注统计（关注数和粉丝数）
     */
    @GetMapping("/follow/stats")
    public Result<Map<String, Integer>> getFollowStats(
            @RequestHeader("X-User-Id") Long userId) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("followingCount", followService.getFollowingCount(userId));
        stats.put("followerCount", followService.getFollowerCount(userId));
        return Result.success(stats);
    }

    /**
     * 检查是否已关注某用户
     */
    @GetMapping("/follow/check/{userId}")
    public Result<Map<String, Boolean>> checkFollowing(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable("userId") Long targetUserId) {
        boolean isFollowing = followService.isFollowing(currentUserId, targetUserId);
        return Result.success(Map.of("isFollowing", isFollowing));
    }

    /**
     * 关注/取消关注用户（带 action 参数）
     * POST /api/user/{id}/follow
     * action: follow - 关注, cancel - 取消关注
     */
    @PostMapping("/{id}/follow")
    public Result<Map<String, Object>> followUserWithAction(
            @RequestHeader("X-User-Id") Long currentUserId,
            @PathVariable("id") Long targetUserId,
            @Valid @RequestBody FollowActionRequest request) {
        
        String action = request.getAction();
        boolean isFollowing = followService.isFollowing(currentUserId, targetUserId);
        
        if ("follow".equals(action)) {
            if (isFollowing) {
                // 幂等性：已关注则返回成功但不重复操作
                return Result.success("已关注", Map.of("isFollowing", true));
            }
            followService.followUser(currentUserId, targetUserId);
            return Result.success("关注成功", Map.of("isFollowing", true));
        } else if ("cancel".equals(action)) {
            if (!isFollowing) {
                // 幂等性：未关注则返回成功但不重复操作
                return Result.success("已取消关注", Map.of("isFollowing", false));
            }
            followService.unfollowUser(currentUserId, targetUserId);
            return Result.success("取消关注成功", Map.of("isFollowing", false));
        }
        
        return Result.error("无效的操作类型");
    }

    private UserInfoDTO toUserInfoDTO(User user) {
        if (user == null) {
            return null;
        }
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(com.aipick.util.AvatarUtil.sanitizeForResponse(user.getAvatar()));
        dto.setGender(user.getGender());
        dto.setBio(user.getBio());
        dto.setCompanyName(user.getCompanyName());
        dto.setCompanyVerified(user.getCompanyVerified());
        dto.setSchoolName(user.getSchoolName());
        dto.setSchoolVerified(user.getSchoolVerified());
        dto.setBirthday(user.getBirthday());
        dto.setTags(user.getTags() != null ? user.getTags() : "");
        return dto;
    }
}
