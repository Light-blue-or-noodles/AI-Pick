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
 * 启动时为 t_partner 增加 preference、scope 列（若不存在）。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PartnerColumnsMigrationRunner implements ApplicationRunner {

    private static final String TABLE = "t_partner";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (!hasColumn(conn, TABLE, "preference")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE t_partner ADD COLUMN preference VARCHAR(512) DEFAULT NULL COMMENT '搭子偏好，逗号分隔标签' AFTER content");
                    log.info("[迁移] t_partner.preference 已添加");
                }
            }
            if (!hasColumn(conn, TABLE, "scope")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE t_partner ADD COLUMN scope TINYINT NOT NULL DEFAULT 1 COMMENT '可见范围位掩码 1公开 2同事 4校友' AFTER preference");
                    log.info("[迁移] t_partner.scope 已添加");
                }
            }
            widenPreferenceColumn(conn);
            migrateScopeEnumToBitmask(conn);
        } catch (Exception e) {
            log.warn("[迁移] 搭子列检查/添加失败（若列已存在可忽略）: {}", e.getMessage());
        }
    }

    /**
     * 偏好为多标签逗号拼接，需长于 VARCHAR(100)
     */
    private void widenPreferenceColumn(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                    "ALTER TABLE " + TABLE + " MODIFY COLUMN preference VARCHAR(512) DEFAULT NULL COMMENT '搭子偏好，逗号分隔标签'");
            log.info("[迁移] t_partner.preference 已调整为 VARCHAR(512)");
        } catch (Exception e) {
            log.debug("[迁移] preference 列宽调整跳过或已满足: {}", e.getMessage());
        }
    }

    /**
     * 历史单选枚举 3 表示「校友」，位掩码下校友为 4
     */
    private void migrateScopeEnumToBitmask(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            int n = stmt.executeUpdate("UPDATE " + TABLE + " SET scope = 4 WHERE scope = 3");
            if (n > 0) {
                log.info("[迁移] t_partner.scope 历史值 3 → 4（校友位）: {} 行", n);
            }
        } catch (Exception e) {
            log.debug("[迁移] scope 历史值转换跳过: {}", e.getMessage());
        }
    }

    private boolean hasColumn(Connection conn, String table, String column) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
}
