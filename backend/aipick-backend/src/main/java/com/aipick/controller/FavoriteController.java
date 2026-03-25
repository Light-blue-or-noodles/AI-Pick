package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.dto.PageRequest;
import com.aipick.entity.Activity;
import com.aipick.entity.Partner;
import com.aipick.service.FavoriteService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 收藏控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    // ==================== 活动收藏 ====================

    /**
     * 收藏活动
     */
    @PostMapping("/activity/{id}")
    public Result<Void> favoriteActivity(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long activityId) {
        favoriteService.favoriteActivity(userId, activityId);
        return Result.success("收藏成功", null);
    }

    /**
     * 取消收藏活动
     */
    @DeleteMapping("/activity/{id}")
    public Result<Void> unfavoriteActivity(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long activityId) {
        favoriteService.unfavoriteActivity(userId, activityId);
        return Result.success("取消收藏成功", null);
    }

    /**
     * 获取我收藏的活动列表
     */
    @GetMapping("/activities")
    public Result<IPage<Activity>> getFavoriteActivities(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @ModelAttribute PageRequest pageRequest) {
        IPage<Activity> list = favoriteService.getFavoriteActivities(userId, pageRequest.getPageNum(), pageRequest.getPageSize());
        return Result.success(list);
    }

    // ==================== 搭子收藏 ====================

    /**
     * 收藏搭子
     */
    @PostMapping("/partner/{id}")
    public Result<Void> favoritePartner(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long partnerId) {
        favoriteService.favoritePartner(userId, partnerId);
        return Result.success("收藏成功", null);
    }

    /**
     * 取消收藏搭子
     */
    @DeleteMapping("/partner/{id}")
    public Result<Void> unfavoritePartner(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable("id") Long partnerId) {
        favoriteService.unfavoritePartner(userId, partnerId);
        return Result.success("取消收藏成功", null);
    }

    /**
     * 获取我收藏的搭子列表
     */
    @GetMapping("/partners")
    public Result<IPage<Partner>> getFavoritePartners(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @ModelAttribute PageRequest pageRequest) {
        IPage<Partner> list = favoriteService.getFavoritePartners(userId, pageRequest.getPageNum(), pageRequest.getPageSize());
        return Result.success(list);
    }
}
