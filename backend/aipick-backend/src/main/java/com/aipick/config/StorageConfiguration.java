package com.aipick.config;

import com.aipick.storage.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用存储相关配置项
 *
 * @author AI-Pick
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {
}
