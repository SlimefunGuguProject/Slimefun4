package io.github.thebusybiscuit.slimefun4.implementation.scheduling;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunInternal;
import io.github.thebusybiscuit.slimefun4.core.services.scheduling.TaskHandle;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.apache.commons.lang.Validate;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Adapts a scheduler-neutral {@link TaskHandle} to the historical {@link BukkitTask} return type.
 *
 * <p>This bridge keeps Slimefun's existing static scheduling helpers binary compatible while allowing their work to
 * be tracked and cancelled by the modern scheduler abstraction. Adapter task identifiers are negative so they cannot
 * be confused with native Bukkit scheduler identifiers.
 */
@SlimefunInternal
public final class LegacyBukkitTask implements BukkitTask {

    private static final AtomicInteger NEXT_TASK_ID = new AtomicInteger(-1);

    private final int taskId = NEXT_TASK_ID.getAndDecrement();
    private final Plugin owner;
    private final TaskHandle handle;
    private final boolean synchronous;

    public LegacyBukkitTask(@Nonnull Plugin owner, @Nonnull TaskHandle handle, boolean synchronous) {
        Validate.notNull(owner, "Owner cannot be null");
        Validate.notNull(handle, "Task handle cannot be null");
        this.owner = owner;
        this.handle = handle;
        this.synchronous = synchronous;
    }

    @Override
    public int getTaskId() {
        return taskId;
    }

    @Override
    public @Nonnull Plugin getOwner() {
        return owner;
    }

    @Override
    public boolean isSync() {
        return synchronous;
    }

    @Override
    public boolean isCancelled() {
        return handle.isCancelled();
    }

    @Override
    public void cancel() {
        handle.cancel();
    }
}
