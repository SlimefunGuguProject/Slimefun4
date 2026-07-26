package io.github.thebusybiscuit.slimefun4.core.services.scheduling;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;
import javax.annotation.Nonnull;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Scheduler abstraction used by Slimefun core services.
 *
 * <p>Location-bound methods execute on the thread that owns the target region, while global methods execute on the
 * server's global scheduler. Async methods must never access Bukkit world state.
 */
@SlimefunAPI
public interface SlimefunScheduler {

    @Nonnull
    TaskHandle run(@Nonnull Runnable task);

    @Nonnull
    TaskHandle runLater(@Nonnull Runnable task, long delayTicks);

    @Nonnull
    TaskHandle runAt(@Nonnull Location location, @Nonnull Runnable task);

    @Nonnull
    TaskHandle runAtLater(@Nonnull Location location, @Nonnull Runnable task, long delayTicks);

    @Nonnull
    TaskHandle runAtFixedRate(@Nonnull Runnable task, long initialDelayTicks, long periodTicks);

    @Nonnull
    TaskHandle runAtFixedRate(
            @Nonnull Location location, @Nonnull Runnable task, long initialDelayTicks, long periodTicks);

    @Nonnull
    TaskHandle runFor(@Nonnull Entity entity, @Nonnull Runnable task);

    @Nonnull
    TaskHandle runForLater(@Nonnull Entity entity, @Nonnull Runnable task, long delayTicks);

    @Nonnull
    TaskHandle runForAtFixedRate(
            @Nonnull Entity entity, @Nonnull Runnable task, long initialDelayTicks, long periodTicks);

    @Nonnull
    TaskHandle runAsync(@Nonnull Runnable task);

    @Nonnull
    TaskHandle runAsyncLater(@Nonnull Runnable task, long delayTicks);

    @Nonnull
    TaskHandle runAsyncAtFixedRate(@Nonnull Runnable task, long initialDelayTicks, long periodTicks);

    boolean isOwnedByCurrentRegion(@Nonnull Location location);

    void cancelAll();
}
