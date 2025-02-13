package com.xzavier0722.mc.plugin.slimefun4.storage.patch;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.ISqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabasePatchV2 extends DatabasePatch {

    public DatabasePatchV2() {
        super(2);
    }

    @Override
    public void patch(Statement stmt, ISqlCommonConfig config) throws SQLException {
        var tablePrefix = config instanceof SqlCommonConfig scc ? scc.tablePrefix() : "";
        stmt.execute("DROP TABLE IF EXISTS " + tablePrefix + SqlConstants.TABLE_NAME_TABLE_INFORMATION);
    }
}
