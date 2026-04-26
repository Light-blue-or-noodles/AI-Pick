package com.sparklink.controller;

import com.sparklink.common.Result;
import com.sparklink.dto.MatchScoreDTO;
import com.sparklink.service.MatchScoreService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 匹配度计算接口
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/ai")
public class MatchScoreController {

    private final MatchScoreService matchScoreService;

    public MatchScoreController(MatchScoreService matchScoreService) {
        this.matchScoreService = matchScoreService;
    }

    /**
     * 计算两个用户之间的匹配度（可带搭子活动以活动标签+活动位置参与）
     *
     * 请求体示例：{"userId":1,"targetId":2,"partnerId":100}
     */
    @PostMapping("/match-score")
    public Result<MatchScoreDTO> calculate(@RequestBody MatchScoreDTO request) {
        MatchScoreDTO dto = matchScoreService.calculateMatchScore(
                request.getUserId(), request.getTargetId(), request.getPartnerId());
        return Result.success("计算成功", dto);
    }
}

