package com.xzavier0722.mc.plugin.slimefun4.storage.adapter;

import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataType;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import java.util.List;

public interface IDataSourceAdapter<T> {
    /** Current database schema version. Increment this when persistent storage changes. */
    int DATABASE_VERSION = 3;

    void prepare(T config);

    void initStorage(DataType type);

    void shutdown();

    void setData(RecordKey key, RecordSet item);

    default List<RecordSet> getData(RecordKey key) {
        return getData(key, false);
    }

    List<RecordSet> getData(RecordKey key, boolean distinct);

    void deleteData(RecordKey key);

    void patch();
}
