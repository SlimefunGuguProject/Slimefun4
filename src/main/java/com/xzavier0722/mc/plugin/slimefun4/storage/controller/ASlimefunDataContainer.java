package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import lombok.Getter;
import lombok.Setter;

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
}
