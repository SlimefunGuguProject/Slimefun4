package com.xzavier0722.mc.plugin.slimefun4.storage.patch;

import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_TABLE_METADATA_KEY;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_TABLE_METADATA_VALUE;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.METADATA_VERSION;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.ISqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite.SqliteConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import java.sql.SQLException;
import java.sql.Statement;
import lombok.Getter;

@Getter
public abstract class DatabasePatch {
    protected int version;

    public DatabasePatch(int version) {
        this.version = version;
    }

    public void updateVersion(Statement stmt, ISqlCommonConfig config) throws SQLException {
        var table = SqlUtils.mapTable(
                DataScope.TABLE_METADATA, config instanceof SqlCommonConfig scc ? scc.tablePrefix() : "");

        if (config instanceof SqliteConfig) {
            stmt.execute(String.format(
                    "INSERT INTO %s (%s, %s) VALUES ('%s', '%s') ON CONFLICT(%s) DO UPDATE SET %s='%s'",
                    table,
                    FIELD_TABLE_METADATA_KEY,
                    FIELD_TABLE_METADATA_VALUE,
                    METADATA_VERSION,
                    getVersion(),
                    FIELD_TABLE_METADATA_KEY,
                    FIELD_TABLE_METADATA_VALUE,
                    getVersion()));
        } else {
            stmt.execute(String.format(
                    "INSERT INTO %s (%s, %s) VALUES ('%s', '%s') ON DUPLICATE KEY UPDATE %s='%s', %s=%s",
                    table,
                    FIELD_TABLE_METADATA_KEY,
                    FIELD_TABLE_METADATA_VALUE,
                    METADATA_VERSION,
                    getVersion(),
                    FIELD_TABLE_METADATA_KEY,
                    METADATA_VERSION,
                    FIELD_TABLE_METADATA_VALUE,
                    getVersion()));
        }
    }

    public abstract void patch(Statement stmt, ISqlCommonConfig config) throws SQLException;
}
