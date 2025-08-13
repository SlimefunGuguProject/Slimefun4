package com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.ISqlCommonConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public record SqliteConfig(String path, int maxConnection) implements ISqlCommonConfig {
    public HikariDataSource createDataSource() {
        var config = new HikariConfig();
        config.setDriverClassName(driver());
        config.setJdbcUrl(jdbcUrl());
        config.setPoolName("SlimefunHikariPool");
        config.setMaximumPoolSize(maxConnection);

        config.setLeakDetectionThreshold(8000);

        return new HikariDataSource(config);
    }

    public String jdbcUrl() {
        return "jdbc:sqlite:"
                + path
                + "?foreign_keys=on"
                + "&journal_mode=WAL"
                + "&synchronous=NORMAL"
                + "&locking_mode=NORMAL";
    }

    public String driver() {
        return "org.sqlite.JDBC";
    }
}
