package com.xzavier0722.mc.plugin.slimefun4.storage.common;

public enum FieldKey {
    PLAYER_UUID,
    PLAYER_NAME,
    PLAYER_BACKPACK_NUM(true),

    RESEARCH_ID,

    BACKPACK_ID,
    BACKPACK_NUMBER(true),
    BACKPACK_NAME,
    BACKPACK_SIZE(true),

    INVENTORY_SLOT,
    INVENTORY_ITEM,

    LOCATION,
    CHUNK,
    SLIMEFUN_ID,

    DATA_KEY,
    DATA_VALUE,

    /**
     * Represents uuid of universal inventory
     */
    UNIVERSAL_UUID,

    UNIVERSAL_TRAITS,

    METADATA_KEY,
    METADATA_VALUE;

    private final boolean isNumType;

    FieldKey() {
        this(false);
    }

    FieldKey(boolean isNumType) {
        this.isNumType = isNumType;
    }

    public boolean isNumType() {
        return isNumType;
    }
}
