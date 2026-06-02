package com.sparklink.knowledge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DifyKnowledgePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldBindDefaultValuesWhenNoExternalProperties() {
        contextRunner.run(context -> {
            DifyKnowledgeProperties properties = context.getBean(DifyKnowledgeProperties.class);
            assertEquals("https://api.dify.ai/v1", properties.getBaseUrl());
            assertEquals(6000L, properties.getTimeoutMs());
            assertEquals(6, properties.getRetrieve().getTopK());
            assertTrue(properties.getRetrieve().isThresholdEnabled());
            assertEquals(0.55D, properties.getRetrieve().getScoreThreshold(), 0.000001D);
            assertEquals("hybrid_search", properties.getRetrieve().getSearchMethod());
            assertEquals(4, properties.getRetrieve().getMaxInjectRecords());
            assertEquals(400, properties.getRetrieve().getMaxInjectCharsPerRecord());
        });
    }

    @Test
    void shouldOverrideDefaultsWhenExternalPropertiesProvided() {
        contextRunner.withPropertyValues(
                "app.dify.base-url=https://example.dify.test/v1",
                "app.dify.timeout-ms=9000",
                "app.dify.retrieve.top-k=9",
                "app.dify.retrieve.score-threshold=0.78",
                "app.dify.retrieve.search-method=full_text_search",
                "app.dify.retrieve.max-inject-records=8",
                "app.dify.retrieve.max-inject-chars-per-record=1200")
                .run(context -> {
                    DifyKnowledgeProperties properties = context.getBean(DifyKnowledgeProperties.class);
                    assertEquals("https://example.dify.test/v1", properties.getBaseUrl());
                    assertEquals(9000L, properties.getTimeoutMs());
                    assertEquals(9, properties.getRetrieve().getTopK());
                    assertEquals(0.78D, properties.getRetrieve().getScoreThreshold(), 0.000001D);
                    assertEquals("full_text_search", properties.getRetrieve().getSearchMethod());
                    assertEquals(8, properties.getRetrieve().getMaxInjectRecords());
                    assertEquals(1200, properties.getRetrieve().getMaxInjectCharsPerRecord());
                });
    }

    @Test
    void shouldFailFastWhenThresholdOutOfRange() {
        contextRunner.withPropertyValues("app.dify.retrieve.score-threshold=1.2")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DifyKnowledgeProperties.class)
    static class TestConfig {
    }
}
