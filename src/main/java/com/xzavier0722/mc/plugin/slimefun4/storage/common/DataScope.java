package com.xzavier0722.mc.plugin.slimefun4.storage.common;

import lombok.Getter;

/**
 * {@link DataScope} 是 Slimefun 数据库表中数据的作用域定义
 */
public enum DataScope {
    NONE,
    PLAYER_RESEARCH,
    PLAYER_PROFILE(new FieldKey[] {FieldKey.PLAYER_UUID}),
    BACKPACK_PROFILE(new FieldKey[] {FieldKey.BACKPACK_ID}),
    BACKPACK_INVENTORY(new FieldKey[] {FieldKey.BACKPACK_ID, FieldKey.INVENTORY_SLOT}),
    BLOCK_RECORD(new FieldKey[] {FieldKey.LOCATION}, true),
    BLOCK_DATA(new FieldKey[] {FieldKey.LOCATION, FieldKey.DATA_KEY}),
    CHUNK_DATA(new FieldKey[] {FieldKey.CHUNK, FieldKey.DATA_KEY}),
    BLOCK_INVENTORY(new FieldKey[] {FieldKey.LOCATION, FieldKey.INVENTORY_SLOT}),
    UNIVERSAL_RECORD(new FieldKey[] {FieldKey.UNIVERSAL_UUID}),
    UNIVERSAL_DATA(new FieldKey[] {FieldKey.UNIVERSAL_UUID, FieldKey.DATA_KEY}),
    UNIVERSAL_INVENTORY(new FieldKey[] {FieldKey.UNIVERSAL_UUID, FieldKey.INVENTORY_SLOT}),
    TABLE_METADATA;

    /**
     * 标记当前 {@link DataScope} 的主键字段
     */
    @Getter
    private final FieldKey[] primaryKeys;

    @Getter
    private final boolean serial;

    DataScope() {
        primaryKeys = new FieldKey[0];
        this.serial = false;
    }

    DataScope(FieldKey[] primaryKeys) {
        this.primaryKeys = primaryKeys;
        this.serial = false;
    }

    DataScope(FieldKey[] primaryKeys, boolean serial) {
        this.primaryKeys = primaryKeys;
        this.serial = serial;
    }
}
