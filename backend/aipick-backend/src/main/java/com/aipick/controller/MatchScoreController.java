package com.aipick.controller;

import com.aipick.common.Result;
import com.aipick.dto.MatchScoreDTO;
import com.aipick.service.MatchScoreService;
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
     * 计算两个用户之间的匹配度
     *
     * 请求体示例：{"userId":1,"targetId":2}
     */
    @PostMapping("/match-score")
    public Result<MatchScoreDTO> calculate(@RequestBody MatchScoreDTO request) {
        MatchScoreDTO dto = matchScoreService.calculateMatchScore(request.getUserId(), request.getTargetId());
        return Result.success("计算成功", dto);
    }
}

