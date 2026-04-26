package com.sparklink.config;

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
 * 用户表增加 location，供匹配度位置维与资料同步落库（原 exist=false 导致恒为 null）。
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class UserLocationColumnMigrationRunner implements ApplicationRunner {

    private static final String TABLE = "t_user";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (!hasColumn(conn, TABLE, "location")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "ALTER TABLE " + TABLE + " ADD COLUMN location VARCHAR(200) DEFAULT NULL "
                                    + "COMMENT '常驻/当前位置(城市或lat,lon GCJ-02)' AFTER tags");
                    log.info("[迁移] t_user.location 已添加");
                }
            }
        } catch (Exception e) {
            log.warn("[迁移] t_user.location 列检查/添加失败: {}", e.getMessage());
        }
    }

    private boolean hasColumn(Connection conn, String table, String column) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
}
