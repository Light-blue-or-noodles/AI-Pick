package com.aipick.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 IM 配置
 *
 * @author AI-Pick
 */
@Configuration
@ConfigurationProperties(prefix = "tencent.im")
public class IMConfig {

    /**
     * IM 应用 SDKAppID
     */
    private Long sdkAppId;

    /**
     * 管理员账号（可选，保留用于后续 REST API 调用）
     */
    private String identifier;

    /**
     * UserSig 生成密钥（与控制台一致）
     */
    private String key;

    /**
     * UserSig 过期时间（秒）
     */
    private Long expireSeconds = 604800L;

    /**
     * IM REST API 域名（中国站默认 console.tim.qq.com），参见 REST API 文档
     */
    private String restApiHost = "https://console.tim.qq.com";

    /**
     * 头像公网访问前缀（与小程序合法 request 域名一致，不含尾斜杠）。
     * 库内为 /static/... 相对路径时，同步 IM FaceUrl 会拼成：{@code avatarPublicBaseUrl + path}；不配置则 IM 侧不传 FaceUrl（仅更新 Nick）。
     */
    private String avatarPublicBaseUrl = "";

    public Long getSdkAppId() {
        return sdkAppId;
    }

    public void setSdkAppId(Long sdkAppId) {
        this.sdkAppId = sdkAppId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(Long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }

    public String getRestApiHost() {
        return restApiHost;
    }

    public void setRestApiHost(String restApiHost) {
        this.restApiHost = restApiHost;
    }

    public String getAvatarPublicBaseUrl() {
        return avatarPublicBaseUrl;
    }

    public void setAvatarPublicBaseUrl(String avatarPublicBaseUrl) {
        this.avatarPublicBaseUrl = avatarPublicBaseUrl;
    }
}
