package com.aipick.service;

import java.util.Map;

/**
 * 腾讯云 IM 服务
 *
 * @author AI-Pick
 */
public interface IMService {

    /**
     * 生成用户登录 IM 所需的 UserSig
     *
     * @param userId 用户 ID
     * @return 包含 userId、sdkAppId、userSig 的结果
     */
    Map<String, Object> generateUserSig(Long userId);

    /**
     * 将业务用户导入腾讯云 IM（REST account_import，重复导入安全）。
     * 需在配置中设置 {@code tencent.im.identifier} 为控制台 App 管理员 UserID；未配置时跳过。
     * 同步请求腾讯云，失败仅记日志，不向调用方抛业务异常。
     *
     * @param userId   业务用户主键，将转为 IM 的 UserID 字符串
     * @param nickname 昵称，可为空
     * @param faceUrl  头像绝对地址（http/https），非绝对 URL 时忽略该字段
     */
    void importUserToIm(Long userId, String nickname, String faceUrl);

    /**
     * 将数据库中未逻辑删除的用户批量导入腾讯云 IM（account_import，可重复执行，幂等）。
     * 依赖 tencent.im 下 sdk-app-id、key、identifier 已配置。
     *
     * @return total、successCount、failedCount、failedUserIds（最多 100 条）等
     */
    Map<String, Object> batchImportAllUsersToIm();

    /**
     * 确保指定业务用户已在腾讯云 IM 中开户（account_import，幂等）。
     * 用于 C2C 发消息前对方账号未导入导致的 20003（Invalid sender or receiver identifier）。
     */
    void ensureImAccountForUser(Long userId);

    /**
     * 单聊前强制校验：对方已在腾讯云 IM 开户；未配置或 REST 导入失败时抛出 {@link com.aipick.common.BusinessException}。
     */
    void assertPeerImportedToIm(Long peerUserId);
}
