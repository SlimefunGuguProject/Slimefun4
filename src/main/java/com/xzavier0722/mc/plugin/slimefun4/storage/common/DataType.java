package com.xzavier0722.mc.plugin.slimefun4.storage.common;

/**
 * {@link DataType} 是 Slimefun 数据库控制器的类型，
 * 用于区分不同的数据存储类型。
 */
public enum DataType {
    /**
     * 玩家档案，通常包含研究进度、背包等其他玩家相关数据。
     */
    PLAYER_PROFILE,

    /**
     * Slimefun 方块数据
     */
    BLOCK_STORAGE
}
