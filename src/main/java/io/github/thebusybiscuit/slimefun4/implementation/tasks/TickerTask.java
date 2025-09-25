package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import city.norain.slimefun4.utils.SlimefunPoolExecutor;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalBlock;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.bakedlibs.dough.blocks.ChunkPosition;
import io.github.thebusybiscuit.slimefun4.api.ErrorReport;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.ticker.TickLocation;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Setter;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.apache.commons.lang.Validate;
import org.bukkit.Chunk;
import org.bukkit.Location;

/**
 * The {@link TickerTask} is responsible for ticking every {@link BlockTicker},
 * synchronous or not.
 *
 * @author TheBusyBiscuit
 * @see BlockTicker
 */
public class TickerTask implements Runnable {

    /**
     * This Map holds all currently actively ticking locations.
     * The value of this map (Set entries) MUST be thread-safe and mutable.
     */
    private final Map<ChunkPosition, Set<TickLocation>> tickingLocations = new ConcurrentHashMap<>();

    /**
     * This Map tracks how many bugs have occurred in a given Location .
     * If too many bugs happen, we delete that Location.
     */
    private final Map<BlockPosition, Integer> bugs = new ConcurrentHashMap<>();

    private final ThreadFactory tickerThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("SF-Ticker-%d")
            .setDaemon(true)
            .setUncaughtExceptionHandler(
                    (t, e) -> Slimefun.logger().log(Level.SEVERE, e, () -> "tick 时发生异常 (@" + t.getName() + ")"))
            .build();

    /**
     * 负责并发运行部分可异步的 Tick 任务的 {@link ExecutorService} 实例.
     */
    private ExecutorService asyncTickerService;

    private ExecutorService fallbackTickerService;

    private int tickRate;

    /**
     * 该标记代表 TickerTask 已被终止.
     */
    private volatile boolean halted = false;

    /**
     * 该标记代表 TickerTask 正在运行.
     */
    private volatile boolean running = false;

    /**
     * 该标记代表 TickerTask 暂时被暂停.
     */
    @Setter
    private volatile boolean paused = false;

    /**
     * This method starts the {@link TickerTask} on an asynchronous schedule.
     */
    public void start() {
        this.tickRate = Slimefun.getCfg().getInt("URID.custom-ticker-delay");

        var initSize = Slimefun.getConfigManager().getAsyncTickerInitSize();
        var maxSize = Slimefun.getCfg().getInt("URID.custom-async-ticker.max-size");

        boolean change = false;

        if (maxSize < 0) {
            maxSize = initSize;
            Slimefun.logger().log(Level.WARNING, "当前设置的 Ticker 线程池最大大小异常，已自动设置为 {0}，请你修改为一个正常的大小", maxSize);
            Slimefun.getCfg().setValue("URID.custom-async-ticker.max-size", maxSize);
            change = true;
        }

        if (initSize > maxSize) {
            initSize = maxSize;
            Slimefun.logger().log(Level.WARNING, "当前设置的 Ticker 线程池初始大小过大，已被重设至 {0}，建议修改为小于 {1} 的值。", new Object[] {
                maxSize, maxSize - 1
            });
            Slimefun.getCfg().setValue("URID.custom.async-ticker.init-size", initSize);
            change = true;
        }

        var poolSize = Slimefun.getCfg().getInt("URID.custom-async-ticker.pool-size");

        if (poolSize < 1024) {
            Slimefun.logger().log(Level.WARNING, "当前设置的 Ticker 线程池任务队列大小过小，请修改成一个大于 1024 的数。");
            poolSize = 1024;
            Slimefun.getCfg().setValue("URID.custom-async-ticker.pool-size", poolSize);
            change = true;
        }

        if (change) {
            Slimefun.getCfg().save();
        }
        this.asyncTickerService = new SlimefunPoolExecutor(
                "Slimefun-Ticker-Pool",
                initSize - 1,
                maxSize - 1,
                1,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(poolSize),
                tickerThreadFactory,
                (r, e) -> {
                    // 任务队列已满，使用备用的单线程池执行该任务
                    fallbackTickerService.execute(r);
                });

        this.fallbackTickerService = new SlimefunPoolExecutor(
                "Slimefun-Ticker-Fallback-Service",
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                tickerThreadFactory);

        Slimefun.getPlatformScheduler().runTimerAsync(this, 100L, tickRate);
    }

    /**
     * This method resets this {@link TickerTask} to run again.
     */
    private void reset() {
        running = false;
    }

    @Override
    public void run() {
        if (paused) {
            return;
        }

        try {
            // If this method is actually still running... DON'T
            if (running) {
                return;
            }

            running = true;
            Slimefun.getProfiler().start();
            Set<BlockTicker> tickers = new HashSet<>();

            // Run our ticker code
            if (!halted) {
                Set<Map.Entry<ChunkPosition, Set<TickLocation>>> loc;

                loc = new HashSet<>(tickingLocations.entrySet());

                for (Map.Entry<ChunkPosition, Set<TickLocation>> entry : loc) {
                    tickChunk(entry.getKey(), tickers, new HashSet<>(entry.getValue()));
                }
            }

            // Start a new tick cycle for every BlockTicker
            for (BlockTicker ticker : tickers) {
                ticker.startNewTick();
            }

            Slimefun.getProfiler().stop();
        } catch (Exception | LinkageError x) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "An Exception was caught while ticking the Block Tickers Task for Slimefun v"
                                    + Slimefun.getVersion());
        } finally {
            reset();
        }
    }

    @ParametersAreNonnullByDefault
    private void tickChunk(ChunkPosition chunk, Set<BlockTicker> tickers, Set<TickLocation> locations) {
        try {
            // Only continue if the Chunk is actually loaded
            if (chunk.isLoaded()) {
                for (TickLocation l : locations) {
                    if (l.isUniversal()) {
                        tickUniversalLocation(l.getUuid(), l.getLocation(), tickers);
                    } else {
                        tickLocation(tickers, l.getLocation());
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException x) {
            Slimefun.logger()
                    .log(Level.SEVERE, x, () -> "An Exception has occurred while trying to resolve Chunk: " + chunk);
        }
    }

    private void tickLocation(@Nonnull Set<BlockTicker> tickers, @Nonnull Location l) {
        var blockData = StorageCacheUtils.getBlock(l);
        if (blockData == null || !blockData.isDataLoaded() || blockData.isPendingRemove()) {
            return;
        }

        SlimefunItem item = SlimefunItem.getById(blockData.getSfId());

        if (item == null) {
            return;
        }

        callBlockTicker(l, item, blockData, tickers);
    }

    @ParametersAreNonnullByDefault
    private void tickUniversalLocation(UUID uuid, Location l, @Nonnull Set<BlockTicker> tickers) {
        var uniData = StorageCacheUtils.getUniversalBlock(uuid);
        if (uniData == null || !uniData.isDataLoaded() || uniData.isPendingRemove()) {
            return;
        }

        var item = SlimefunItem.getById(uniData.getSfId());

        if (item == null) {
            return;
        }

        callBlockTicker(l, item, uniData, tickers);
    }

    @ParametersAreNonnullByDefault
    private void callBlockTicker(
            Location l, SlimefunItem item, ASlimefunDataContainer data, @Nonnull Set<BlockTicker> tickers) {
        if (item.getBlockTicker() != null) {
            if (item.isDisabledIn(l.getWorld())) {
                return;
            }

            try {
                if (item.getBlockTicker().isSynchronized()) {
                    Slimefun.getProfiler().scheduleEntries(1);
                    item.getBlockTicker().update();

                    /**
                     * We are inserting a new timestamp because synchronized actions
                     * are always ran with a 50ms delay (1 game tick)
                     */
                    Slimefun.runSync(
                            () -> {
                                if (data.isPendingRemove()) {
                                    return;
                                }
                                tickBlock(l, item, data, System.nanoTime());
                            },
                            l);
                } else {
                    long timestamp = Slimefun.getProfiler().newEntry();
                    item.getBlockTicker().update();

                    Runnable func = () -> {
                        try {
                            if (Slimefun.isFolia()) {
                                Slimefun.getPlatformScheduler()
                                        .runAtLocation(l, task -> tickBlock(l, item, data, timestamp));
                            } else {
                                tickBlock(l, item, data, timestamp);
                            }
                        } catch (Exception x) {
                            reportErrors(l, item, x);
                        }
                    };

                    if (item.getBlockTicker().isConcurrentSafe()) {
                        asyncTickerService.execute(func);
                    } else {
                        fallbackTickerService.execute(func);
                    }
                }

                tickers.add(item.getBlockTicker());
            } catch (Exception x) {
                reportErrors(l, item, x);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void tickBlock(Location l, SlimefunItem item, ASlimefunDataContainer data, long timestamp) {
        try {
            if (item.getBlockTicker().useUniversalData()) {
                if (data instanceof SlimefunUniversalData universalData) {
                    item.getBlockTicker().tick(l.getBlock(), item, universalData);
                } else {
                    throw new IllegalStateException("BlockTicker is universal but item is non-universal!");
                }
            } else {
                if (data instanceof SlimefunBlockData blockData) {
                    item.getBlockTicker().tick(l.getBlock(), item, blockData);
                } else {
                    throw new IllegalStateException("BlockTicker is non-universal but item is universal!");
                }
            }
        } catch (Exception | LinkageError x) {
            reportErrors(l, item, x);
        } finally {
            Slimefun.getProfiler().closeEntry(l, item, timestamp);
        }
    }

    @ParametersAreNonnullByDefault
    private void reportErrors(Location l, SlimefunItem item, Throwable x) {
        BlockPosition position = new BlockPosition(l);
        int errors = bugs.getOrDefault(position, 0) + 1;

        if (errors == 1) {
            // Generate a new Error-Report
            new ErrorReport<>(x, l, item);
            bugs.put(position, errors);
        } else if (errors == 4) {
            Slimefun.logger().log(Level.SEVERE, "X: {0} Y: {1} Z: {2} ({3})", new Object[] {
                l.getBlockX(), l.getBlockY(), l.getBlockZ(), item.getId()
            });
            Slimefun.logger().log(Level.SEVERE, "该位置上的机器在过去一段时间内多次报错，对应机器已被停用。");
            Slimefun.logger().log(Level.SEVERE, "请在 /plugins/Slimefun/error-reports/ 文件夹中查看错误详情。");
            Slimefun.logger().log(Level.SEVERE, "在反馈时，请向他人发送上述错误报告文件，而不是发送这段话的截图");
            Slimefun.logger().log(Level.SEVERE, " ");
            bugs.remove(position);

            disableTicker(l);
        } else {
            bugs.put(position, errors);
        }
    }

    public boolean isHalted() {
        return halted;
    }

    public void halt() {
        halted = true;
    }

    /**
     * This returns the delay between ticks
     *
     * @return The tick delay
     */
    public int getTickRate() {
        return tickRate;
    }

    /**
     * BINARY COMPATIBILITY
     *
     * Use #getTickLocations instead
     *
     * @return A {@link Map} representation of all ticking {@link Location Locations}
     */
    @Nonnull
    public Map<ChunkPosition, Set<Location>> getLocations() {
        return tickingLocations.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .map(TickLocation::getLocation)
                        .collect(Collectors.toUnmodifiableSet())));
    }

    /**
     * This method returns a <strong>read-only</strong> {@link Map}
     * representation of every {@link ChunkPosition} and its corresponding
     * {@link Set} of ticking {@link Location Locations}.
     *
     * This does include any {@link Location} from an unloaded {@link Chunk} too!
     *
     * @return A {@link Map} representation of all ticking {@link TickLocation Locations}
     */
    @Nonnull
    public Map<ChunkPosition, Set<TickLocation>> getTickLocations() {
        return Collections.unmodifiableMap(tickingLocations);
    }

    /**
     * This method returns a <strong>read-only</strong> {@link Set}
     * of all ticking {@link Location Locations} in a given {@link Chunk}.
     * The {@link Chunk} does not have to be loaded.
     * If no {@link Location} is present, the returned {@link Set} will be empty.
     *
     * @param chunk The {@link Chunk}
     * @return A {@link Set} of all ticking {@link Location Locations}
     */
    @Nonnull
    public Set<Location> getLocations(@Nonnull Chunk chunk) {
        Validate.notNull(chunk, "The Chunk cannot be null!");

        Set<TickLocation> locations = tickingLocations.getOrDefault(new ChunkPosition(chunk), Collections.emptySet());
        return locations.stream().map(TickLocation::getLocation).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 返回一个给定区块下的 <strong>只读</strong> 的 {@link Map}
     * 代表每个 {@link ChunkPosition} 中有 {@link UniversalBlock} 属性的物品
     * Tick 的 {@link Location 位置}集合.
     *
     * 其中包含的 {@link Location} 可以是已加载或卸载的 {@link Chunk}
     *
     * @param chunk {@link Chunk}
     * @return 包含所有机器 Tick {@link TickLocation 位置}的只读 {@link Map}
     */
    @Nonnull
    public Set<TickLocation> getTickLocations(@Nonnull Chunk chunk) {
        Validate.notNull(chunk, "The Chunk cannot be null!");

        return tickingLocations.getOrDefault(new ChunkPosition(chunk), Collections.emptySet());
    }

    /**
     * This enables the ticker at the given {@link Location} and adds it to our "queue".
     *
     * @param l The {@link Location} to activate
     */
    public void enableTicker(@Nonnull Location l) {
        enableTicker(l, null);
    }

    public void enableTicker(@Nonnull Location l, @Nullable UUID uuid) {
        Validate.notNull(l, "Location cannot be null!");

        synchronized (tickingLocations) {
            ChunkPosition chunk = new ChunkPosition(l.getWorld(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
            final var tickPosition = uuid == null
                    ? new TickLocation(new BlockPosition(l))
                    : new TickLocation(new BlockPosition(l), uuid);

            /*
              Note that all the values in #tickingLocations must be thread-safe.
              Thus, the choice is between the CHM KeySet or a synchronized set.
              The CHM KeySet was chosen since it at least permits multiple concurrent
              reads without blocking.
            */
            Set<TickLocation> newValue = ConcurrentHashMap.newKeySet();
            Set<TickLocation> oldValue = tickingLocations.putIfAbsent(chunk, newValue);

            /**
             * This is faster than doing computeIfAbsent(...)
             * on a ConcurrentHashMap because it won't block the Thread for too long
             */
            if (oldValue != null) {
                oldValue.add(tickPosition);
            } else {
                newValue.add(tickPosition);
            }
        }
    }

    /**
     * This method disables the ticker at the given {@link Location} and removes it from our internal
     * "queue".
     *
     * @param l The {@link Location} to remove
     */
    public void disableTicker(@Nonnull Location l) {
        Validate.notNull(l, "Location cannot be null!");

        synchronized (tickingLocations) {
            ChunkPosition chunk = new ChunkPosition(l.getWorld(), l.getBlockX() >> 4, l.getBlockZ() >> 4);
            Set<TickLocation> locations = tickingLocations.get(chunk);

            if (locations != null) {
                locations.removeIf(tk -> l.equals(tk.getLocation()));

                if (locations.isEmpty()) {
                    tickingLocations.remove(chunk);
                }
            }
        }
    }

    /**
     * This method disables the ticker at the given {@link UUID} and removes it from our internal
     * "queue".
     *
     * We don't recommend disable by this way unless you only have UUID of universal data.
     *
     * @param uuid The {@link UUID} to remove
     */
    public void disableTicker(@Nonnull UUID uuid) {
        Validate.notNull(uuid, "Universal Data ID cannot be null!");

        synchronized (tickingLocations) {
            tickingLocations.values().forEach(loc -> loc.removeIf(tk -> uuid.equals(tk.getUuid())));
        }
    }

    public void shutdown() {
        setPaused(true);
        halt();

        try {
            asyncTickerService.shutdown();
            if (!asyncTickerService.awaitTermination(10, TimeUnit.SECONDS)) {
                asyncTickerService.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncTickerService.shutdownNow();
        } finally {
            asyncTickerService = null;
        }

        try {
            fallbackTickerService.shutdown();
            if (!fallbackTickerService.awaitTermination(10, TimeUnit.SECONDS)) {
                fallbackTickerService.shutdownNow();
            }
        } catch (InterruptedException e) {
            fallbackTickerService.shutdownNow();
        } finally {
            fallbackTickerService = null;
        }
    }
}
