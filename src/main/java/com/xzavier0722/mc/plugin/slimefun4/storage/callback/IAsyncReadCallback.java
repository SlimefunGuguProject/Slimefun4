package com.xzavier0722.mc.plugin.slimefun4.storage.callback;

import io.github.bakedlibs.dough.collections.Pair;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public interface IAsyncReadCallback<T> {
    default Pair<Boolean, Pair<Entity, Location>> runOnMainThread() {
        return new Pair<>(false, null);
    }

    default void onResult(T result) {}

    default void onResultNotFound() {}
}
