package com.xzavier0722.mc.plugin.slimefun4.storage.callback;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ADataController;
import io.github.thebusybiscuit.slimefun4.utils.ThreadUtils;
import java.util.function.Consumer;

/**
 * This interface is deprecated and related methods will be replaced by methods using {@link java.util.concurrent.CompletableFuture} in the future, <- this work is a completable future
 * related methods will be kept and mark deprecated
 * you can run your original {@link IAsyncReadCallback} callbacks using {@link java.util.concurrent.CompletableFuture#thenAcceptAsync(Consumer, java.util.concurrent.Executor)}, don't forget to check whether the accepted value is null or not
 * If you want to invoke callback not on the main thread, use
 * {@link ADataController#getCallbackExecutor}
 * If you want to invoke callback on the main thread , see
 * {@link ThreadUtils#getMainThreadExecutor()} or {@link ThreadUtils#getMainDelayedExecutor()}
 * @param <T>
 */
public interface IAsyncReadCallback<T> {
    default boolean runOnMainThread() {
        return false;
    }

    default void onResult(T result) {}

    default void onResultNotFound() {}
}
