package com.sparklink.service.impl;

import com.sparklink.service.ContentModerationProvider;
import com.sparklink.util.SensitiveWordFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 本地敏感词审核提供者
 * 基于本地 Trie 树实现的敏感词过滤
 *
 * @author AI-Pick
 */
@Slf4j
@Component
public class LocalSensitiveWordProvider implements ContentModerationProvider {

    private final SensitiveWordFilter sensitiveWordFilter;

    public LocalSensitiveWordProvider(SensitiveWordFilter sensitiveWordFilter) {
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    @Override
    public String getProviderName() {
        return "local-sensitive-word";
    }

    @Override
    public boolean isAvailable() {
        return true; // 本地敏感词库始终可用
    }

    @Override
    public ModerationResult moderateText(TextModerationRequest request) {
        SensitiveWordFilter.SensitiveCheckResult result = sensitiveWordFilter.check(request.getText());

        if (result.isContainsSensitiveWord()) {
            return ModerationResult.block(
                    getProviderName(),
                    ContentType.TEXT,
                    "sensitive_word",
                    100f,
                    "包含敏感词: " + result.getMatchedWord()
            );
        }

        return ModerationResult.pass(getProviderName(), ContentType.TEXT);
    }

    @Override
    public List<ModerationResult> moderateTextBatch(List<TextModerationRequest> requests) {
        return requests.stream()
                .map(this::moderateText)
                .collect(Collectors.toList());
    }

    @Override
    public ModerationResult moderateImage(ImageModerationRequest request) {
        // 本地敏感词库不支持图片审核
        return ModerationResult.error(getProviderName(), "本地敏感词库不支持图片审核");
    }

    @Override
    public List<ModerationResult> moderateImageBatch(List<ImageModerationRequest> requests) {
        return requests.stream()
                .map(this::moderateImage)
                .collect(Collectors.toList());
    }

    /**
     * 查找文本中所有敏感词
     */
    public List<String> findAllSensitiveWords(String text) {
        return sensitiveWordFilter.findAllSensitiveWords(text);
    }

    /**
     * 替换敏感词
     */
    public String replaceSensitiveWords(String text, char replacement) {
        return sensitiveWordFilter.replaceSensitiveWords(text, replacement);
    }
}
