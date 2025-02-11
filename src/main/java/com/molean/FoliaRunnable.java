package com.molean;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;

@Deprecated
@ApiStatus.ScheduledForRemoval
public abstract class FoliaRunnable {
    private ScheduledTask scheduledTask;

    public ScheduledTask runTaskTimerAsynchronously(Plugin plugin, long delay, long period) {
        return scheduledTask = Bukkit.getAsyncScheduler()
                .runAtFixedRate(
                        plugin,
                        scheduledTask1 -> run(),
                        Math.max(1, delay) * 50L,
                        Math.max(1, period) * 50L,
                        TimeUnit.MILLISECONDS);
    }

    public ScheduledTask runTaskAsynchronously(Plugin plugin) {
        return scheduledTask = Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask1 -> run());
    }

    public ScheduledTask runTaskTimer(Plugin plugin, Location location, long delay, long period) {
        return scheduledTask = Bukkit.getRegionScheduler()
                .runAtFixedRate(plugin, location, scheduledTask1 -> run(), Math.max(1, delay), Math.max(1, period));
    }

    public ScheduledTask runTaskLaterAsynchronously(Plugin plugin, long delay) {
        return scheduledTask = Bukkit.getAsyncScheduler()
                .runDelayed(plugin, scheduledTask1 -> run(), delay * 50L, TimeUnit.MILLISECONDS);
    }

    public ScheduledTask runTaskLater(Plugin plugin, Location location, long delay) {
        return scheduledTask =
                Bukkit.getRegionScheduler().runDelayed(plugin, location, scheduledTask1 -> run(), Math.max(1, delay));
    }

    public ScheduledTask runTaskTimer(Plugin plugin, Entity entity, long delay, long period) {
        return scheduledTask = entity.getScheduler()
                .runAtFixedRate(plugin, scheduledTask1 -> run(), null, Math.max(1, delay), Math.max(1, period));
    }

    public ScheduledTask runTaskLater(Plugin plugin, Entity entity, long delay) {
        return scheduledTask =
                entity.getScheduler().runDelayed(plugin, scheduledTask1 -> run(), null, Math.max(1, delay));
    }

    public abstract void run();

    public void cancel() {
        scheduledTask.cancel();
    }

    public boolean isCancelled() {
        return scheduledTask.isCancelled();
    }
}
