package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

public abstract class ADataContainer {
    private final String key;
    private final Map<String, String> data;
    private volatile boolean isDataLoaded = false;

    @ParametersAreNonnullByDefault
    public ADataContainer(String key) {
        this.key = key;
        data = new ConcurrentHashMap<>();
    }

    @ParametersAreNonnullByDefault
    public ADataContainer(String key, ADataContainer other) {
        this.key = key;
        this.data = other.data;
        this.isDataLoaded = other.isDataLoaded;
    }

    public boolean isDataLoaded() {
        return isDataLoaded;
    }

    protected String getCacheInternal(String key) {
        return data.get(key);
    }

    protected void setIsDataLoaded(boolean isDataLoaded) {
        this.isDataLoaded = isDataLoaded;
    }

    protected void setCacheInternal(String key, String val, boolean override) {
        if (override) {
            data.put(key, val);
        } else {
            data.putIfAbsent(key, val);
        }
    }

    protected String removeCacheInternal(String key) {
        return data.remove(key);
    }

    protected void checkData() {
        if (!isDataLoaded) {
            throw new IllegalStateException("Unable to access unloaded data!");
        }
    }

    @Nonnull
    public Map<String, String> getAllData() {
        checkData();
        return Collections.unmodifiableMap(data);
    }

    @Nonnull
    public Set<String> getDataKeys() {
        checkData();
        return Collections.unmodifiableSet(data.keySet());
    }

    @Nullable public String getData(String key) {
        checkData();
        return getCacheInternal(key);
    }

    @Nonnull
    public String getKey() {
        return key;
    }

    public abstract void setData(String key, String val);
}
