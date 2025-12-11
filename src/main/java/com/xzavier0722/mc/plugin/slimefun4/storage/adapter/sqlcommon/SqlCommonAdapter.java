package com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon;

import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_TABLE_METADATA_KEY;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_TABLE_METADATA_VALUE;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_TABLE_VERSION;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.METADATA_VERSION;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.TABLE_NAME_TABLE_INFORMATION;

import city.norain.slimefun4.timings.entry.SQLEntry;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.IDataSourceAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.FieldKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import com.xzavier0722.mc.plugin.slimefun4.storage.patch.DatabasePatch;
import com.xzavier0722.mc.plugin.slimefun4.storage.patch.DatabasePatchV1;
import com.xzavier0722.mc.plugin.slimefun4.storage.patch.DatabasePatchV2;
import com.zaxxer.hikari.HikariDataSource;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;

public abstract class SqlCommonAdapter<T extends ISqlCommonConfig> implements IDataSourceAdapter<T> {
    protected HikariDataSource ds;
    protected String profileTable, researchTable, backpackTable, bpInvTable;
    protected String blockRecordTable,
            blockDataTable,
            universalRecordTable,
            universalDataTable,
            chunkDataTable,
            blockInvTable,
            universalInvTable;
    protected String tableMetadataTable;
    protected T config;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    @Override
    public void prepare(T config) {
        this.config = config;
        ds = config.createDataSource();
    }

    protected void executeSql(String sql) {
        var entry = new SQLEntry(sql);
        Slimefun.getSQLProfiler().recordEntry(entry);
        Connection conn = null;
        try {
            conn = getConnection();
            SqlUtils.execSql(conn, sql);
        } catch (SQLException e) {
            throw new IllegalStateException("An exception thrown while executing sql: " + sql, e);
        } finally {
            try {
                releaseConnection(conn);
            } catch (SQLException e) {
                throw new IllegalStateException("An exception thrown while releasing connection for sql: " + sql, e);
            } finally {
                Slimefun.getSQLProfiler().finishEntry(entry);
            }
        }
    }

    protected List<RecordSet> executeQuery(String sql) {
        var entry = new SQLEntry(sql);
        Slimefun.getSQLProfiler().recordEntry(entry);

        Connection conn = null;
        try {
            conn = getConnection();
            return SqlUtils.execQuery(conn, sql);
        } catch (SQLException e) {
            throw new IllegalStateException("An exception thrown while executing sql: " + sql, e);
        } finally {
            try {
                releaseConnection(conn);
            } catch (SQLException e) {
                throw new IllegalStateException("An exception thrown while releasing connection for sql: " + sql, e);
            } finally {
                Slimefun.getSQLProfiler().finishEntry(entry);
            }
        }
    }

    protected String mapTable(DataScope scope) {
        return switch (scope) {
            case PLAYER_PROFILE -> profileTable;
            case BACKPACK_INVENTORY -> bpInvTable;
            case BACKPACK_PROFILE -> backpackTable;
            case PLAYER_RESEARCH -> researchTable;
            case BLOCK_INVENTORY -> blockInvTable;
            case CHUNK_DATA -> chunkDataTable;
            case BLOCK_DATA -> blockDataTable;
            case BLOCK_RECORD -> blockRecordTable;
            case UNIVERSAL_INVENTORY -> universalInvTable;
            case UNIVERSAL_RECORD -> universalRecordTable;
            case UNIVERSAL_DATA -> universalDataTable;
            case TABLE_METADATA -> tableMetadataTable;
            case NONE -> throw new IllegalArgumentException("NONE cannot be a storage data scope!");
        };
    }

    @Override
    public void shutdown() {
        ds.close();
        ds = null;
        profileTable = null;
        researchTable = null;
        backpackTable = null;
        bpInvTable = null;
        blockDataTable = null;
        blockRecordTable = null;
        chunkDataTable = null;
        blockInvTable = null;
        universalInvTable = null;
        universalDataTable = null;
        universalRecordTable = null;
        tableMetadataTable = null;
    }

    public int getDatabaseVersion() {
        if (Slimefun.isNewlyInstalled()) {
            return IDataSourceAdapter.DATABASE_VERSION;
        }

        var query = executeQuery(String.format(
                "SELECT (%s) FROM %s WHERE %s='%s';",
                FIELD_TABLE_METADATA_VALUE, tableMetadataTable, FIELD_TABLE_METADATA_KEY, METADATA_VERSION));

        if (query.isEmpty()) {
            try {
                var prefix = config instanceof SqlCommonConfig sqc ? sqc.tablePrefix() : "";
                var fallbackQuery = executeQuery(
                        "SELECT (" + FIELD_TABLE_VERSION + ") FROM " + (prefix + TABLE_NAME_TABLE_INFORMATION));

                if (fallbackQuery.isEmpty()) {
                    return 0;
                }

                return fallbackQuery.get(0).getInt(null);
            } catch (Exception e) {
                return 0;
            }
        } else {
            return query.get(0).getInt(FieldKey.METADATA_VALUE);
        }
    }

    @Override
    public void patch() {
        DatabasePatch patch = null;
        var dbVer = getDatabaseVersion();

        Slimefun.logger().log(Level.INFO, "当前数据库版本 {0}", new Object[] {dbVer});

        switch (dbVer) {
            case 0 -> patch = new DatabasePatchV1();
            case 1 -> patch = new DatabasePatchV2();
        }

        if (patch == null) {
            return;
        }

        try (var conn = ds.getConnection()) {
            Slimefun.logger().log(Level.INFO, "正在更新数据库版本至 " + patch.getVersion() + ", 可能需要一段时间...");
            var stmt = conn.createStatement();
            patch.updateVersion(stmt, config);
            patch.patch(stmt, config);
            Slimefun.logger().log(Level.INFO, "更新完成. ");

            if (getDatabaseVersion() != IDataSourceAdapter.DATABASE_VERSION) {
                patch();
            }
        } catch (SQLException e) {
            Slimefun.logger().log(Level.SEVERE, "更新数据库时出现问题!", e);
        }
    }

    @Override
    public void beginTransaction() {
        if (transactionConnection.get() != null) {
            throw new IllegalStateException("Transaction already started on current thread.");
        }

        try {
            var conn = ds.getConnection();
            conn.setAutoCommit(false);
            transactionConnection.set(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("An exception thrown while beginning transaction", e);
        }
    }

    @Override
    public void commitTransaction() {
        var conn = transactionConnection.get();
        if (conn == null) {
            Slimefun.logger()
                    .log(
                            Level.WARNING,
                            "Attempted to commit without an active transaction on thread: {0}",
                            Thread.currentThread().getName());
            return;
        }

        try {
            conn.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("An exception thrown while committing transaction", e);
        } finally {
            endTransaction(conn);
        }
    }

    @Override
    public void rollbackTransaction() {
        var conn = transactionConnection.get();
        if (conn == null) {
            Slimefun.logger()
                    .log(
                            Level.WARNING,
                            "Attempted to roll back without an active transaction on thread: {0}",
                            Thread.currentThread().getName());
            return;
        }

        try {
            conn.rollback();
        } catch (SQLException e) {
            throw new IllegalStateException("An exception thrown while rolling back transaction", e);
        } finally {
            endTransaction(conn);
        }
    }

    private void endTransaction(Connection conn) {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            Slimefun.logger().log(Level.SEVERE, "Failed to reset auto-commit after transaction.", e);
        }

        try {
            conn.close();
        } catch (SQLException e) {
            Slimefun.logger().log(Level.SEVERE, "Failed to close connection after transaction.", e);
        }

        transactionConnection.remove();
    }

    private Connection getConnection() throws SQLException {
        var conn = transactionConnection.get();
        if (conn != null) {
            return conn;
        }
        return ds.getConnection();
    }

    private void releaseConnection(Connection conn) throws SQLException {
        if (conn == null || transactionConnection.get() == conn) {
            return;
        }

        conn.close();
    }
}
