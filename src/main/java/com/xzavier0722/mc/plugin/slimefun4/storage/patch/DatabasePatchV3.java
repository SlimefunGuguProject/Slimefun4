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
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabasePatchV3 extends DatabasePatch {
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
        var prefix = config instanceof SqlCommonConfig sqlConfig ? sqlConfig.tablePrefix() : "";
        migrateTable(stmt, config, SqlUtils.mapTable(DataScope.BACKPACK_INVENTORY, prefix), FIELD_BACKPACK_ID);
        migrateTable(stmt, config, SqlUtils.mapTable(DataScope.BLOCK_INVENTORY, prefix), FIELD_LOCATION);
        migrateTable(stmt, config, SqlUtils.mapTable(DataScope.UNIVERSAL_INVENTORY, prefix), FIELD_UNIVERSAL_UUID);
    }

    private void migrateTable(Statement stmt, ISqlCommonConfig config, String table, String ownerField)
            throws SQLException {
        if (!tableExists(stmt, table)) {
            return;
        }

        migrateColumnType(stmt, config, table);

        var selectSql =
                "SELECT " + ownerField + ", " + FIELD_INVENTORY_SLOT + ", " + FIELD_INVENTORY_ITEM + " FROM " + table;
        var updateSql = "UPDATE " + table + " SET " + FIELD_INVENTORY_ITEM + "=? WHERE " + ownerField + "=? AND "
                + FIELD_INVENTORY_SLOT + "=?";
        var migrated = 0;
        var failed = 0;

        try (var result = stmt.executeQuery(selectSql);
                var update = stmt.getConnection().prepareStatement(updateSql)) {
            while (result.next()) {
                var storedItem = result.getObject(FIELD_INVENTORY_ITEM);
                var serializedItem = storedItem instanceof byte[] bytes
                        ? bytes
                        : storedItem.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII);
                if (!ItemStackDataCodec.isLegacy(serializedItem)) {
                    continue;
                }

                try {
                    var itemStack = ItemStackDataCodec.deserialize(serializedItem);
                    if (itemStack == null) {
                        continue;
                    }

                    var migratedItem = ItemStackDataCodec.serialize(itemStack);
                    if (migratedItem.length == 0) {
                        throw new IllegalStateException("迁移后的物品数据为空");
                    }
                    update.setBytes(1, migratedItem);
                    update.setString(2, result.getString(ownerField));
                    update.setInt(3, result.getInt(FIELD_INVENTORY_SLOT));
                    update.addBatch();
                    migrated++;
                } catch (Exception exception) {
                    failed++;
                    logger.get()
                            .log(
                                    Level.SEVERE,
                                    "无法迁移旧版物品数据: table=" + table + ", " + ownerField + "="
                                            + result.getString(ownerField) + ", slot="
                                            + result.getInt(FIELD_INVENTORY_SLOT),
                                    exception);
                }
            }
            update.executeBatch();
        }

        logger.get()
                .log(Level.INFO, "物品数据迁移完成: table={0}, migrated={1}, failed={2}", new Object[] {table, migrated, failed
                });

        if (failed > 0) {
            throw new SQLException("仍有 " + failed + " 条物品数据无法迁移，数据库版本未更新");
        }
    }

    private void migrateColumnType(Statement stmt, ISqlCommonConfig config, String table) throws SQLException {
        if (isBinaryColumn(stmt, table)) {
            return;
        }

        if (config instanceof MysqlConfig) {
            stmt.execute("ALTER TABLE " + table + " MODIFY COLUMN " + FIELD_INVENTORY_ITEM + " MEDIUMBLOB NOT NULL");
        } else if (config instanceof PostgreSqlConfig) {
            stmt.execute("ALTER TABLE " + table + " ALTER COLUMN " + FIELD_INVENTORY_ITEM
                    + " TYPE BYTEA USING convert_to(" + FIELD_INVENTORY_ITEM + ", 'UTF8')");
        }
    }

    private boolean isBinaryColumn(Statement stmt, String table) throws SQLException {
        try (var columns = stmt.getConnection().getMetaData().getColumns(null, null, table, FIELD_INVENTORY_ITEM)) {
            if (!columns.next()) {
                return false;
            }

            var type = columns.getInt("DATA_TYPE");
            return type == Types.BINARY || type == Types.VARBINARY || type == Types.LONGVARBINARY || type == Types.BLOB;
        }
    }

    private boolean tableExists(Statement stmt, String table) throws SQLException {
        try (var tables = stmt.getConnection().getMetaData().getTables(null, null, table, new String[] {"TABLE"})) {
            return tables.next();
        }
    }
}
