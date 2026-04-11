package com.aipick.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 阿里云内容安全配置
 * 支持两种认证方式：
 * 1. ECS RAM 角色（推荐，更安全）- 自动获取临时凭证
 * 2. AccessKey（兼容旧方式）
 *
 * @author AI-Pick
 */
@Slf4j
@Configuration
@Data
public class AliyunContentSecurityConfig {

    @Value("${aliyun.content-security.enabled:false}")
    private boolean enabled;

    @Value("${aliyun.content-security.use-ram-role:true}")
    private boolean useRamRole;

    @Value("${aliyun.content-security.ram-role-name:aipick-content-security-role}")
    private String ramRoleName;

    @Value("${aliyun.content-security.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.content-security.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.content-security.region:cn-beijing}")
    private String region;

    @Value("${aliyun.content-security.text-review-on-suspicious:false}")
    private boolean textReviewOnSuspicious;

    @Value("${aliyun.content-security.image-review-threshold:60}")
    private int imageReviewThreshold;

    @Value("${aliyun.content-security.text-review-threshold:60}")
    private int textReviewThreshold;

    // 临时凭证（RAM 角色使用）
    private volatile String securityToken;
    private volatile long credentialsExpireTime;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ECS 元数据服务地址
    private static final String ECS_METADATA_URL = "http://100.100.100.200/latest/meta-data/ram/security-credentials/";

    @PostConstruct
    public void init() {
        if (enabled && useRamRole) {
            refreshCredentials();
        }
    }

    /**
     * 获取 AccessKeyId
     */
    public String getAccessKeyId() {
        if (useRamRole) {
            refreshCredentialsIfNeeded();
            // 从 ECS 元数据获取
            return getRamRoleCredentials().getAccessKeyId();
        }
        return accessKeyId;
    }

    /**
     * 获取 AccessKeySecret
     */
    public String getAccessKeySecret() {
        if (useRamRole) {
            refreshCredentialsIfNeeded();
            return getRamRoleCredentials().getAccessKeySecret();
        }
        return accessKeySecret;
    }

    /**
     * 获取 SecurityToken
     */
    public String getSecurityToken() {
        if (useRamRole) {
            refreshCredentialsIfNeeded();
            return securityToken;
        }
        return null;
    }

    /**
     * 如果需要，刷新凭证
     */
    private void refreshCredentialsIfNeeded() {
        if (System.currentTimeMillis() > credentialsExpireTime - 300000) { // 提前5分钟刷新
            refreshCredentials();
        }
    }

    /**
     * 刷新 RAM 角色凭证
     */
    private synchronized void refreshCredentials() {
        try {
            String metadataUrl = ECS_METADATA_URL + ramRoleName;
            URL url = new URL(metadataUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.error("获取 RAM 角色凭证失败: HTTP {}", responseCode);
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JsonNode jsonNode = objectMapper.readTree(response.toString());
                String code = jsonNode.get("Code").asText();

                if (!"Success".equals(code)) {
                    log.error("获取 RAM 角色凭证失败: {}", code);
                    return;
                }

                this.accessKeyId = jsonNode.get("AccessKeyId").asText();
                this.accessKeySecret = jsonNode.get("AccessKeySecret").asText();
                this.securityToken = jsonNode.get("SecurityToken").asText();

                // 解析过期时间
                String expirationStr = jsonNode.get("Expiration").asText();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date expirationDate = sdf.parse(expirationStr);
                this.credentialsExpireTime = expirationDate.getTime();

                log.info("成功刷新 RAM 角色凭证，过期时间: {}", expirationStr);
            }
        } catch (Exception e) {
            log.error("刷新 RAM 角色凭证失败", e);
        }
    }

    /**
     * 获取 RAM 角色凭证（内部使用）
     */
    private RamRoleCredentials getRamRoleCredentials() {
        return new RamRoleCredentials(accessKeyId, accessKeySecret, securityToken);
    }

    /**
     * 获取认证方式描述
     */
    public String getAuthTypeDescription() {
        if (!enabled) {
            return "未启用";
        }
        if (useRamRole) {
            return "ECS RAM 角色 (" + ramRoleName + ")";
        }
        return "AccessKey";
    }

    /**
     * RAM 角色凭证封装
     */
    private static class RamRoleCredentials {
        private final String accessKeyId;
        private final String accessKeySecret;
        private final String securityToken;

        public RamRoleCredentials(String accessKeyId, String accessKeySecret, String securityToken) {
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
            this.securityToken = securityToken;
        }

        public String getAccessKeyId() { return accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public String getSecurityToken() { return securityToken; }
    }
}
