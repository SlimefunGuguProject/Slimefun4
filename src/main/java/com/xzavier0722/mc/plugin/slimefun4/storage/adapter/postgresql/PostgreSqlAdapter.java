package com.xzavier0722.mc.plugin.slimefun4.storage.adapter.postgresql;

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

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.IDataSourceAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlCommonAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataType;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PostgreSqlAdapter extends SqlCommonAdapter<PostgreSqlConfig> {
    @Override
    public void initStorage(DataType type) {
        switch (type) {
            case PLAYER_PROFILE -> {
                profileTable = SqlUtils.mapTable(DataScope.PLAYER_PROFILE, config.tablePrefix());
                researchTable = SqlUtils.mapTable(DataScope.PLAYER_RESEARCH, config.tablePrefix());
                backpackTable = SqlUtils.mapTable(DataScope.BACKPACK_PROFILE, config.tablePrefix());
                bpInvTable = SqlUtils.mapTable(DataScope.BACKPACK_INVENTORY, config.tablePrefix());
                createProfileTables();
            }
            case BLOCK_STORAGE -> {
                blockRecordTable = SqlUtils.mapTable(DataScope.BLOCK_RECORD, config.tablePrefix());
                blockDataTable = SqlUtils.mapTable(DataScope.BLOCK_DATA, config.tablePrefix());
                blockInvTable = SqlUtils.mapTable(DataScope.BLOCK_INVENTORY, config.tablePrefix());
                chunkDataTable = SqlUtils.mapTable(DataScope.CHUNK_DATA, config.tablePrefix());
                universalInvTable = SqlUtils.mapTable(DataScope.UNIVERSAL_INVENTORY, config.tablePrefix());
                universalDataTable = SqlUtils.mapTable(DataScope.UNIVERSAL_DATA, config.tablePrefix());
                universalRecordTable = SqlUtils.mapTable(DataScope.UNIVERSAL_RECORD, config.tablePrefix());
                createBlockStorageTables();
            }
        }

        tableMetadataTable = SqlUtils.mapTable(DataScope.TABLE_METADATA, config.tablePrefix());
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

        if (!updateFields.isEmpty() && key.getConditions().isEmpty()) {
            throw new IllegalArgumentException("Condition is required for update statement!");
        }

        var contrastStr = Arrays.stream(key.getScope().getPrimaryKeys())
                .map(SqlUtils::mapField)
                .collect(Collectors.joining(", "));

        executeSql("INSERT INTO "
                + mapTable(key.getScope())
                + " ("
                + fieldStr.get()
                + ") "
                + "VALUES ("
                + valStr
                + ")"
                + (contrastStr.isEmpty()
                        ? ""
                        : " ON CONFLICT ("
                                + contrastStr
                                + ") "
                                + (updateFields.isEmpty()
                                        ? "DO NOTHING"
                                        : "DO UPDATE SET "
                                                + String.join(
                                                        ", ",
                                                        updateFields.stream()
                                                                .map(field -> {
                                                                    var val = item.get(field);
                                                                    if (val == null) {
                                                                        throw new IllegalArgumentException(
                                                                                "Cannot find value in RecordSet for the specific"
                                                                                        + " key: "
                                                                                        + field);
                                                                    }
                                                                    return SqlUtils.buildKvStr(field, val);
                                                                })
                                                                .toList())))
                + ";");
    }

    @Override
    public List<RecordSet> getData(RecordKey key, boolean distinct) {
        return executeQuery((distinct ? "SELECT DISTINCT " : "SELECT ")
                + SqlUtils.buildFieldStr(key.getFields()).orElse("*")
                + " FROM "
                + mapTable(key.getScope())
                + SqlUtils.buildConditionStr(key.getConditions())
                + ";");
    }

    @Override
    public void deleteData(RecordKey key) {
        executeSql("DELETE FROM " + mapTable(key.getScope()) + SqlUtils.buildConditionStr(key.getConditions()) + ";");
    }

    private void createProfileTables() {
        createProfileTable();
        createResearchTable();
        createBackpackTable();
        createBackpackInventoryTable();
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
        executeSql("CREATE TABLE IF NOT EXISTS "
                + profileTable
                + "("
                + FIELD_PLAYER_UUID
                + " VARCHAR(64) PRIMARY KEY NOT NULL, "
                + FIELD_PLAYER_NAME
                + " VARCHAR(64) NOT NULL, "
                + FIELD_BACKPACK_NUM
                + " INT DEFAULT 0"
                + ");");

        executeSql("CREATE INDEX IF NOT EXISTS index_player_name ON " + profileTable + " (" + FIELD_PLAYER_NAME + ");");
    }

    private void createResearchTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + researchTable
                + "("
                + FIELD_PLAYER_UUID
                + " VARCHAR(64) NOT NULL, "
                + FIELD_RESEARCH_KEY
                + " VARCHAR(64) NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_PLAYER_UUID
                + ") "
                + "REFERENCES "
                + profileTable
                + "("
                + FIELD_PLAYER_UUID
                + ") "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ");");

        executeSql("CREATE INDEX IF NOT EXISTS index_player_research ON "
                + researchTable
                + " ("
                + FIELD_PLAYER_UUID
                + ", "
                + FIELD_RESEARCH_KEY
                + ");");
    }

    private void createBackpackTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + backpackTable
                + "("
                + FIELD_BACKPACK_ID
                + " VARCHAR(64) PRIMARY KEY NOT NULL, "
                + FIELD_PLAYER_UUID
                + " VARCHAR(64) NOT NULL, "
                + FIELD_BACKPACK_NUM
                + " INT NOT NULL, "
                + FIELD_BACKPACK_NAME
                + " VARCHAR(64) NULL, "
                + FIELD_BACKPACK_SIZE
                + " SMALLINT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_PLAYER_UUID
                + ") "
                + "REFERENCES "
                + profileTable
                + "("
                + FIELD_PLAYER_UUID
                + ") "
                + "ON UPDATE CASCADE ON DELETE CASCADE"
                + ");");

        executeSql("CREATE INDEX IF NOT EXISTS index_player_backpack ON "
                + backpackTable
                + " ("
                + FIELD_PLAYER_UUID
                + ", "
                + FIELD_BACKPACK_NUM
                + ");");
    }

    private void createBackpackInventoryTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + bpInvTable
                + "("
                + FIELD_BACKPACK_ID
                + " VARCHAR(64) NOT NULL, "
                + FIELD_INVENTORY_SLOT
                + " SMALLINT NOT NULL, "
                + FIELD_INVENTORY_ITEM
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_BACKPACK_ID
                + ") "
                + "REFERENCES "
                + backpackTable
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
        executeSql("CREATE TABLE IF NOT EXISTS "
                + blockRecordTable
                + "("
                + FIELD_LOCATION
                + " VARCHAR(64) PRIMARY KEY NOT NULL, "
                + FIELD_CHUNK
                + " VARCHAR(64) NOT NULL, "
                + FIELD_SLIMEFUN_ID
                + " VARCHAR(64) NOT NULL"
                + ");");

        executeSql("CREATE INDEX IF NOT EXISTS index_ticking ON " + blockRecordTable + " (" + FIELD_CHUNK + ");");
    }

    private void createBlockDataTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + blockDataTable
                + "("
                + FIELD_LOCATION
                + " VARCHAR(64) NOT NULL, "
                + FIELD_DATA_KEY
                + " VARCHAR(64) NOT NULL, "
                + FIELD_DATA_VALUE
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_LOCATION
                + ") "
                + "REFERENCES "
                + blockRecordTable
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
                + chunkDataTable
                + "("
                + FIELD_CHUNK
                + " VARCHAR(64) NOT NULL, "
                + FIELD_DATA_KEY
                + " VARCHAR(64) NOT NULL, "
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
                + blockInvTable
                + "("
                + FIELD_LOCATION
                + " VARCHAR(64) NOT NULL, "
                + FIELD_INVENTORY_SLOT
                + " SMALLINT NOT NULL, "
                + FIELD_INVENTORY_ITEM
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_LOCATION
                + ") "
                + "REFERENCES "
                + blockRecordTable
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
                + universalInvTable
                + "("
                + FIELD_UNIVERSAL_UUID
                + " UUID NOT NULL, "
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
                + universalRecordTable
                + "("
                + FIELD_UNIVERSAL_UUID
                + " UUID NOT NULL, "
                + FIELD_SLIMEFUN_ID
                + " TEXT NOT NULL, "
                + FIELD_UNIVERSAL_TRAITS
                + " TEXT NOT NULL, "
                + "PRIMARY KEY ("
                + FIELD_UNIVERSAL_UUID
                + ")"
                + ");");
    }

    private void createUniversalDataTable() {
        executeSql("CREATE TABLE IF NOT EXISTS "
                + universalDataTable
                + "("
                + FIELD_UNIVERSAL_UUID
                + " UUID NOT NULL, "
                + FIELD_DATA_KEY
                + " VARCHAR(64) NOT NULL, "
                + FIELD_DATA_VALUE
                + " TEXT NOT NULL, "
                + "FOREIGN KEY ("
                + FIELD_UNIVERSAL_UUID
                + ") "
                + "REFERENCES "
                + universalRecordTable
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
                    {1} VARCHAR(100) UNIQUE NOT NULL,
                    {2} TEXT NOT NULL
                );
                """,
                tableMetadataTable, FIELD_TABLE_METADATA_KEY, FIELD_TABLE_METADATA_VALUE));

        if (Slimefun.isNewlyInstalled()) {
            executeSql(MessageFormat.format(
                    """
                    INSERT INTO {0} ({1}, {2}) VALUES ("{3}", {4}) ON CONFLICT ({1}) DO NOTHING;
                    """,
                    tableMetadataTable,
                    FIELD_TABLE_METADATA_KEY,
                    FIELD_TABLE_METADATA_VALUE,
                    METADATA_VERSION,
                    IDataSourceAdapter.DATABASE_VERSION));
        }
    }
}
