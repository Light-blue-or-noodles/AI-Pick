package com.aipick.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 敏感词过滤器 - 基于 Trie 树实现
 * 支持精确匹配、前缀匹配、后缀匹配、中间匹配和变体识别
 *
 * @author AI-Pick
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    private final TrieNode root = new TrieNode();
    private volatile boolean initialized = false;

    /**
     * Trie 树节点
     */
    private static class TrieNode {
        // 子节点映射
        Map<Character, TrieNode> children = new HashMap<>();
        // 是否为敏感词结尾
        boolean isEnd = false;
        // 完整敏感词（用于返回匹配的敏感词）
        String word = null;
    }

    /**
     * 初始化敏感词库
     */
    public synchronized void init() {
        if (initialized) {
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("sensitive/sensitive_words.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    // 跳过空行和注释行
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    // 转换为小写并添加
                    addWord(line.toLowerCase());
                }
            }
            initialized = true;
            log.info("敏感词库初始化完成，共加载 {} 个敏感词", countWords());
        } catch (IOException e) {
            log.error("敏感词库初始化失败", e);
            throw new RuntimeException("敏感词库初始化失败", e);
        }
    }

    /**
     * 添加敏感词到 Trie 树
     */
    private void addWord(String word) {
        if (word == null || word.isEmpty()) {
            return;
        }

        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd = true;
        node.word = word;
    }

    /**
     * 检测文本是否包含敏感词
     *
     * @param text 待检测文本
     * @return 检测结果，包含是否命中和匹配的敏感词
     */
    public SensitiveCheckResult check(String text) {
        if (!initialized) {
            init();
        }

        if (text == null || text.isEmpty()) {
            return SensitiveCheckResult.pass();
        }

        // 预处理文本：转小写、去除常见干扰字符
        String processedText = preprocess(text);

        // 遍历文本检查敏感词
        for (int i = 0; i < processedText.length(); i++) {
            TrieNode node = root;
            int j = i;
            while (j < processedText.length()) {
                char c = processedText.charAt(j);
                node = node.children.get(c);
                if (node == null) {
                    break;
                }
                if (node.isEnd) {
                    // 找到敏感词
                    return SensitiveCheckResult.fail(node.word);
                }
                j++;
            }
        }

        return SensitiveCheckResult.pass();
    }

    /**
     * 检测文本并返回所有命中的敏感词
     *
     * @param text 待检测文本
     * @return 所有命中的敏感词列表
     */
    public List<String> findAllSensitiveWords(String text) {
        if (!initialized) {
            init();
        }

        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        String processedText = preprocess(text);
        Set<String> foundWords = new HashSet<>();

        for (int i = 0; i < processedText.length(); i++) {
            TrieNode node = root;
            int j = i;
            while (j < processedText.length()) {
                char c = processedText.charAt(j);
                node = node.children.get(c);
                if (node == null) {
                    break;
                }
                if (node.isEnd) {
                    foundWords.add(node.word);
                }
                j++;
            }
        }

        return new ArrayList<>(foundWords);
    }

    /**
     * 替换敏感词为指定字符
     *
     * @param text        待处理文本
     * @param replacement 替换字符
     * @return 处理后的文本
     */
    public String replaceSensitiveWords(String text, char replacement) {
        if (!initialized) {
            init();
        }

        if (text == null || text.isEmpty()) {
            return text;
        }

        String processedText = preprocess(text);
        StringBuilder result = new StringBuilder(text);

        // 记录需要替换的位置
        List<int[]> replaceRanges = new ArrayList<>();

        for (int i = 0; i < processedText.length(); i++) {
            TrieNode node = root;
            int j = i;
            while (j < processedText.length()) {
                char c = processedText.charAt(j);
                node = node.children.get(c);
                if (node == null) {
                    break;
                }
                if (node.isEnd) {
                    // 记录替换范围
                    replaceRanges.add(new int[]{i, j + 1});
                }
                j++;
            }
        }

        // 从后向前替换，避免位置偏移
        for (int k = replaceRanges.size() - 1; k >= 0; k--) {
            int[] range = replaceRanges.get(k);
            for (int pos = range[0]; pos < range[1]; pos++) {
                result.setCharAt(pos, replacement);
            }
        }

        return result.toString();
    }

    /**
     * 预处理文本
     * - 转小写
     * - 去除常见干扰字符（空格、特殊符号等）
     */
    private String preprocess(String text) {
        if (text == null) {
            return "";
        }

        // 转小写
        String lower = text.toLowerCase();

        // 去除常见干扰字符
        StringBuilder sb = new StringBuilder();
        for (char c : lower.toCharArray()) {
            // 保留中文、英文、数字
            if (isChinese(c) || Character.isLetterOrDigit(c)) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 判断是否为中文字符
     */
    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * 统计敏感词数量
     */
    private int countWords() {
        return countWordsRecursive(root);
    }

    private int countWordsRecursive(TrieNode node) {
        int count = node.isEnd ? 1 : 0;
        for (TrieNode child : node.children.values()) {
            count += countWordsRecursive(child);
        }
        return count;
    }

    /**
     * 敏感词检测结果
     */
    public static class SensitiveCheckResult {
        private final boolean containsSensitiveWord;
        private final String matchedWord;

        private SensitiveCheckResult(boolean containsSensitiveWord, String matchedWord) {
            this.containsSensitiveWord = containsSensitiveWord;
            this.matchedWord = matchedWord;
        }

        public static SensitiveCheckResult pass() {
            return new SensitiveCheckResult(false, null);
        }

        public static SensitiveCheckResult fail(String matchedWord) {
            return new SensitiveCheckResult(true, matchedWord);
        }

        public boolean isContainsSensitiveWord() {
            return containsSensitiveWord;
        }

        public String getMatchedWord() {
            return matchedWord;
        }
    }
}
