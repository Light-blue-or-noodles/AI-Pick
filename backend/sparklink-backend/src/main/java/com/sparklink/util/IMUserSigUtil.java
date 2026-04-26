package com.sparklink.util;

import com.sparklink.common.BusinessException;
import com.sparklink.config.IMConfig;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.zip.Deflater;

/**
 * 生成腾讯云 IM UserSig 工具
 *
 * @author AI-Pick
 */
@Component
public class IMUserSigUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private final IMConfig imConfig;

    public IMUserSigUtil(IMConfig imConfig) {
        this.imConfig = imConfig;
    }

    /**
     * 生成指定用户的 UserSig
     *
     * @param userId 用户 ID
     * @return UserSig 字符串
     */
    public String generateUserSig(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new BusinessException("用户标识不能为空");
        }
        Long sdkAppId = imConfig.getSdkAppId();
        String key = imConfig.getKey();
        Long expire = imConfig.getExpireSeconds();
        if (sdkAppId == null || sdkAppId <= 0) {
            throw new BusinessException("腾讯 IM 配置错误：sdkAppId 未配置");
        }
        if (key == null || key.trim().isEmpty()) {
            throw new BusinessException("腾讯 IM 配置错误：key 未配置");
        }
        if (expire == null || expire <= 0) {
            throw new BusinessException("腾讯 IM 配置错误：expireSeconds 非法");
        }

        long currTime = System.currentTimeMillis() / 1000;
        String contentToBeSigned = "TLS.identifier:" + userId + "\n"
                + "TLS.sdkappid:" + sdkAppId + "\n"
                + "TLS.time:" + currTime + "\n"
                + "TLS.expire:" + expire + "\n";

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            String sig = Base64.getEncoder().encodeToString(mac.doFinal(contentToBeSigned.getBytes(StandardCharsets.UTF_8)));

            String json = "{\"TLS.ver\":\"2.0\","
                    + "\"TLS.identifier\":\"" + jsonEscape(userId) + "\","
                    + "\"TLS.sdkappid\":" + sdkAppId + ","
                    + "\"TLS.expire\":" + expire + ","
                    + "\"TLS.time\":" + currTime + ","
                    + "\"TLS.sig\":\"" + sig + "\"}";

            return base64UrlEncode(deflate(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException("生成 UserSig 失败");
        }
    }

    private static byte[] deflate(byte[] input) {
        Deflater compressor = new Deflater();
        try {
            compressor.setInput(input);
            compressor.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
            byte[] buffer = new byte[1024];
            while (!compressor.finished()) {
                int n = compressor.deflate(buffer);
                if (n > 0) {
                    out.write(buffer, 0, n);
                }
            }
            return out.toByteArray();
        } finally {
            compressor.end();
        }
    }

    private static String base64UrlEncode(byte[] data) {
        return Base64.getEncoder()
                .encodeToString(data)
                .replace('+', '*')
                .replace('/', '-')
                .replace('=', '_');
    }

    private static String jsonEscape(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
