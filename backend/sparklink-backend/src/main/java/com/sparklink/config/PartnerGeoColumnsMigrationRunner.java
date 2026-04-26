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
 * 搭子表增加 GCJ-02 经纬度，便于距离匹配与推荐。
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class PartnerGeoColumnsMigrationRunner implements ApplicationRunner {

    private static final String TABLE = "t_partner";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        try (Connection conn = dataSource.getConnection()) {
            if (!hasColumn(conn, TABLE, "latitude")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "ALTER TABLE " + TABLE + " ADD COLUMN latitude DECIMAL(10, 7) DEFAULT NULL "
                                    + "COMMENT '纬度 GCJ-02' AFTER location");
                    log.info("[迁移] t_partner.latitude 已添加");
                }
            }
            if (!hasColumn(conn, TABLE, "longitude")) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(
                            "ALTER TABLE " + TABLE + " ADD COLUMN longitude DECIMAL(11, 7) DEFAULT NULL "
                                    + "COMMENT '经度 GCJ-02' AFTER latitude");
                    log.info("[迁移] t_partner.longitude 已添加");
                }
            }
        } catch (Exception e) {
            log.warn("[迁移] 搭子经纬度列检查/添加失败: {}", e.getMessage());
        }
    }

    private boolean hasColumn(Connection conn, String table, String column) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        }
    }
}
