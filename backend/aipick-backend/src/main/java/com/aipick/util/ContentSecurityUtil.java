package com.aipick.util;

import com.aipick.service.ContentSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 内容安全工具类
 * 提供便捷的内容审核方法
 *
 * @author AI-Pick
 */
@Slf4j
@Component
public class ContentSecurityUtil {

    private static ContentSecurityService contentSecurityService;

    public ContentSecurityUtil(ContentSecurityService contentSecurityService) {
        ContentSecurityUtil.contentSecurityService = contentSecurityService;
    }

    /**
     * 检查文本内容是否通过审核
     *
     * @param text 待审核文本
     * @return true - 通过，false - 未通过
     */
    public static boolean isTextSafe(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        ContentSecurityService.ContentCheckResult result = contentSecurityService.checkText(text);
        return result.isPassed();
    }

    /**
     * 检查文本内容并返回详细结果
     *
     * @param text 待审核文本
     * @return 审核结果
     */
    public static ContentSecurityService.ContentCheckResult checkText(String text) {
        return contentSecurityService.checkText(text);
    }

    /**
     * 检查图片内容是否通过审核
     *
     * @param imageUrl 图片URL
     * @return true - 通过，false - 未通过
     */
    public static boolean isImageSafe(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return true;
        }
        ContentSecurityService.ContentModerationResult result = contentSecurityService.checkImage(imageUrl);
        return result.isPassed();
    }

    /**
     * 检查图片内容并返回详细结果
     *
     * @param imageUrl 图片URL
     * @return 审核结果
     */
    public static ContentSecurityService.ContentModerationResult checkImage(String imageUrl) {
        return contentSecurityService.checkImage(imageUrl);
    }

    /**
     * 检查文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return true - 包含敏感词，false - 不包含
     */
    public static boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        ContentSecurityService.SensitiveWordResult result = contentSecurityService.checkSensitiveWords(text);
        return result.isContainsSensitiveWord();
    }

    /**
     * 批量检查文本列表
     *
     * @param texts 待检测文本列表
     * @return 包含敏感词的文本索引列表
     */
    public static List<Integer> batchCheckSensitiveWords(List<String> texts) {
        return contentSecurityService.batchCheckSensitiveWords(texts);
    }

    /**
     * 验证文本内容，未通过时抛出异常
     *
     * @param text    待审核文本
     * @param fieldName 字段名称（用于错误提示）
     * @throws IllegalArgumentException 审核未通过时抛出
     */
    public static void validateText(String text, String fieldName) {
        ContentSecurityService.ContentCheckResult result = checkText(text);
        if (!result.isPassed()) {
            String message = String.format("%s包含违规内容: %s", fieldName, result.getMessage());
            log.warn("内容审核未通过 - {}: {}", fieldName, result.getMessage());
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 验证图片内容，未通过时抛出异常
     *
     * @param imageUrl  图片URL
     * @param fieldName 字段名称（用于错误提示）
     * @throws IllegalArgumentException 审核未通过时抛出
     */
    public static void validateImage(String imageUrl, String fieldName) {
        ContentSecurityService.ContentModerationResult result = checkImage(imageUrl);
        if (!result.isPassed()) {
            String message = String.format("%s包含违规内容: %s", fieldName, result.getLabel());
            log.warn("图片审核未通过 - {}: {}", fieldName, result.getLabel());
            throw new IllegalArgumentException(message);
        }
    }
}
