package com.xzavier0722.mc.plugin.slimefun4.storage.common;

public enum DataScope {
    NONE,
    PLAYER_RESEARCH,
    PLAYER_PROFILE(new FieldKey[] {FieldKey.PLAYER_UUID}),
    BACKPACK_PROFILE(new FieldKey[] {FieldKey.BACKPACK_ID}),
    BACKPACK_INVENTORY(new FieldKey[] {FieldKey.BACKPACK_ID, FieldKey.INVENTORY_SLOT}),
    BLOCK_RECORD(new FieldKey[] {FieldKey.LOCATION}),
    BLOCK_DATA(new FieldKey[] {FieldKey.LOCATION, FieldKey.DATA_KEY}),
    CHUNK_DATA(new FieldKey[] {FieldKey.CHUNK, FieldKey.DATA_KEY}),
    BLOCK_INVENTORY(new FieldKey[] {FieldKey.LOCATION, FieldKey.INVENTORY_SLOT}),
    UNIVERSAL_RECORD(new FieldKey[] {FieldKey.UNIVERSAL_UUID}),
    UNIVERSAL_DATA(new FieldKey[] {FieldKey.UNIVERSAL_UUID, FieldKey.DATA_KEY}),
    UNIVERSAL_INVENTORY(new FieldKey[] {FieldKey.UNIVERSAL_UUID, FieldKey.INVENTORY_SLOT}),
    TABLE_METADATA;

    private final FieldKey[] primaryKeys;

    DataScope() {
        primaryKeys = new FieldKey[0];
    }

    DataScope(FieldKey[] primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public FieldKey[] getPrimaryKeys() {
        return primaryKeys;
    }
}
