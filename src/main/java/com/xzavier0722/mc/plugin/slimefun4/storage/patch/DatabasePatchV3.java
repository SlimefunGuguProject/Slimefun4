package com.xzavier0722.mc.plugin.slimefun4.storage.patch;

import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_ID;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_INVENTORY_ITEM;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_INVENTORY_SLOT;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_LOCATION;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_UNIVERSAL_UUID;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.mysql.MysqlConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.postgresql.PostgreSqlConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.ISqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.ItemStackDataCodec;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Converts schema-2 inventory text values to the versioned binary ItemStack format. */
public final class DatabasePatchV3 extends DatabasePatch {
    private final Supplier<Logger> logger;

    public DatabasePatchV3() {
        this(Slimefun::logger);
    }

    DatabasePatchV3(Supplier<Logger> logger) {
        super(3);
        this.logger = logger;
    }

    @Override
    public void patch(Statement stmt, ISqlCommonConfig config) throws SQLException {
        var connection = stmt.getConnection();
        var prefix = config instanceof SqlCommonConfig commonConfig ? commonConfig.tablePrefix() : "";

        migrateInventoryTable(
                connection,
                config,
                SqlUtils.mapTable(DataScope.BACKPACK_INVENTORY, prefix),
                FIELD_BACKPACK_ID);
        migrateInventoryTable(
                connection,
                config,
                SqlUtils.mapTable(DataScope.BLOCK_INVENTORY, prefix),
                FIELD_LOCATION);
        migrateInventoryTable(
                connection,
                config,
                SqlUtils.mapTable(DataScope.UNIVERSAL_INVENTORY, prefix),
                FIELD_UNIVERSAL_UUID);
    }

    private void migrateInventoryTable(
            Connection connection, ISqlCommonConfig config, String table, String ownerField) throws SQLException {
        if (!tableExists(connection, table)) {
            return;
        }

        if (!isBinaryColumn(connection, table)) {
            convertColumnType(connection, config, table);
        }

        var selectSql = "SELECT "
                + ownerField
                + ", "
                + FIELD_INVENTORY_SLOT
                + ", "
                + FIELD_INVENTORY_ITEM
                + " FROM "
                + table;
        var updateSql = "UPDATE "
                + table
                + " SET "
                + FIELD_INVENTORY_ITEM
                + "=? WHERE "
                + ownerField
                + "=? AND "
                + FIELD_INVENTORY_SLOT
                + "=?";

        var migratedCount = 0;
        var failedCount = 0;
        try (var select = connection.createStatement();
                var rows = select.executeQuery(selectSql);
                var update = connection.prepareStatement(updateSql)) {
            var batchSize = 0;
            while (rows.next()) {
                var stored = toBytes(rows.getObject(FIELD_INVENTORY_ITEM));
                if (stored.length == 0 || !ItemStackDataCodec.isLegacy(stored)) {
                    continue;
                }

                try {
                    var item = ItemStackDataCodec.deserialize(stored);
                    if (item == null) {
                        throw new IllegalStateException("The legacy item decoded to null");
                    }

                    var migrated = ItemStackDataCodec.serialize(item);
                    if (migrated.length == 0) {
                        throw new IllegalStateException("The migrated item data is empty");
                    }

                    update.setBytes(1, migrated);
                    update.setObject(2, rows.getObject(ownerField));
                    update.setObject(3, rows.getObject(FIELD_INVENTORY_SLOT));
                    update.addBatch();
                    batchSize++;
                    migratedCount++;

                    if (batchSize >= 250) {
                        verifyBatch(update.executeBatch(), table);
                        batchSize = 0;
                    }
                } catch (Exception ex) {
                    failedCount++;
                    logger.get()
                            .log(
                                    Level.SEVERE,
                                    "Unable to migrate legacy item data: table="
                                            + table
                                            + ", "
                                            + ownerField
                                            + "="
                                            + rows.getObject(ownerField)
                                            + ", slot="
                                            + rows.getObject(FIELD_INVENTORY_SLOT),
                                    ex);
                }
            }

            if (batchSize > 0) {
                verifyBatch(update.executeBatch(), table);
            }
        }

        logger.get()
                .log(
                        Level.INFO,
                        "Item data migration completed: table={0}, migrated={1}, failed={2}",
                        new Object[] {table, migratedCount, failedCount});
        if (failedCount > 0) {
            throw new SQLException(
                    failedCount + " item record(s) could not be migrated; the database version was not updated");
        }
    }

    private static byte[] toBytes(Object value) throws SQLException {
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        if (value instanceof String string) {
            return string.getBytes(StandardCharsets.US_ASCII);
        }
        if (value == null) {
            return new byte[0];
        }
        throw new SQLException("Unsupported inventory column value type: " + value.getClass().getName());
    }

    private static void verifyBatch(int[] results, String table) throws SQLException {
        for (var result : results) {
            if (result == Statement.EXECUTE_FAILED) {
                throw new SQLException("An inventory row failed to migrate in " + table);
            }
        }
    }

    private static void convertColumnType(Connection connection, ISqlCommonConfig config, String table)
            throws SQLException {
        String sql = null;
        if (config instanceof MysqlConfig) {
            sql = "ALTER TABLE " + table + " MODIFY COLUMN " + FIELD_INVENTORY_ITEM + " MEDIUMBLOB NOT NULL";
        } else if (config instanceof PostgreSqlConfig) {
            sql = "ALTER TABLE "
                    + table
                    + " ALTER COLUMN "
                    + FIELD_INVENTORY_ITEM
                    + " TYPE BYTEA USING convert_to("
                    + FIELD_INVENTORY_ITEM
                    + ", 'UTF8')";
        }

        // SQLite uses dynamic typing and accepts BLOB values in existing schema-2 TEXT columns.
        if (sql != null) {
            try (var statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    private static boolean isBinaryColumn(Connection connection, String table) throws SQLException {
        for (var candidate : new String[] {
            table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)
        }) {
            try (var columns = connection.getMetaData().getColumns(
                    connection.getCatalog(), null, candidate, FIELD_INVENTORY_ITEM)) {
                if (!columns.next()) {
                    continue;
                }
                var type = columns.getInt("DATA_TYPE");
                return type == Types.BINARY
                        || type == Types.VARBINARY
                        || type == Types.LONGVARBINARY
                        || type == Types.BLOB;
            }
        }
        return false;
    }

    private static boolean tableExists(Connection connection, String table) throws SQLException {
        var metadata = connection.getMetaData();
        for (var candidate : new String[] {table, table.toUpperCase(Locale.ROOT), table.toLowerCase(Locale.ROOT)}) {
            try (var tables = metadata.getTables(connection.getCatalog(), null, candidate, new String[] {"TABLE"})) {
                if (tables.next()) {
                    return true;
                }
            }
        }
        return false;
    }
}
