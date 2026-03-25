package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.service.HomeService;
import com.aipick.vo.HomeRecommendVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    /**
     * 首页推荐
     */
    @GetMapping("/recommend")
    public Result<HomeRecommendVO> getRecommend(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) {
        HomeRecommendVO result = homeService.getRecommend(userId, latitude, longitude);
        return Result.success(result);
    }
}