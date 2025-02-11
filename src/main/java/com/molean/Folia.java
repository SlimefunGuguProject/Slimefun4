package com.molean;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import it.unimi.dsi.fastutil.Pair;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.misc.Unsafe;

@Deprecated
@ApiStatus.ScheduledForRemoval
public class Folia {
    private static Scheduler scheduler = new Scheduler();

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static class Scheduler {
        public ScheduledTask runTaskTimer(Plugin plugin, Runnable runnable, Entity entity, long delay, long period) {
            return entity.getScheduler()
                    .runAtFixedRate(
                            plugin, scheduledTask -> runnable.run(), null, Math.max(1, delay), Math.max(1, period));
        }

        public @NotNull ScheduledTask runTaskTimer(
                Plugin plugin, Runnable runnable, Location location, long delay, long period) {
            return Bukkit.getRegionScheduler()
                    .runAtFixedRate(
                            plugin, location, scheduledTask -> runnable.run(), Math.max(1, delay), Math.max(1, period));
        }

        public ScheduledTask runTaskTimer(
                Plugin plugin, Consumer<ScheduledTask> runnable, Entity entity, long delay, long period) {
            return entity.getScheduler()
                    .runAtFixedRate(plugin, runnable, null, Math.max(1, delay), Math.max(1, period));
        }

        public @NotNull ScheduledTask runTaskTimer(
                Plugin plugin, Consumer<ScheduledTask> runnable, Location location, long delay, long period) {
            return Bukkit.getRegionScheduler()
                    .runAtFixedRate(plugin, location, runnable, Math.max(1, delay), Math.max(1, period));
        }

        public @NotNull ScheduledTask runTaskTimerAsynchronously(
                Plugin plugin, Runnable runnable, long delay, long period) {
            return Bukkit.getAsyncScheduler()
                    .runAtFixedRate(
                            plugin,
                            scheduledTask -> runnable.run(),
                            Math.max(1, delay) * 50L,
                            Math.max(1, period) * 50L,
                            TimeUnit.MILLISECONDS);
        }

        public @NotNull ScheduledTask runTaskTimerGlobally(Plugin plugin, Runnable runnable, long delay, long period) {
            return Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, scheduledTask -> runnable.run(), delay, period);
        }

        public @NotNull ScheduledTask runTaskLaterAsync(Plugin plugin, Runnable runnable, long delay) {
            return Bukkit.getAsyncScheduler()
                    .runDelayed(
                            plugin, scheduledTask -> runnable.run(), Math.max(1, delay) * 50L, TimeUnit.MILLISECONDS);
        }

        public @Nullable ScheduledTask scheduleSyncDelayedTask(
                Plugin plugin, Entity entity, Runnable runnable, long delay) {
            return entity.getScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), null, Math.max(1, delay));
        }

        public @Nullable ScheduledTask scheduleSyncDelayedTask(
                Plugin plugin, Location location, Runnable runnable, long delay) {
            return Bukkit.getRegionScheduler()
                    .runDelayed(plugin, location, scheduledTask -> runnable.run(), Math.max(1, delay));
        }

        public @Nullable ScheduledTask runTask(Plugin plugin, Entity entity, Runnable runnable) {
            return entity.getScheduler().run(plugin, scheduledTask -> runnable.run(), null);
        }

        public @Nullable ScheduledTask runTask(Plugin plugin, World world, int chunkX, int chunkZ, Runnable runnable) {
            return Bukkit.getRegionScheduler().run(plugin, world, chunkX, chunkZ, scheduledTask -> runnable.run());
        }

        public @Nullable ScheduledTask runTask(Plugin plugin, Location location, Runnable runnable) {
            return Bukkit.getRegionScheduler().run(plugin, location, scheduledTask -> runnable.run());
        }

        public <T> Future<T> callSyncMethod(Plugin plugin, Callable<T> callable, Pair<Entity, Location> right) {

            CompletableFuture<T> tCompletableFuture = new CompletableFuture<>();
            if (right.left() == null) {
                if (right.right() == null) {
                    throw new RuntimeException("Context not enough!");
                }
                runTask(plugin, right.right(), () -> {
                    try {
                        tCompletableFuture.complete(callable.call());
                    } catch (Exception e) {
                        tCompletableFuture.completeExceptionally(e);
                    }
                });
            } else {
                runTask(plugin, right.left(), () -> {
                    try {
                        tCompletableFuture.complete(callable.call());
                    } catch (Exception e) {
                        tCompletableFuture.completeExceptionally(e);
                    }
                });
            }
            return tCompletableFuture;
        }

        public @Nullable ScheduledTask runTaskLater(Plugin plugin, Entity entity, Runnable runnable, long delay) {
            return entity.getScheduler().runDelayed(plugin, scheduledTask -> runnable.run(), null, Math.max(1, delay));
        }

        public @Nullable ScheduledTask runTaskLater(Plugin plugin, Location location, Runnable runnable, long delay) {
            return Bukkit.getRegionScheduler()
                    .runDelayed(plugin, location, scheduledTask -> runnable.run(), Math.max(1, delay));
        }

        public ScheduledTask runTaskAsynchronously(Plugin plugin, Runnable runnable) {
            return Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> runnable.run());
        }

        public ScheduledTask runTaskAsynchronously(Plugin plugin, Consumer<ScheduledTask> runnable) {
            return Bukkit.getAsyncScheduler().runNow(plugin, runnable);
        }

        public @NotNull ScheduledTask runTaskTimerAsynchronously(
                Plugin plugin, Consumer<ScheduledTask> consumer, long delay, long period) {
            return Bukkit.getAsyncScheduler()
                    .runAtFixedRate(
                            plugin,
                            consumer,
                            Math.max(1, delay) * 50L,
                            Math.max(1, period) * 50L,
                            TimeUnit.MILLISECONDS);
        }
    }

    private static Plugin plugin;

    private static Plugin getPlugin() {
        return plugin;
    }

    public static void setPlugin(Plugin plugin) {
        Folia.plugin = plugin;
    }

    public static ScheduledTask runSync(Runnable runnable, Entity entity, long delay) {
        if (getPlugin() == null || !getPlugin().isEnabled()) {
            return null;
        }
        return getScheduler().runTaskLater(getPlugin(), entity, runnable, delay);
    }

    public static ScheduledTask runSync(Runnable runnable, Location location, long delay) {
        if (getPlugin() == null || !getPlugin().isEnabled()) {
            return null;
        }
        return getScheduler().runTaskLater(getPlugin(), location, runnable, delay);
    }

    public static ScheduledTask runSync(Runnable runnable, Entity entity) {
        return runSync(runnable, entity, 1);
    }

    public static ScheduledTask runSync(Runnable runnable, Location location) {
        return runSync(runnable, location, 1);
    }

    public static void runAtFirstTick(Plugin plugin, Runnable runnable) {
        com.molean.folia.adapter.Folia.runAtFirstTick(plugin, runnable);
    }

    private static boolean paper = false;

    public static class PluginManager {

        public void callEvent(Event event) {
            ce(event);
        }

        public void ce(Event event) {
            if (!paper) {
                try {
                    // 如果是Folia，关闭callEvent的线程检查
                    Constructor<Unsafe> declaredConstructor = Unsafe.class.getDeclaredConstructor();
                    declaredConstructor.setAccessible(true);
                    Unsafe unsafe = declaredConstructor.newInstance();
                    Field asyncField = Event.class.getDeclaredField("async");
                    unsafe.putBoolean(
                            event,
                            unsafe.objectFieldOffset(asyncField),
                            !Class.forName("ca.spottedleaf.moonrise.common.util.TickThread")
                                    .isAssignableFrom(Thread.currentThread().getClass()));
                } catch (NoSuchFieldException
                        | IllegalAccessException
                        | ClassNotFoundException
                        | InstantiationException
                        | NoSuchMethodException
                        | InvocationTargetException ignored) {
                    // paper
                    paper = true;
                    return;
                }
            }
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    private static final PluginManager pluginManager = new PluginManager();

    public static PluginManager getPluginManager() {
        return pluginManager;
    }
}
