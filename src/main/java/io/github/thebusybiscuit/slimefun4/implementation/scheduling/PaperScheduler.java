package io.github.thebusybiscuit.slimefun4.implementation.scheduling;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunInternal;
import io.github.thebusybiscuit.slimefun4.core.services.scheduling.FoliaSupport;
import io.github.thebusybiscuit.slimefun4.core.services.scheduling.SchedulerTime;
import io.github.thebusybiscuit.slimefun4.core.services.scheduling.SlimefunScheduler;
import io.github.thebusybiscuit.slimefun4.core.services.scheduling.TaskHandle;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Paper and Folia implementation of Slimefun's scheduler abstraction.
 *
 * <p>Standard Paper retains Bukkit's tick-based scheduling semantics. Folia uses its global, region, entity, and
 * asynchronous schedulers so location-bound work is executed by the owning region.
 */
@SlimefunInternal
public final class PaperScheduler implements SlimefunScheduler {

    private static final boolean FOLIA = FoliaSupport.isFolia();

    private final Plugin plugin;
    private final Set<TrackedTask> tasks = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean stopped = new AtomicBoolean();

    public PaperScheduler(@Nonnull Plugin plugin) {
        Validate.notNull(plugin, "Plugin cannot be null");
        this.plugin = plugin;
    }

    @Override
    public @Nonnull TaskHandle run(@Nonnull Runnable task) {
        Validate.notNull(task, "Task cannot be null");

        TrackedTask handle = track(false);
        if (handle.isCancelled()) {
            return handle;
        }

        if (FOLIA) {
            handle.attach(Bukkit.getGlobalRegionScheduler().run(plugin, ignored -> handle.execute(task)));
        } else {
            handle.attach(Bukkit.getScheduler().runTask(plugin, () -> handle.execute(task)));
        }
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runLater(@Nonnull Runnable task, long delayTicks) {
        Validate.notNull(task, "Task cannot be null");
        Validate.isTrue(delayTicks >= 0, "Delay cannot be negative");

        if (delayTicks == 0) {
            return run(task);
        }

        TrackedTask handle = track(false);
        if (handle.isCancelled()) {
            return handle;
        }

        if (FOLIA) {
            handle.attach(Bukkit.getGlobalRegionScheduler()
                    .runDelayed(plugin, ignored -> handle.execute(task), delayTicks));
        } else {
            handle.attach(Bukkit.getScheduler().runTaskLater(plugin, () -> handle.execute(task), delayTicks));
        }
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runAt(@Nonnull Location location, @Nonnull Runnable task) {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(task, "Task cannot be null");

        if (!FOLIA) {
            return run(task);
        }

        TrackedTask handle = track(false);
        if (handle.isCancelled()) {
            return handle;
        }

        handle.attach(Bukkit.getRegionScheduler().run(plugin, location, ignored -> handle.execute(task)));
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runAtLater(@Nonnull Location location, @Nonnull Runnable task, long delayTicks) {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(task, "Task cannot be null");
        Validate.isTrue(delayTicks >= 0, "Delay cannot be negative");

        if (!FOLIA) {
            return runLater(task, delayTicks);
        }

        if (delayTicks == 0) {
            return runAt(location, task);
        }

        TrackedTask handle = track(false);
        if (handle.isCancelled()) {
            return handle;
        }

        handle.attach(Bukkit.getRegionScheduler()
                .runDelayed(plugin, location, ignored -> handle.execute(task), delayTicks));
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runAtFixedRate(
            @Nonnull Runnable task, long initialDelayTicks, long periodTicks) {
        Validate.notNull(task, "Task cannot be null");
        validateRepeating(initialDelayTicks, periodTicks);

        TrackedTask handle = track(true);
        if (handle.isCancelled()) {
            return handle;
        }

        if (FOLIA) {
            handle.attach(Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(
                            plugin,
                            ignored -> handle.execute(task),
                            Math.max(1L, initialDelayTicks),
                            periodTicks));
        } else {
            handle.attach(Bukkit.getScheduler()
                    .runTaskTimer(plugin, () -> handle.execute(task), initialDelayTicks, periodTicks));
        }
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runAtFixedRate(
            @Nonnull Location location,
            @Nonnull Runnable task,
            long initialDelayTicks,
            long periodTicks) {
        Validate.notNull(location, "Location cannot be null");
        Validate.notNull(task, "Task cannot be null");
        validateRepeating(initialDelayTicks, periodTicks);

        if (!FOLIA) {
            return runAtFixedRate(task, initialDelayTicks, periodTicks);
        }

        TrackedTask handle = track(true);
        if (handle.isCancelled()) {
            return handle;
        }

        handle.attach(Bukkit.getRegionScheduler()
                .runAtFixedRate(
                        plugin,
                        location,
                        ignored -> handle.execute(task),
                        Math.max(1L, initialDelayTicks),
                        periodTicks));
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runFor(@Nonnull Entity entity, @Nonnull Runnable task) {
        return runFor(entity, task, () -> {});
    }

    @Override
    public @Nonnull TaskHandle runFor(
            @Nonnull Entity entity, @Nonnull Runnable task, @Nonnull Runnable retired) {
        Validate.notNull(entity, "Entity cannot be null");
        Validate.notNull(task, "Task cannot be null");
        Validate.notNull(retired, "Retired callback cannot be null");

        if (!FOLIA) {
            return run(task);
        }

        TrackedTask handle = track(false);
        if (handle.isCancelled()) {
            return handle;
        }

        handle.attach(entity.getScheduler()
                .run(plugin, ignored -> handle.execute(task), () -> handle.retire(retired)));
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runForLater(@Nonnull Entity entity, @Nonnull Runnable task, long delayTicks) {
        return runForLater(entity, task, () -> {}, delayTicks);
    }

    @Override
    public @Nonnull TaskHandle runForLater(
            @Nonnull Entity entity,
            @Nonnull Runnable task,
            @Nonnull Runnable retired,
            long delayTicks) {
        Validate.notNull(entity, "Entity cannot be null");
        Validate.notNull(task, "Task cannot be null");
        Validate.notNull(retired, "Retired callback cannot be null");
        Validate.isTrue(delayTicks >= 0, "Delay cannot be negative");

        if (!FOLIA) {
            return runLater(task, delayTicks);
        }

        if (delayTicks == 0) {
            return runFor(entity, task, retired);
        }

        TrackedTask handle = track(false);
        if (handle.isCancelled()) {
            return handle;
        }

        handle.attach(entity.getScheduler()
                .runDelayed(
                        plugin,
                        ignored -> handle.execute(task),
                        () -> handle.retire(retired),
                        delayTicks));
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runForAtFixedRate(
            @Nonnull Entity entity,
            @Nonnull Runnable task,
            long initialDelayTicks,
            long periodTicks) {
        Validate.notNull(entity, "Entity cannot be null");
        Validate.notNull(task, "Task cannot be null");
        validateRepeating(initialDelayTicks, periodTicks);

        if (!FOLIA) {
            return runAtFixedRate(task, initialDelayTicks, periodTicks);
        }

        TrackedTask handle = track(true);
        if (handle.isCancelled()) {
            return handle;
        }

        handle.attach(entity.getScheduler()
                .runAtFixedRate(
                        plugin,
                        ignored -> handle.execute(task),
                        handle::retire,
                        Math.max(1L, initialDelayTicks),
                        periodTicks));
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runAsync(@Nonnull Runnable task) {
        Validate.notNull(task, "Task cannot be null");

        TrackedTask handle = track(false);
        if (handle.isCancelled()) {
            return handle;
        }

        if (FOLIA) {
            handle.attach(Bukkit.getAsyncScheduler().runNow(plugin, ignored -> handle.execute(task)));
        } else {
            handle.attach(Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> handle.execute(task)));
        }
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runAsyncLater(@Nonnull Runnable task, long delayTicks) {
        Validate.notNull(task, "Task cannot be null");
        Validate.isTrue(delayTicks >= 0, "Delay cannot be negative");

        if (delayTicks == 0) {
            return runAsync(task);
        }

        TrackedTask handle = track(false);
        if (handle.isCancelled()) {
            return handle;
        }

        if (FOLIA) {
            handle.attach(Bukkit.getAsyncScheduler()
                    .runDelayed(
                            plugin,
                            ignored -> handle.execute(task),
                            SchedulerTime.ticksToMillis(delayTicks),
                            TimeUnit.MILLISECONDS));
        } else {
            handle.attach(Bukkit.getScheduler()
                    .runTaskLaterAsynchronously(plugin, () -> handle.execute(task), delayTicks));
        }
        return handle;
    }

    @Override
    public @Nonnull TaskHandle runAsyncAtFixedRate(
            @Nonnull Runnable task, long initialDelayTicks, long periodTicks) {
        Validate.notNull(task, "Task cannot be null");
        validateRepeating(initialDelayTicks, periodTicks);

        TrackedTask handle = track(true);
        if (handle.isCancelled()) {
            return handle;
        }

        if (FOLIA) {
            handle.attach(Bukkit.getAsyncScheduler()
                    .runAtFixedRate(
                            plugin,
                            ignored -> handle.execute(task),
                            Math.max(1L, SchedulerTime.ticksToMillis(initialDelayTicks)),
                            SchedulerTime.ticksToMillis(periodTicks),
                            TimeUnit.MILLISECONDS));
        } else {
            handle.attach(Bukkit.getScheduler()
                    .runTaskTimerAsynchronously(
                            plugin, () -> handle.execute(task), initialDelayTicks, periodTicks));
        }
        return handle;
    }

    @Override
    public boolean isOwnedByCurrentRegion(@Nonnull Location location) {
        Validate.notNull(location, "Location cannot be null");
        return FOLIA ? Bukkit.isOwnedByCurrentRegion(location) : Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isOwnedByCurrentRegion(@Nonnull Entity entity) {
        Validate.notNull(entity, "Entity cannot be null");
        return FOLIA ? Bukkit.isOwnedByCurrentRegion(entity) : Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isFolia() {
        return FOLIA;
    }

    @Override
    public void cancelAll() {
        stopped.set(true);

        for (TrackedTask task : Set.copyOf(tasks)) {
            task.cancel();
        }
    }

    private void validateRepeating(long initialDelayTicks, long periodTicks) {
        Validate.isTrue(initialDelayTicks >= 0, "Initial delay cannot be negative");
        Validate.isTrue(periodTicks > 0, "Period must be greater than zero");
    }

    private TrackedTask track(boolean repeating) {
        TrackedTask task = new TrackedTask(repeating);

        if (stopped.get()) {
            task.cancel();
            return task;
        }

        tasks.add(task);
        if (stopped.get()) {
            task.cancel();
        }

        return task;
    }

    private final class TrackedTask implements TaskHandle {

        private final boolean repeating;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private volatile Runnable cancellation;

        private TrackedTask(boolean repeating) {
            this.repeating = repeating;
        }

        private void attach(ScheduledTask scheduledTask) {
            if (scheduledTask == null) {
                retire();
                return;
            }

            attachCancellation(scheduledTask::cancel);
        }

        private void attach(BukkitTask bukkitTask) {
            attachCancellation(bukkitTask::cancel);
        }

        private void attachCancellation(Runnable cancellationAction) {
            cancellation = cancellationAction;

            if (cancelled.get()) {
                cancellationAction.run();
            }
        }

        private void retire() {
            retire(null);
        }

        private void retire(Runnable retiredCallback) {
            boolean firstRetirement = cancelled.compareAndSet(false, true);
            tasks.remove(this);

            if (firstRetirement && retiredCallback != null) {
                try {
                    retiredCallback.run();
                } catch (RuntimeException | LinkageError ex) {
                    plugin.getLogger().log(Level.WARNING, "A scheduler retirement callback failed.", ex);
                }
            }
        }

        private void execute(Runnable runnable) {
            if (cancelled.get()) {
                return;
            }

            try {
                runnable.run();
            } finally {
                if (!repeating) {
                    tasks.remove(this);
                }
            }
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                tasks.remove(this);

                Runnable cancellationAction = cancellation;
                if (cancellationAction != null) {
                    cancellationAction.run();
                }
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }
    }
}
