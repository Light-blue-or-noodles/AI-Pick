package com.sparklink.service.impl;

import com.sparklink.config.AliyunContentSecurityConfig;
import com.sparklink.service.ContentModerationProvider;
import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.*;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 阿里云内容安全审核提供者
 * 实现文本审核和图片审核增强版 API 调用
 * 使用阿里云官方 SDK，支持 RAM 角色临时凭证
 *
 * @author AI-Pick
 */
@Slf4j
@Component
public class AliyunContentModerationProvider implements ContentModerationProvider {

    private final AliyunContentSecurityConfig config;
    private final ObjectMapper objectMapper;

    // 客户端缓存
    private volatile Client client;
    private volatile long credentialsExpireTime;

    // API 配置
    private static final String ENDPOINT = "green-cip.cn-beijing.aliyuncs.com";
    private static final String REGION_ID = "cn-beijing";

    public AliyunContentModerationProvider(AliyunContentSecurityConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        if (config.isEnabled()) {
            try {
                refreshClient();
                log.info("阿里云内容安全客户端初始化成功，认证方式: {}", config.getAuthTypeDescription());
            } catch (Exception e) {
                log.error("阿里云内容安全客户端初始化失败", e);
            }
        }
    }

    @Override
    public String getProviderName() {
        return "aliyun";
    }

    @Override
    public boolean isAvailable() {
        return config.isEnabled() && client != null;
    }

    /**
     * 获取或刷新客户端
     */
    private Client getClient() throws Exception {
        // 检查是否需要刷新凭证
        if (config.isUseRamRole() && System.currentTimeMillis() > credentialsExpireTime - 300000) {
            synchronized (this) {
                if (System.currentTimeMillis() > credentialsExpireTime - 300000) {
                    refreshClient();
                }
            }
        }
        return client;
    }

    /**
     * 刷新客户端（用于 RAM 角色凭证更新）
     */
    private void refreshClient() throws Exception {
        Config sdkConfig = new Config();
        sdkConfig.setAccessKeyId(config.getAccessKeyId());
        sdkConfig.setAccessKeySecret(config.getAccessKeySecret());

        // 如果有 SecurityToken（RAM 角色），设置它
        String securityToken = config.getSecurityToken();
        if (securityToken != null && !securityToken.isEmpty()) {
            sdkConfig.setSecurityToken(securityToken);
        }

        sdkConfig.setRegionId(REGION_ID);
        sdkConfig.setEndpoint(ENDPOINT);
        sdkConfig.setReadTimeout(10000);
        sdkConfig.setConnectTimeout(5000);

        this.client = new Client(sdkConfig);

        // 设置凭证过期时间（如果使用的是 RAM 角色）
        if (config.isUseRamRole()) {
            // 临时凭证通常有效期为 1 小时，我们设置 55 分钟后刷新
            this.credentialsExpireTime = System.currentTimeMillis() + 55 * 60 * 1000;
        }

        log.debug("阿里云内容安全客户端已刷新");
    }

    @Override
    public ModerationResult moderateText(TextModerationRequest request) {
        if (!isAvailable()) {
            log.debug("阿里云内容安全未启用或客户端未初始化，返回默认通过");
            return ModerationResult.pass(getProviderName(), ContentType.TEXT);
        }

        try {
            log.info("调用阿里云文本审核，dataId={}", request.getDataId());

            // 构建服务参数
            java.util.Map<String, String> serviceParameters = new java.util.HashMap<>();
            serviceParameters.put("content", request.getText());
            if (request.getDataId() != null) {
                serviceParameters.put("dataId", request.getDataId());
            }

            // 构建请求 - 使用阿里云 SDK 的类
            com.aliyun.green20220302.models.TextModerationRequest textRequest =
                    new com.aliyun.green20220302.models.TextModerationRequest();
            textRequest.setService(getTextServiceCode(request.getScene()));
            textRequest.setServiceParameters(objectMapper.writeValueAsString(serviceParameters));

            // 设置运行时选项
            RuntimeOptions runtime = new RuntimeOptions();
            runtime.readTimeout = 10000;
            runtime.connectTimeout = 5000;

            // 发送请求
            Client currentClient = getClient();
            com.aliyun.green20220302.models.TextModerationResponse response =
                    currentClient.textModerationWithOptions(textRequest, runtime);

            // 处理响应
            return parseTextModerationResponse(response, request.getDataId());

        } catch (Exception e) {
            log.error("阿里云文本审核失败: {}", e.getMessage(), e);
            // 失败时默认通过，避免阻塞业务
            return ModerationResult.pass(getProviderName(), ContentType.TEXT);
        }
    }

    @Override
    public List<ModerationResult> moderateTextBatch(List<TextModerationRequest> requests) {
        return requests.stream()
                .map(this::moderateText)
                .collect(Collectors.toList());
    }

    @Override
    public ModerationResult moderateImage(ImageModerationRequest request) {
        if (!isAvailable()) {
            log.debug("阿里云内容安全未启用或客户端未初始化，返回默认通过");
            return ModerationResult.pass(getProviderName(), ContentType.IMAGE);
        }

        try {
            log.info("调用阿里云图片审核，dataId={}", request.getDataId());

            // 构建服务参数
            java.util.Map<String, String> serviceParameters = new java.util.HashMap<>();
            if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
                serviceParameters.put("imageUrl", request.getImageUrl());
            }
            if (request.getImageData() != null && !request.getImageData().isEmpty()) {
                serviceParameters.put("imageData", request.getImageData());
            }
            if (request.getDataId() != null) {
                serviceParameters.put("dataId", request.getDataId());
            }

            // 构建请求 - 使用阿里云 SDK 的类
            com.aliyun.green20220302.models.ImageModerationRequest imageRequest =
                    new com.aliyun.green20220302.models.ImageModerationRequest();
            imageRequest.setService(getImageServiceCode(request.getScene()));
            imageRequest.setServiceParameters(objectMapper.writeValueAsString(serviceParameters));

            // 设置运行时选项
            RuntimeOptions runtime = new RuntimeOptions();
            runtime.readTimeout = 10000;
            runtime.connectTimeout = 5000;

            // 发送请求
            Client currentClient = getClient();
            com.aliyun.green20220302.models.ImageModerationResponse response =
                    currentClient.imageModerationWithOptions(imageRequest, runtime);

            // 处理响应
            return parseImageModerationResponse(response, request.getDataId());

        } catch (Exception e) {
            log.error("阿里云图片审核失败: {}", e.getMessage(), e);
            // 失败时默认通过，避免阻塞业务
            return ModerationResult.pass(getProviderName(), ContentType.IMAGE);
        }
    }

    @Override
    public List<ModerationResult> moderateImageBatch(List<ImageModerationRequest> requests) {
        return requests.stream()
                .map(this::moderateImage)
                .collect(Collectors.toList());
    }

    /**
     * 获取文本审核服务代码
     */
    private String getTextServiceCode(String scene) {
        if (scene == null || scene.isEmpty()) {
            return "chat_detection"; // 默认使用聊天检测
        }
        return scene;
    }

    /**
     * 获取图片审核服务代码
     */
    private String getImageServiceCode(String scene) {
        if (scene == null || scene.isEmpty()) {
            return "baselineCheck"; // 默认使用基线检测
        }
        return scene;
    }

    /**
     * 解析文本审核响应
     */
    private ModerationResult parseTextModerationResponse(
            com.aliyun.green20220302.models.TextModerationResponse response, String dataId) {
        if (response == null || response.getBody() == null) {
            log.warn("文本审核响应为空");
            return ModerationResult.pass(getProviderName(), ContentType.TEXT);
        }

        com.aliyun.green20220302.models.TextModerationResponseBody body = response.getBody();
        Integer code = body.getCode();

        if (code == null || code != 200) {
            String msg = body.getMessage();
            log.error("文本审核 API 返回错误: code={}, msg={}", code, msg);
            return ModerationResult.pass(getProviderName(), ContentType.TEXT);
        }

        com.aliyun.green20220302.models.TextModerationResponseBody.TextModerationResponseBodyData data = body.getData();
        if (data == null) {
            log.warn("文本审核响应缺少 Data 字段");
            return ModerationResult.pass(getProviderName(), ContentType.TEXT);
        }

        // 获取审核结果
        String labels = data.getLabels();
        String reason = data.getReason();

        // nonLabel 或空标签表示正常
        if (labels == null || labels.isEmpty() || "nonLabel".equals(labels)) {
            return ModerationResult.pass(getProviderName(), ContentType.TEXT);
        }

        // 有违规标签，返回 block
        return ModerationResult.block(getProviderName(), ContentType.TEXT, labels, 1.0f,
                "文本审核未通过: " + (reason != null ? reason : labels));
    }

    /**
     * 解析图片审核响应
     */
    private ModerationResult parseImageModerationResponse(
            com.aliyun.green20220302.models.ImageModerationResponse response, String dataId) {
        if (response == null || response.getBody() == null) {
            log.warn("图片审核响应为空");
            return ModerationResult.pass(getProviderName(), ContentType.IMAGE);
        }

        com.aliyun.green20220302.models.ImageModerationResponseBody body = response.getBody();
        Integer code = body.getCode();

        if (code == null || code != 200) {
            String msg = body.getMsg();
            log.error("图片审核 API 返回错误: code={}, msg={}", code, msg);
            return ModerationResult.pass(getProviderName(), ContentType.IMAGE);
        }

        com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyData data = body.getData();
        if (data == null) {
            log.warn("图片审核响应缺少 Data 字段");
            return ModerationResult.pass(getProviderName(), ContentType.IMAGE);
        }

        java.util.List<com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyDataResult>
                results = data.getResult();
        if (results == null || results.isEmpty()) {
            return ModerationResult.pass(getProviderName(), ContentType.IMAGE);
        }

        // 获取第一个结果
        com.aliyun.green20220302.models.ImageModerationResponseBody.ImageModerationResponseBodyDataResult result =
                results.get(0);
        String label = result.getLabel();
        Float confidence = result.getConfidence();

        // nonLabel 表示正常
        if (label == null || label.isEmpty() || "nonLabel".equals(label)) {
            return ModerationResult.pass(getProviderName(), ContentType.IMAGE);
        }

        // 根据置信度判断建议
        float conf = confidence != null ? confidence : 100.0f;
        Suggestion suggestion = conf >= config.getImageReviewThreshold()
                ? Suggestion.BLOCK : Suggestion.REVIEW;

        return ModerationResult.block(getProviderName(), ContentType.IMAGE, label, conf,
                "图片审核未通过: " + label);
    }
}