package com.xzavier0722.mc.plugin.slimefun4.storage.callback;

import io.github.thebusybiscuit.slimefun4.utils.ThreadUtils;
import java.util.function.Consumer;

/**
 * This interface is deprecated and related methods will be replaced by methods using {@link java.util.concurrent.CompletableFuture} in the future.
 * Related methods will be kept and mark deprecated.
 * You can run your original {@link IAsyncReadCallback} callbacks using {@link java.util.concurrent.CompletableFuture#thenAcceptAsync(Consumer, java.util.concurrent.Executor)}, don't forget to check whether the accepted value is null or not.
 * If you want to invoke callback not on the main thread, use the callback executor from the data controller.
 * If you want to invoke callback on the main thread, see {@link ThreadUtils#getMainThreadExecutor()} or {@link ThreadUtils#getMainDelayedExecutor()}.
 *
 * @param <T> the type of result returned by the callback
 */
public interface IAsyncReadCallback<T> {
    default boolean runOnMainThread() {
        return false;
    }

    default void onResult(T result) {}

    default void onResultNotFound() {}
}
