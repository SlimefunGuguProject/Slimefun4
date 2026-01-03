package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import lombok.Getter;
import lombok.Setter;

/**
 * Slimefun 数据容器的抽象类.
 * <p>
 * 该类用于存储 Slimefun 特有的数据容器, 包括 Slimefun ID 和是否待删除的标志.
 *
 * @author NoRainCity
 *
 * @see ADataController
 */
public abstract class ASlimefunDataContainer extends ADataContainer {
    @Getter
    private final String sfId;

    @Setter
    @Getter
    private volatile boolean pendingRemove = false;

    public ASlimefunDataContainer(String key, String sfId) {
        super(key);
        this.sfId = sfId;
    }

    public ASlimefunDataContainer(String key, ADataContainer other, String sfId) {
        super(key, other);
        this.sfId = sfId;
    }

    @Override
    protected void checkData() {
        super.checkData();

        if (pendingRemove) {
            throw new IllegalStateException("Unable to access data that is pending removal!");
        }
    }
}
