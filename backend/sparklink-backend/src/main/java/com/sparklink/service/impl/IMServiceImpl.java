package com.sparklink.service.impl;

import com.sparklink.common.BusinessException;
import com.sparklink.config.IMConfig;
import com.sparklink.entity.User;
import com.sparklink.mapper.UserMapper;
import com.sparklink.service.IMService;
import com.sparklink.util.AvatarUtil;
import com.sparklink.util.MediaPathUtil;
import com.sparklink.util.IMUserSigUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 腾讯云 IM 服务实现
 *
 * @author AI-Pick
 */
@Service
public class IMServiceImpl implements IMService {

    private static final Logger log = LoggerFactory.getLogger(IMServiceImpl.class);
    private static final int NICK_MAX_CHARS = 100;

    private final IMUserSigUtil imUserSigUtil;
    private final IMConfig imConfig;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IMServiceImpl(IMUserSigUtil imUserSigUtil, IMConfig imConfig, UserMapper userMapper) {
        this.imUserSigUtil = imUserSigUtil;
        this.imConfig = imConfig;
        this.userMapper = userMapper;
    }

    private enum ImAccountImportResult {
        /** 缺少 sdkAppId、key 或管理员 identifier */
        SKIPPED_CONFIG,
        SUCCESS,
        FAILED
    }

    @Override
    public void ensureImAccountForUser(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        importUserToIm(u.getId(), u.getNickname(), AvatarUtil.sanitizeForResponse(u.getAvatar()));
    }

    @Override
    public void assertPeerImportedToIm(Long peerUserId) {
        if (peerUserId == null || peerUserId <= 0) {
            throw new BusinessException("对方用户 ID 无效");
        }
        User u = userMapper.selectById(peerUserId);
        if (u == null) {
            throw new BusinessException("对方用户不存在");
        }
        if (!isImAccountImportConfigured()) {
            throw new BusinessException(
                    "腾讯云 IM 未配齐：请在配置中填写 tencent.im.sdk-app-id、key、identifier（控制台「应用管理」中的管理员 UserID，须已创建）");
        }
        ImAccountImportResult r = accountImportOnce(
                u.getId(),
                u.getNickname(),
                AvatarUtil.sanitizeForResponse(u.getAvatar()));
        if (r == ImAccountImportResult.SKIPPED_CONFIG) {
            throw new BusinessException("IM 导入被跳过：请检查 tencent.im.identifier 等配置");
        }
        if (r == ImAccountImportResult.FAILED) {
            throw new BusinessException(
                    "腾讯云 IM 未成功导入该用户（account_import 失败）。请查看服务端日志「IM account_import 失败」中的 ErrorCode；"
                            + "并核对 REST 地址 tencent.im.rest-api-host、SDKAppID 与密钥是否与控制台一致");
        }
    }

    @Override
    public Map<String, Object> generateUserSig(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException("用户ID不能为空");
        }
        ensureImAccountForUser(userId);
        String userIdStr = String.valueOf(userId);
        String userSig = imUserSigUtil.generateUserSig(userIdStr);
        Map<String, Object> result = new HashMap<>(4);
        result.put("userId", userIdStr);
        result.put("sdkAppId", imConfig.getSdkAppId());
        result.put("userSig", userSig);
        result.put("expireSeconds", imConfig.getExpireSeconds());
        return result;
    }

    @Override
    public void importUserToIm(Long userId, String nickname, String faceUrl) {
        if (userId == null || userId <= 0) {
            return;
        }
        try {
            ImAccountImportResult r = accountImportOnce(userId, nickname, faceUrl);
            if (r == ImAccountImportResult.FAILED) {
                log.warn("IM account_import 未成功 userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("腾讯云 IM account_import 异常 userId={}", userId, e);
        }
    }

    @Override
    public Map<String, Object> batchImportAllUsersToIm() {
        if (!isImAccountImportConfigured()) {
            throw new BusinessException("IM 批量导入需配置 tencent.im.sdk-app-id、key、identifier（管理员 UserID）");
        }
        List<User> users = userMapper.selectList(null);
        int success = 0;
        int failed = 0;
        List<Long> failedUserIds = new ArrayList<>();
        for (User user : users) {
            if (user.getId() == null || user.getId() <= 0) {
                continue;
            }
            ImAccountImportResult r = accountImportOnce(
                    user.getId(),
                    user.getNickname(),
                    AvatarUtil.sanitizeForResponse(user.getAvatar()));
            if (r == ImAccountImportResult.SUCCESS) {
                success++;
            } else if (r == ImAccountImportResult.FAILED) {
                failed++;
                if (failedUserIds.size() < 100) {
                    failedUserIds.add(user.getId());
                }
            }
        }
        Map<String, Object> out = new HashMap<>(8);
        out.put("total", users.size());
        out.put("successCount", success);
        out.put("failedCount", failed);
        out.put("failedUserIds", failedUserIds);
        log.info("IM 批量导入完成 total={} success={} failed={}", users.size(), success, failed);
        return out;
    }

    private boolean isImAccountImportConfigured() {
        Long sdkAppId = imConfig.getSdkAppId();
        String adminIdentifier = imConfig.getIdentifier();
        String key = imConfig.getKey();
        return sdkAppId != null && sdkAppId > 0
                && StringUtils.hasText(key)
                && StringUtils.hasText(adminIdentifier);
    }

    /**
     * 同步调用 REST account_import（幂等；账号已存在时一般仍返回成功）
     */
    private ImAccountImportResult accountImportOnce(Long userId, String nickname, String faceUrl) {
        Long sdkAppId = imConfig.getSdkAppId();
        String adminIdentifier = imConfig.getIdentifier();
        String key = imConfig.getKey();
        if (sdkAppId == null || sdkAppId <= 0 || !StringUtils.hasText(key)) {
            log.debug("跳过 IM 账号导入：sdkAppId 或 key 未配置");
            return ImAccountImportResult.SKIPPED_CONFIG;
        }
        if (!StringUtils.hasText(adminIdentifier)) {
            log.warn("跳过 IM 账号导入：未配置 tencent.im.identifier（控制台管理员 UserID），"
                    + "请在 IM 控制台创建管理员账号并填入配置");
            return ImAccountImportResult.SKIPPED_CONFIG;
        }

        String adminId = adminIdentifier.trim();
        String adminSig = imUserSigUtil.generateUserSig(adminId);
        int random = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
        String host = imConfig.getRestApiHost();
        if (!StringUtils.hasText(host)) {
            host = "https://console.tim.qq.com";
        }
        host = host.trim().replaceAll("/$", "");

        URI uri = UriComponentsBuilder.fromHttpUrl(host)
                .path("/v4/im_open_login_svc/account_import")
                .queryParam("sdkappid", sdkAppId)
                .queryParam("identifier", adminId)
                .queryParam("usersig", adminSig)
                .queryParam("random", random)
                .queryParam("contenttype", "json")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();

        ObjectNode body = objectMapper.createObjectNode();
        body.put("UserID", String.valueOf(userId));
        if (StringUtils.hasText(nickname)) {
            body.put("Nick", truncateNick(nickname));
        }
        String faceForIm = resolveFaceUrlForIm(faceUrl);
        if (StringUtils.hasText(faceForIm)) {
            body.put("FaceUrl", faceForIm);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            log.warn("IM account_import 序列化请求体失败 userId={}", userId, e);
            return ImAccountImportResult.FAILED;
        }

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(uri, HttpMethod.POST,
                    new HttpEntity<>(jsonBody, headers), String.class);
        } catch (Exception e) {
            log.warn("IM account_import HTTP 调用失败 userId={} uri={}", userId, uri, e);
            return ImAccountImportResult.FAILED;
        }

        String respBody = response.getBody();
        if (!StringUtils.hasText(respBody)) {
            log.warn("IM account_import 响应为空 userId={}", userId);
            return ImAccountImportResult.FAILED;
        }
        try {
            JsonNode root = objectMapper.readTree(respBody);
            int errorCode = root.path("ErrorCode").asInt(-1);
            String errorInfo = root.path("ErrorInfo").asText("");
            if (errorCode != 0) {
                log.warn("IM account_import 失败 userId={} ErrorCode={} ErrorInfo={}", userId, errorCode, errorInfo);
                return ImAccountImportResult.FAILED;
            }
            log.debug("IM account_import 成功 userId={}", userId);
            return ImAccountImportResult.SUCCESS;
        } catch (Exception e) {
            log.warn("IM account_import 解析响应失败 userId={} body={}", userId, respBody, e);
            return ImAccountImportResult.FAILED;
        }
    }

    private static String truncateNick(String nickname) {
        if (nickname.length() <= NICK_MAX_CHARS) {
            return nickname;
        }
        return nickname.substring(0, NICK_MAX_CHARS);
    }

    /**
     * 生成 IM {@code FaceUrl}：本站头像必须为公网 HTTPS 可访问；库内 {@code /static/...} 由公网前缀拼接。
     * 若配置的 {@code avatar-public-base-url} 为 {@code http://公网IP:8080/api}，则改用 {@code avatar-public-https-base}，
     * 避免小程序与 TIM 客户端无法加载头像。
     */
    private String resolveFaceUrlForIm(String faceUrl) {
        if (!StringUtils.hasText(faceUrl)) {
            return null;
        }
        String pathOrHttps = toImFacePathOrExternalHttps(faceUrl.trim());
        if (!StringUtils.hasText(pathOrHttps)) {
            return null;
        }
        if (pathOrHttps.startsWith("https://")) {
            return pathOrHttps;
        }
        if (!pathOrHttps.startsWith("/static/")) {
            return null;
        }
        String base = pickImAvatarPublicBase(
                imConfig.getAvatarPublicBaseUrl(), imConfig.getAvatarPublicHttpsBase());
        if (!StringUtils.hasText(base)) {
            log.warn("IM FaceUrl 跳过：未配置可用的头像公网前缀（或 https 回退），path={}", pathOrHttps);
            return null;
        }
        return base + pathOrHttps;
    }

    /**
     * 收成 IM 可用的「/static/...」或外站 https 完整 URL；拒绝把内网 http 绝对地址原样写入 IM。
     */
    static String toImFacePathOrExternalHttps(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = AvatarUtil.sanitizeForResponse(raw);
        if (!StringUtils.hasText(s)) {
            s = MediaPathUtil.normalizeForResponse(raw.trim());
        }
        if (!StringUtils.hasText(s)) {
            return null;
        }
        s = s.trim();
        if (s.startsWith("https://")) {
            return s;
        }
        if (s.startsWith("/static/")) {
            return s;
        }
        if (s.startsWith("http://")) {
            String again = MediaPathUtil.normalizeForResponse(s);
            if (StringUtils.hasText(again) && again.startsWith("/static/")) {
                return again;
            }
        }
        return null;
    }

    /**
     * 主配置为 IPv4 主机时采用 HTTPS 回退前缀；否则使用主配置（去尾斜杠）。
     */
    static String pickImAvatarPublicBase(String primary, String httpsFallback) {
        String fb = trimImBase(httpsFallback);
        if (!StringUtils.hasText(primary)) {
            return fb;
        }
        String p = trimImBase(primary);
        if (!StringUtils.hasText(p)) {
            return fb;
        }
        try {
            URI u = URI.create(p);
            String host = u.getHost();
            if (host != null && host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
                return fb;
            }
        } catch (Exception ignored) {
            // 非标准 URI 时仍尝试作为主前缀使用
        }
        return p;
    }

    private static String trimImBase(String base) {
        if (!StringUtils.hasText(base)) {
            return null;
        }
        return base.trim().replaceAll("/+$", "");
    }
}
