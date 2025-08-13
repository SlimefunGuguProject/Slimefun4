package com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public abstract class SqlCommonConfig implements ISqlCommonConfig {
    protected final String host;
    protected final int port;
    protected final String database;
    protected final String tablePrefix;
    protected final String user;
    protected final String passwd;
    protected final boolean useSsl;
    protected final int maxConnection;

    public SqlCommonConfig(
            String host,
            int port,
            String database,
            String tablePrefix,
            String user,
            String passwd,
            boolean useSsl,
            int maxConnection) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.tablePrefix = tablePrefix;
        this.user = user;
        this.passwd = passwd;
        this.useSsl = useSsl;
        this.maxConnection = maxConnection;
    }

    public HikariDataSource createDataSource() {
        var config = new HikariConfig();
        config.setDriverClassName(driver());
        config.setJdbcUrl(jdbcUrl());
        config.setPoolName("SlimefunHikariPool");

        if (!user.isEmpty()) {
            config.setUsername(user);
        }

        if (!passwd.isEmpty()) {
            config.setPassword(passwd);
        }

        config.setMaximumPoolSize(maxConnection);

        config.setLeakDetectionThreshold(8000);

        return new HikariDataSource(config);
    }

    public String tablePrefix() {
        return tablePrefix;
    }
}
