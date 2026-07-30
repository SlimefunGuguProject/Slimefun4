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

    /**
     * Runs entity-owned work and invokes {@code retired} if the entity becomes retired before execution.
     *
     * <p>The default implementation preserves compatibility for third-party scheduler implementations. Implementations
     * with native entity-retirement support should override this method.
     *
     * @param entity the entity that owns the task
     * @param task the work to execute
     * @param retired cleanup to execute when the entity retires before the task can run
     * @return the scheduled task handle
     */
    @Nonnull
    default TaskHandle runFor(
            @Nonnull Entity entity, @Nonnull Runnable task, @Nonnull Runnable retired) {
        return runFor(entity, task);
    }

    @Nonnull
    TaskHandle runForLater(@Nonnull Entity entity, @Nonnull Runnable task, long delayTicks);

    /**
     * Runs delayed entity-owned work and invokes {@code retired} if the entity becomes retired before execution.
     *
     * <p>The default implementation preserves compatibility for third-party scheduler implementations. Implementations
     * with native entity-retirement support should override this method.
     *
     * @param entity the entity that owns the task
     * @param task the work to execute
     * @param retired cleanup to execute when the entity retires before the task can run
     * @param delayTicks the delay in server ticks
     * @return the scheduled task handle
     */
    @Nonnull
    default TaskHandle runForLater(
            @Nonnull Entity entity,
            @Nonnull Runnable task,
            @Nonnull Runnable retired,
            long delayTicks) {
        return runForLater(entity, task, delayTicks);
    }

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

    /**
     * Returns whether the current thread owns the supplied entity.
     *
     * <p>The default preserves binary compatibility for external scheduler implementations. Implementations with
     * native entity ownership checks should override it.
     *
     * @param entity the entity to test
     * @return whether the current execution context owns the entity
     */
    default boolean isOwnedByCurrentRegion(@Nonnull Entity entity) {
        return !isFolia();
    }

    /**
     * Returns whether this scheduler is using Folia regionized semantics.
     *
     * @return whether Folia is active
     */
    default boolean isFolia() {
        return false;
    }

    void cancelAll();
}
