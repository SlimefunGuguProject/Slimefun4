package com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite;

import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_ID;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_NAME;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_NUM;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_SIZE;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_CHUNK;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_DATA_KEY;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_DATA_VALUE;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_INVENTORY_ITEM;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_INVENTORY_SLOT;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_LOCATION;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_PLAYER_NAME;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_PLAYER_UUID;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_RESEARCH_KEY;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_SLIMEFUN_ID;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_TABLE_METADATA_KEY;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_TABLE_METADATA_VALUE;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_UNIVERSAL_TRAITS;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_UNIVERSAL_UUID;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.METADATA_VERSION;

import city.norain.slimefun4.timings.entry.SQLEntry;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.IDataSourceAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlCommonAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataType;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

public class SqliteAdapter extends SqlCommonAdapter<SqliteConfig> {
    @Override
    public void initStorage(DataType type) {
        switch (type) {
            case PLAYER_PROFILE -> createProfileTables();
            case BLOCK_STORAGE -> createBlockStorageTables();
        }

        tableMetadataTable = SqlUtils.mapTable(DataScope.TABLE_METADATA);
        createTableMetadataTable();
    }

    @Override
    public void setData(RecordKey key, RecordSet item) {
        var data = item.getAll();
        var fields = data.keySet();
        var fieldStr = SqlUtils.buildFieldStr(fields);
        if (fieldStr.isEmpty()) {
            throw new IllegalArgumentException("No data provided in RecordSet.");
        }

        var valStr = new StringBuilder();
        var flag = false;
        for (var field : fields) {
            if (flag) {
                valStr.append(", ");
            } else {
                flag = true;
            }
            valStr.append(SqlUtils.toSqlValStr(field, data.get(field)));
        }

        var updateFields = key.getFields();
        var table = SqlUtils.mapTable(key.getScope());

        if (!updateFields.isEmpty()) {
            if (key.getConditions().isEmpty()) {
                throw new IllegalArgumentException("Condition is required for update statement!");
            }

            var row = executeUpdate("UPDATE "
                    + table
                    + " SET "
                    + String.join(
                            ", ",
                            updateFields.stream()
                                    .map(field -> {
                                        var val = item.get(field);
                                        if (val == null) {
                                            throw new IllegalArgumentException(
                                                    "Cannot find value in RecordSet for the specific key: " + field);
                                        }
                                        return SqlUtils.buildKvStr(field, val);
                                    })
                                    .toList())
                    + SqlUtils.buildConditionStr(key.getConditions())
                    + ";");
            if (row > 0) {
                return;
            }
        }

        executeSql("INSERT OR IGNORE INTO " + table + " (" + fieldStr.get() + ") VALUES (" + valStr + ");");
    }

    @Override
    public List<RecordSet> getData(RecordKey key, boolean distinct) {
        return executeQuery((distinct ? "SELECT DISTINCT " : "SELECT ")
                + SqlUtils.buildFieldStr(key.getFields()).orElse("*")
                + " FROM "
                + SqlUtils.mapTable(key.getScope())
                + SqlUtils.buildConditionStr(key.getConditions())
                + ";");
    }

    @Override
    public void deleteData(RecordKey key) {
        executeSql("DELETE FROM "
                + SqlUtils.mapTable(key.getScope())
                + SqlUtils.buildConditionStr(key.getConditions())
                + ";");
    }

    private void createProfileTables() {
        createProfileTable();
        createResearchTable();
        createBackpackTable();
        createBackpackInvTable();
    }

    private void createBlockStorageTables() {
        createBlockRecordTable();
        createBlockDataTable();
        createBlockInvTable();
        createChunkDataTable();
        createUniversalInventoryTable();
        createUniversalRecordTable();
        createUniversalDataTable();
    }

    private void createProfileTable() {
        var table = SqlUtils.mapTable(DataScope.PLAYER_PROFILE);
        executeSql("CREATE TABLE IF NOT EXISTS "
                + table
                + "("
                + FIELD_PLAYER_UUID
                + " TEXT PRIMARY KEY NOT NULL, "
                + FIELD_PLAYER_NAME
                + " TEXT NOT NULL, "
                + FIELD_BACKPACK_NUM
                + " INTEGER DEFAULT 0"
                + ");");

        executeSql("CREATE INDEX IF NOT EXISTS index_player_name ON " + table + " (" + FIELD_PLAYER_NAME + ");");
    }

    private void createResearchTable() {
        var table = SqlUtils.mapTable(DataScope.PLAYER_RESEARCH);
        executeSql("CREATE TABLE IF NOT EXISTS "
                + table
                + "("
                + FIELD_PLAYER_UUID
                + " TEXT NOT NULL, "
                + FIELD_RESEARCH_KEY
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_PLAYER_UUID
                + ") "
                + "REFERENCES "
                + SqlUtils.mapTable(DataScope.PLAYER_PROFILE)
                + "("
                + FIELD_PLAYER_UUID
                + ") "
                + "ON UPDATE CASCADE ON DELETE CASCADE);");

        executeSql("CREATE INDEX IF NOT EXISTS index_player_researches ON "
                + table
                + " ("
                + FIELD_PLAYER_UUID
                + ", "
                + FIELD_RESEARCH_KEY
                + ");");
    }

    private void createBackpackTable() {
        var table = SqlUtils.mapTable(DataScope.BACKPACK_PROFILE);
        executeSql("CREATE TABLE IF NOT EXISTS "
                + table
                + "("
                + FIELD_BACKPACK_ID
                + " TEXT PRIMARY KEY NOT NULL, "
                + FIELD_PLAYER_UUID
                + " TEXT NOT NULL, "
                + FIELD_BACKPACK_NUM
                + " INTEGER NOT NULL, "
                + FIELD_BACKPACK_NAME
                + " TEXT NULL, "
                + FIELD_BACKPACK_SIZE
                + " INTEGER NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_PLAYER_UUID
                + ") "
                + "REFERENCES "
                + SqlUtils.mapTable(DataScope.PLAYER_PROFILE)
                + "("
                + FIELD_PLAYER_UUID
                + ") "
                + "ON UPDATE CASCADE ON DELETE CASCADE);");

        executeSql("CREATE INDEX IF NOT EXISTS index_player_backpack ON "
                + table
                + " ("
                + FIELD_PLAYER_UUID
                + ", "
                + FIELD_BACKPACK_NUM
                + ");");
    }

    private void createBackpackInvTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + SqlUtils.mapTable(DataScope.BACKPACK_INVENTORY)
                + "("
                + FIELD_BACKPACK_ID
                + " TEXT NOT NULL, "
                + FIELD_INVENTORY_SLOT
                + " INTEGER NOT NULL, "
                + FIELD_INVENTORY_ITEM
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_BACKPACK_ID
                + ") "
                + "REFERENCES "
                + SqlUtils.mapTable(DataScope.BACKPACK_PROFILE)
                + "("
                + FIELD_BACKPACK_ID
                + ") "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "PRIMARY KEY ("
                + FIELD_BACKPACK_ID
                + ", "
                + FIELD_INVENTORY_SLOT
                + ")"
                + ");");
    }

    private void createBlockRecordTable() {
        var table = SqlUtils.mapTable(DataScope.BLOCK_RECORD);
        executeSql("CREATE TABLE IF NOT EXISTS "
                + table
                + "("
                + FIELD_LOCATION
                + " TEXT PRIMARY KEY NOT NULL, "
                + FIELD_CHUNK
                + " TEXT NOT NULL, "
                + FIELD_SLIMEFUN_ID
                + " TEXT NOT NULL"
                + ");");

        executeSql("CREATE INDEX IF NOT EXISTS index_chunk ON " + table + "(" + FIELD_CHUNK + ");");
    }

    private void createBlockDataTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + SqlUtils.mapTable(DataScope.BLOCK_DATA)
                + "("
                + FIELD_LOCATION
                + " TEXT NOT NULL, "
                + FIELD_DATA_KEY
                + " TEXT NOT NULL, "
                + FIELD_DATA_VALUE
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_LOCATION
                + ") "
                + "REFERENCES "
                + SqlUtils.mapTable(DataScope.BLOCK_RECORD)
                + "("
                + FIELD_LOCATION
                + ") "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "PRIMARY KEY ("
                + FIELD_LOCATION
                + ", "
                + FIELD_DATA_KEY
                + ")"
                + ");");
    }

    private void createChunkDataTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + SqlUtils.mapTable(DataScope.CHUNK_DATA)
                + "("
                + FIELD_CHUNK
                + " TEXT NOT NULL, "
                + FIELD_DATA_KEY
                + " TEXT NOT NULL, "
                + FIELD_DATA_VALUE
                + " TEXT NOT NULL, "
                + "PRIMARY KEY ("
                + FIELD_CHUNK
                + ", "
                + FIELD_DATA_KEY
                + ")"
                + ");");
    }

    private void createBlockInvTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + SqlUtils.mapTable(DataScope.BLOCK_INVENTORY)
                + "("
                + FIELD_LOCATION
                + " TEXT NOT NULL, "
                + FIELD_INVENTORY_SLOT
                + " INTEGER NOT NULL, "
                + FIELD_INVENTORY_ITEM
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_LOCATION
                + ") "
                + "REFERENCES "
                + SqlUtils.mapTable(DataScope.BLOCK_RECORD)
                + "("
                + FIELD_LOCATION
                + ") "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "PRIMARY KEY ("
                + FIELD_LOCATION
                + ", "
                + FIELD_INVENTORY_SLOT
                + ")"
                + ");");
    }

    private void createUniversalInventoryTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + SqlUtils.mapTable(DataScope.UNIVERSAL_INVENTORY)
                + "("
                + FIELD_UNIVERSAL_UUID
                + " CHAR(64) NOT NULL, "
                + FIELD_INVENTORY_SLOT
                + " TINYINT UNSIGNED NOT NULL, "
                + FIELD_INVENTORY_ITEM
                + " TEXT NOT NULL,"
                + "PRIMARY KEY ("
                + FIELD_UNIVERSAL_UUID
                + ", "
                + FIELD_INVENTORY_SLOT
                + ")"
                + ");");
    }

    private void createUniversalRecordTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + SqlUtils.mapTable(DataScope.UNIVERSAL_RECORD)
                + "("
                + FIELD_UNIVERSAL_UUID
                + " CHAR(64) NOT NULL, "
                + FIELD_SLIMEFUN_ID
                + " CHAR(64) NOT NULL, "
                + FIELD_UNIVERSAL_TRAITS
                + " CHAR(64) NOT NULL, "
                + "PRIMARY KEY ("
                + FIELD_UNIVERSAL_UUID
                + ")"
                + ");");
    }

    private void createUniversalDataTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + SqlUtils.mapTable(DataScope.UNIVERSAL_DATA)
                + "("
                + FIELD_UNIVERSAL_UUID
                + " CHAR(64) NOT NULL, "
                + FIELD_DATA_KEY
                + " CHAR(64) NOT NULL, "
                + FIELD_DATA_VALUE
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_UNIVERSAL_UUID
                + ") "
                + "REFERENCES "
                + SqlUtils.mapTable(DataScope.UNIVERSAL_RECORD)
                + "("
                + FIELD_UNIVERSAL_UUID
                + ") "
                + "ON UPDATE CASCADE ON DELETE CASCADE, "
                + "PRIMARY KEY ("
                + FIELD_UNIVERSAL_UUID
                + ", "
                + FIELD_DATA_KEY
                + ")"
                + ");");
    }

    private void createTableMetadataTable() {
        executeSql(MessageFormat.format(
                """
                CREATE TABLE IF NOT EXISTS {0}
                (
                    {1} VARCHAR(255) UNIQUE NOT NULL,
                    {2} TEXT NOT NULL
                );
                """,
                SqlUtils.mapTable(DataScope.TABLE_METADATA), FIELD_TABLE_METADATA_KEY, FIELD_TABLE_METADATA_VALUE));

        if (Slimefun.isNewlyInstalled()) {
            executeSql(MessageFormat.format(
                    """
                    INSERT OR IGNORE INTO {0} ({1}, {2}) VALUES ("{3}", {4});
                    """,
                    SqlUtils.mapTable(DataScope.TABLE_METADATA),
                    FIELD_TABLE_METADATA_KEY,
                    FIELD_TABLE_METADATA_VALUE,
                    METADATA_VERSION,
                    IDataSourceAdapter.DATABASE_VERSION));
        }
    }

    public synchronized void executeSql(String sql) {
        super.executeSql(sql);
    }

    private synchronized int executeUpdate(String sql) {
        var entry = new SQLEntry(sql);
        Slimefun.getSQLProfiler().recordEntry(entry);

        try (var conn = ds.getConnection()) {
            return SqlUtils.execUpdate(conn, sql);
        } catch (SQLException e) {
            throw new IllegalStateException("An exception thrown while executing sql: " + sql, e);
        } finally {
            Slimefun.getSQLProfiler().finishEntry(entry);
        }
    }
}
