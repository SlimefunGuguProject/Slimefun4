package com.xzavier0722.mc.plugin.slimefun4.storage.patch;

import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_INVENTORY_ITEM;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.mysql.MysqlConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.ISqlCommonConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabasePatchV1 extends DatabasePatch {
    public DatabasePatchV1() {
        super(1);
    }

    @Override
    public void patch(Statement stmt, ISqlCommonConfig config) throws SQLException {
        if (config instanceof MysqlConfig mysqlConf) {
            var uniInvTable = SqlUtils.mapTable(DataScope.UNIVERSAL_INVENTORY, mysqlConf.tablePrefix());
            stmt.execute("ALTER TABLE " + uniInvTable + " MODIFY COLUMN " + FIELD_INVENTORY_ITEM + " TEXT;");
        }
    }
}
