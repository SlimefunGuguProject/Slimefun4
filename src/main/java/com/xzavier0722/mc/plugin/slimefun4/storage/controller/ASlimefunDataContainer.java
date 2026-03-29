package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Getter;

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

    @Getter
    private volatile boolean pendingRemove = false;

    // use weak hash map to capture the pending remove datas, if it is removed finally, it will not cause memory leak
    private static final WeakHashMap<ASlimefunDataContainer, Map<String, String>> capturedPendingRemoveData =
            new WeakHashMap<>();

    protected void setWhilePendingRemove(String key, String value) {
        if (pendingRemove) {
            Map<String, String> map;
            synchronized (capturedPendingRemoveData) {
                map = capturedPendingRemoveData.computeIfAbsent(this, (k) -> new ConcurrentHashMap<>());
            }
            map.put(key, value);
        }
    }

    public void setPendingRemove(boolean val) {
        if (val) {
            pendingRemove = true;
        } else {
            // save keys during the remove
            pendingRemove = false;
            Map<String, String> map;
            synchronized (capturedPendingRemoveData) {
                map = capturedPendingRemoveData.remove(this);
            }
            if (map != null) {
                for (var entry : map.entrySet()) {
                    setData(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @ParametersAreNonnullByDefault
    public void setData(String key, String val) {
        checkData();
        setCacheInternal(key, val, true);
        if (isPendingRemove()) {
            // someone is modifying a removed blockData or a virtual blockData, DO NOT SAVE
            // save the key-value for other later use
            setWhilePendingRemove(key, val);
            return;
        } else {
            updateDataBaseKey(key);
        }
    }

    @ParametersAreNonnullByDefault
    public void removeData(String key) {
        if (removeCacheInternal(key) != null || !isDataLoaded()) {
            if (isPendingRemove()) {
                setWhilePendingRemove(key, null);
            } else {
                updateDataBaseKey(key);
            }
        }
    }

    @ParametersAreNonnullByDefault
    public abstract void updateDataBaseKey(String key);

    public ASlimefunDataContainer(String key, String sfId) {
        super(key);
        this.sfId = sfId;
    }

    public ASlimefunDataContainer(String key, ADataContainer other, String sfId) {
        super(key, other);
        this.sfId = sfId;
    }
}
