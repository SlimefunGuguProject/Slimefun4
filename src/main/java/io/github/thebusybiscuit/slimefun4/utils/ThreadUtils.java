package io.github.thebusybiscuit.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.core.services.scheduling.FoliaSupport;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import javax.annotation.Nonnull;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class ThreadUtils {
    private static final Executor MAIN_THREAD_EXECUTOR;
    private static final Executor MAIN_DELAYED_EXECUTOR;

    public static Executor getMainThreadExecutor() {
        return MAIN_THREAD_EXECUTOR;
    }

    public static Executor getMainDelayedExecutor() {
        return MAIN_DELAYED_EXECUTOR;
    }

    /**
     * Returns an executor that follows the supplied entity across Folia region changes.
     *
     * @param entity the entity that owns the callback
     * @return an entity-owned executor
     */
    public static Executor getEntityThreadExecutor(@Nonnull Entity entity) {
        Validate.notNull(entity, "Entity cannot be null");
        return task -> {
            if (Slimefun.getSchedulerService().isOwnedByCurrentRegion(entity)) {
                task.run();
            } else {
                Slimefun.getSchedulerService().runFor(entity, task);
            }
        };
    }

    /**
     * Returns a one-tick-delayed executor that follows the supplied entity across Folia region changes.
     *
     * @param entity the entity that owns the callback
     * @return a delayed entity-owned executor
     */
    public static Executor getEntityDelayedExecutor(@Nonnull Entity entity) {
        Validate.notNull(entity, "Entity cannot be null");
        return task -> Slimefun.getSchedulerService().runForLater(entity, task, 1L);
    }

    /**
     * Returns an executor owned by the region containing the supplied location.
     *
     * @param location the location whose region owns the callback
     * @return a location-owned executor
     */
    public static Executor getLocationThreadExecutor(@Nonnull Location location) {
        Validate.notNull(location, "Location cannot be null");
        Location anchor = location.clone();
        return task -> {
            if (Slimefun.getSchedulerService().isOwnedByCurrentRegion(anchor)) {
                task.run();
            } else {
                Slimefun.getSchedulerService().runAt(anchor, task);
            }
        };
    }

    /**
     * Executes a task synchronously on Paper's primary thread or Folia's global region.
     * Location- or entity-bound Bukkit work must use the corresponding owned executor instead.
     *
     * @param runnable the task to execute
     */
    public static void executeSync(Runnable runnable) {
        if (!FoliaSupport.isFolia() && Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            runSyncNMS(runnable);
        }
    }

    /**
     * Always schedules a task onto Paper's primary thread or Folia's global region.
     *
     * @param runnable the task to execute
     */
    public static void executeSyncSched(Runnable runnable) {
        CompletableFuture.runAsync(runnable, MAIN_THREAD_EXECUTOR);
    }

    public static <T> FutureTask<T> getFutureTask(Callable<T> callable) {
        return new FutureTask<>(callable);
    }

    @SuppressWarnings("unchecked")
    public static FutureTask<Void> getFutureTask(Runnable runnable) {
        return runnable instanceof FutureTask<?> future
                ? (FutureTask<Void>) future
                : new FutureTask<>(runnable, (Void) null);
    }

    public static <T> FutureTask<T> getFutureTask(Runnable runnable, T val) {
        return new FutureTask<>(runnable, val);
    }

    private static void runSyncNMS(Runnable runnable) {
        MAIN_THREAD_EXECUTOR.execute(runnable);
    }

    static {
        Executor executor;
        if (FoliaSupport.isFolia()) {
            // Folia has no universal main thread. Generic legacy callbacks are placed on the global region.
            executor = task -> Slimefun.getSchedulerService().run(task);
        } else {
            try {
                Class<?> mcUtils = Class.forName("io.papermc.paper.util.MCUtil");
                Field field = mcUtils.getDeclaredField("MAIN_EXECUTOR");
                field.setAccessible(true);
                executor = (Executor) field.get(null);
            } catch (Throwable ignored) {
                executor = task -> {
                    if (Bukkit.isPrimaryThread()) {
                        task.run();
                    } else {
                        Slimefun.runSync(task);
                    }
                };
            }
        }
        MAIN_THREAD_EXECUTOR = executor;
        MAIN_DELAYED_EXECUTOR = task -> Slimefun.getSchedulerService().runLater(task, 1L);
    }
}
