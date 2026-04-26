package com.sparklink.controller;

import com.sparklink.common.Result;
import com.sparklink.service.IMService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

/**
 * 腾讯云 IM 控制器
 *
 * @author AI-Pick
 */
@RestController
@RequestMapping("/im")
public class IMController {

    private final IMService imService;

    @Value("${app.im-batch-import-secret:}")
    private String imBatchImportSecret;

    public IMController(IMService imService) {
        this.imService = imService;
    }

    /**
     * 获取当前用户的 IM UserSig
     * 实际路由：GET /api/im/usersig
     */
    @GetMapping("/usersig")
    public Result<Map<String, Object>> getUserSig(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(imService.generateUserSig(userId));
    }

    /**
     * 单聊前确保对方用户已在腾讯云 IM 开户（account_import），避免 SDK 报错 20003。
     * POST /api/im/prep-peer，body: {@code {"peerUserId": 123}}
     */
    @PostMapping("/prep-peer")
    public Result<Map<String, Object>> prepPeerForIm(
            @RequestHeader("X-User-Id") Long viewerId,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null || !body.containsKey("peerUserId")) {
            return Result.error(400, "peerUserId 必填");
        }
        Object raw = body.get("peerUserId");
        if (raw == null) {
            return Result.error(400, "peerUserId 无效");
        }
        long peerId;
        try {
            peerId = Long.parseLong(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return Result.error(400, "peerUserId 无效");
        }
        if (peerId <= 0) {
            return Result.error(400, "peerUserId 无效");
        }
        if (peerId == viewerId) {
            return Result.error(400, "不能与自身发起 IM 单聊");
        }
        imService.assertPeerImportedToIm(peerId);
        imService.ensureImAccountForUser(viewerId);
        return Result.success(Collections.singletonMap("ok", true));
    }

    /**
     * 将库中未删除用户批量导入腾讯云 IM（account_import）。
     * 需配置 {@code app.im-batch-import-secret}，请求头携带 {@code X-Im-Batch-Secret} 且一致；生产务必使用强随机密钥并勿泄露。
     */
    @PostMapping("/admin/batch-import-users")
    public Result<Map<String, Object>> batchImportUsers(
            @RequestHeader(value = "X-Im-Batch-Secret", required = false) String secret) {
        if (!StringUtils.hasText(imBatchImportSecret)) {
            return Result.error(403, "未配置 app.im-batch-import-secret，拒绝执行批量导入");
        }
        if (!StringUtils.hasText(secret) || !imBatchImportSecret.equals(secret.trim())) {
            return Result.error(403, "密钥无效");
        }
        return Result.success(imService.batchImportAllUsersToIm());
    }
}
