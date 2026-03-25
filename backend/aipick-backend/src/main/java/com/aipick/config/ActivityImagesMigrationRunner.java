package com.aipick.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 启动时自动为 t_activity 表添加 images 列（若不存在）。
 * 避免依赖本机 mysql 客户端执行迁移脚本。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ActivityImagesMigrationRunner implements ApplicationRunner {

    private static final String TABLE_NAME = "t_activity";
    private static final String COLUMN_NAME = "images";
    private static final String ALTER_SQL = "ALTER TABLE t_activity ADD COLUMN images TEXT DEFAULT NULL COMMENT '多图JSON数组，首张为封面'";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (hasColumn(conn, TABLE_NAME, COLUMN_NAME)) {
                log.info("[迁移] t_activity.images 已存在，跳过");
                return;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(ALTER_SQL);
                log.info("[迁移] 已为 t_activity 添加 images 列");
            }
        } catch (Exception e) {
            log.warn("[迁移] 执行 add-activity-images 失败（若列已存在可忽略）: {}", e.getMessage());
        }
    }

    private boolean hasColumn(Connection conn, String table, String column) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
}
