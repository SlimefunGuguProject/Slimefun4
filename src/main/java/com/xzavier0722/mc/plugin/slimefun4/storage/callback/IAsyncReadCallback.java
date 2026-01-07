package com.xzavier0722.mc.plugin.slimefun4.storage.callback;

@Deprecated
public interface IAsyncReadCallback<T> {
    default boolean runOnMainThread() {
        return false;
    }

    default void onResult(T result) {}

    default void onResultNotFound() {}
}
