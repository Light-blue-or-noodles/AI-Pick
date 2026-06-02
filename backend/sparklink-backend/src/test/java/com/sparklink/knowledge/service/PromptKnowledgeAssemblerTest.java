package com.sparklink.knowledge.service;

import com.sparklink.knowledge.model.KnowledgeRecord;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptKnowledgeAssemblerTest {

    @Test
    void shouldReturnOriginalMessageWhenNoValidRecords() {
        PromptKnowledgeAssembler assembler = new PromptKnowledgeAssembler(3, 50);
        KnowledgeRetrieveResult result = new KnowledgeRetrieveResult();
        result.setRecords(List.of(
                buildRecord("seg-1", "   ", 0.99D),
                buildRecord("seg-2", null, 0.98D)
        ));

        String prompt = assembler.merge("用户原始问题", result);

        assertEquals("用户原始问题", prompt);
    }

    @Test
    void shouldSortDedupeLimitAndTruncateRecords() {
        PromptKnowledgeAssembler assembler = new PromptKnowledgeAssembler(2, 5);
        KnowledgeRetrieveResult result = new KnowledgeRetrieveResult();
        result.setRecords(List.of(
                buildRecord("seg-1", "hello world", 0.50D),
                buildRecord("seg-2", "abcdefg", 0.90D),
                buildRecord("seg-2", "should be dropped", 0.80D),
                buildRecord("seg-3", "   ", 0.99D),
                buildRecord("seg-4", "xyzxyz", 0.70D)
        ));

        String prompt = assembler.merge("请总结", result);

        assertTrue(prompt.startsWith("请总结"));
        assertTrue(prompt.contains("1. abcde"));
        assertTrue(prompt.contains("2. xyzxy"));
        assertTrue(!prompt.contains("hello"));
        assertTrue(!prompt.contains("should be dropped"));
    }

    @Test
    void shouldTreatNullResultAsNoKnowledge() {
        PromptKnowledgeAssembler assembler = new PromptKnowledgeAssembler(4, 100);

        String prompt = assembler.merge("只返回用户消息", null);

        assertEquals("只返回用户消息", prompt);
    }

    @Test
    void shouldNotThrowWhenRecordsContainNullItem() {
        PromptKnowledgeAssembler assembler = new PromptKnowledgeAssembler(3, 50);
        KnowledgeRetrieveResult result = new KnowledgeRetrieveResult();
        List<KnowledgeRecord> records = new java.util.ArrayList<>();
        records.add(buildRecord("seg-1", "有效内容", 0.80D));
        records.add(null);
        records.add(buildRecord("seg-2", "另一个内容", 0.70D));
        result.setRecords(records);

        assertDoesNotThrow(() -> assembler.merge("问题", result));
    }

    @Test
    void shouldRejectIllegalConstructorArguments() {
        assertThrows(IllegalArgumentException.class, () -> new PromptKnowledgeAssembler(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new PromptKnowledgeAssembler(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> new PromptKnowledgeAssembler(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new PromptKnowledgeAssembler(1, -2));
    }

    @Test
    void shouldSortNonNullScoreBeforeNullScore() {
        PromptKnowledgeAssembler assembler = new PromptKnowledgeAssembler(3, 20);
        KnowledgeRetrieveResult result = new KnowledgeRetrieveResult();
        result.setRecords(List.of(
                buildRecord("seg-null", "null-score", null),
                buildRecord("seg-low", "score-low", 0.20D),
                buildRecord("seg-high", "score-high", 0.90D)
        ));

        String prompt = assembler.merge("排序测试", result);

        assertTrue(prompt.indexOf("1. score-high") < prompt.indexOf("2. score-low"));
        assertTrue(prompt.indexOf("2. score-low") < prompt.indexOf("3. null-score"));
    }

    private KnowledgeRecord buildRecord(String segmentId, String content, Double score) {
        KnowledgeRecord record = new KnowledgeRecord();
        record.setSegmentId(segmentId);
        record.setContent(content);
        record.setScore(score);
        return record;
    }
}
