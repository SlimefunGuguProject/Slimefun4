package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Abstract base class for data containers that store key-value data.
 */
public abstract class ADataContainer {
    private final String key;
    private final Map<String, String> data;
    private volatile boolean isDataLoaded = false;

    /**
     * Constructs a new ADataContainer.
     *
     * @param key The key for this container
     */
    @ParametersAreNonnullByDefault
    public ADataContainer(String key) {
        this.key = key;
        data = new ConcurrentHashMap<>();
    }

    /**
     * Constructs a new ADataContainer by copying data from another container.
     *
     * @param key   The key for this container
     * @param other The other container to copy data from
     */
    @ParametersAreNonnullByDefault
    public ADataContainer(String key, ADataContainer other) {
        this.key = key;
        this.data = other.data;
        this.isDataLoaded = other.isDataLoaded;
    }

    /**
     * Checks if the data has been loaded.
     *
     * @return true if data is loaded, false otherwise
     */
    public boolean isDataLoaded() {
        return isDataLoaded;
    }

    /**
     * Gets the cached value for the given key.
     *
     * @param key The key to look up
     * @return The cached value, or null if not found
     */
    protected String getCacheInternal(String key) {
        return data.get(key);
    }

    /**
     * Sets whether the data is loaded.
     *
     * @param isDataLoaded Whether data is loaded
     */
    protected void setIsDataLoaded(boolean isDataLoaded) {
        this.isDataLoaded = isDataLoaded;
    }

    /**
     * Sets a value in the cache.
     *
     * @param key      The key to set
     * @param val      The value to set
     * @param override Whether to override existing values
     */
    protected void setCacheInternal(String key, String val, boolean override) {
        if (override) {
            data.put(key, val);
        } else {
            data.putIfAbsent(key, val);
        }
    }

    /**
     * Removes a value from the cache.
     *
     * @param key The key to remove
     * @return The removed value, or null if not found
     */
    protected String removeCacheInternal(String key) {
        return data.remove(key);
    }

    /**
     * Checks if data is loaded and throws an exception if not.
     */
    protected void checkData() {
        if (!isDataLoaded) {
            throw new IllegalStateException("Unable to access unloaded data!");
        }
    }

    /**
     * Gets all data in this container.
     *
     * @return An unmodifiable map of all data
     */
    @Nonnull
    public Map<String, String> getAllData() {
        checkData();
        return Collections.unmodifiableMap(data);
    }

    /**
     * Gets all keys in this container.
     *
     * @return An unmodifiable set of all keys
     */
    @Nonnull
    public Set<String> getDataKeys() {
        checkData();
        return Collections.unmodifiableSet(data.keySet());
    }

    /**
     * Gets the value for the given key.
     *
     * @param key The key to look up
     * @return The value, or null if not found
     */
    @Nullable public String getData(String key) {
        checkData();
        return getCacheInternal(key);
    }

    /**
     * Gets the key of this container.
     *
     * @return The key
     */
    @Nonnull
    public String getKey() {
        return key;
    }

    /**
     * Sets data in the container.
     *
     * @param key The key to set
     * @param val The value to set
     */
    public abstract void setData(String key, String val);

    /**
     * Removes data from the container.
     *
     * @param key The key to remove
     */
    public abstract void removeData(String key);
}
