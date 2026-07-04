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
import io.github.bakedlibs.dough.folialib.impl.PlatformScheduler;
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
 * <h3>Folia 混合移植架构</h3>
 * <p>
 * 本版本融合了两种 Folia 调度策略:
 * <ul>
 *   <li><b>Craft233MC 调度:</b> 同步 ticker 使用 {@code runSyncAtLocation(location)}
 *       确保在正确的 Folia 区域线程上执行方块操作。</li>
 *   <li><b>SlimefunGuguProject 线程池:</b> 非同步通用 ticker 通过
 *       {@link SlimefunPoolExecutor} 线程池并发执行, 提升 CPU 密集型 ticker 性能。</li>
 * </ul>
 *
 * <h3>跨区域风险说明</h3>
 * <p>
 * Folia 将世界按 chunk 划分为独立区域, 每个区域由专用线程 tick。
 * 以下场景可能触发区域边界问题:
 * <ul>
 *   <li><b>货网 (CargoNet) 跨区域:</b> 节点跨越区域边界时, {@code withdraw()} 和
 *       {@code insert()} 可能在不同区域的容器上操作, 导致 {@code IllegalStateException}。</li>
 *   <li><b>能网 (EnergyNet) 跨区域:</b> {@code HashMap} 在异步 tick 中无同步保护,
 *       跨区域并发修改可能产生竞态条件。</li>
 *   <li><b>实体生成 (spawnEntity):</b> 非同步 ticker 内部如未显式调用
 *       {@code runSyncAtLocation} 包裹实体操作, 可能触发区域线程检查失败。</li>
 * </ul>
 * 这些问题主要影响大型跨区域网络, 单区域内正常使用不受影响。
 *
 * @author TheBusyBiscuit
 * @author Craft233MC (Folia 区域调度)
 * @author SlimefunGuguProject (线程池优化)
 * @author qzgeek (混合移植)
 *
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
     * 负责并发运行部分可异步的 Tick 任务的 {@link ExecutorService} 实例。
     * 来自 SlimefunGuguProject 的线程池优化。
     */
    private ExecutorService asyncTickerService;

    /**
     * 当异步线程池满载时，回退到单线程顺序执行。
     */
    private ExecutorService fallbackTickerService;

    private int tickRate;

    /**
     * 该标记代表 TickerTask 已被终止。
     */
    private volatile boolean halted = false;

    /**
     * 该标记代表 TickerTask 正在运行。
     */
    private volatile boolean running = false;

    /**
     * 该标记代表 TickerTask 暂时被暂停。
     */
    @Setter
    private volatile boolean paused = false;

    /**
     * This method starts the {@link TickerTask} on an asynchronous schedule.
     * Initializes concurrent thread pools for non-synchronized tickers.
     *
     * @param plugin
     *            The instance of our {@link Slimefun}
     */
    public void start(@Nonnull Slimefun plugin) {
        this.tickRate = Slimefun.getCfg().getInt("URID.custom-ticker-delay");

        // Initialize thread pools for concurrent ticker execution (from GuguProject)
        // Using defaults since ConfigManager async ticker methods may not be available.
        // These can be made configurable when ConfigManager is updated upstream.
        int initSize = 4;
        int maxSize = 8;
        int poolSize = 1024;

        this.asyncTickerService = new SlimefunPoolExecutor(
                "Slimefun-Ticker-Pool",
                Math.max(1, initSize - 1),
                Math.max(1, maxSize - 1),
                1,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(poolSize),
                tickerThreadFactory,
                (r, e) -> {
                    // 任务队列已满，使用备用的单线程池执行该任务
                    fallbackTickerService.submit(r);
                });

        this.fallbackTickerService = new SlimefunPoolExecutor(
                "Slimefun-Ticker-Fallback-Service",
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                tickerThreadFactory);

        PlatformScheduler scheduler = Slimefun.getFoliaLib().getScheduler();
        scheduler.runTimerAsync(this, 100L, tickRate);
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

                synchronized (tickingLocations) {
                    loc = new HashSet<>(tickingLocations.entrySet());
                }

                for (Map.Entry<ChunkPosition, Set<TickLocation>> entry : loc) {
                    tickChunk(entry.getKey(), tickers, new HashSet<>(entry.getValue()));
                }
            }

            // Start a new tick cycle for every BlockTicker
            for (BlockTicker ticker : tickers) {
                ticker.startNewTick();
            }

            reset();
            Slimefun.getProfiler().stop();
        } catch (Exception | LinkageError x) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "An Exception was caught while ticking the Block Tickers Task for Slimefun v"
                                    + Slimefun.getVersion());
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

    /**
     * Tick a standard (non-universal) location.
     * 
     * Uses Craft233's correct Folia scheduling:
     * - Synchronized tickers are dispatched via runSyncAtLocation(location) 
     *   to ensure they execute on the correct region thread.
     * - Non-synchronized tickers execute directly (they must handle their
     *   own thread safety via internal runSyncAtLocation calls).
     */
    private void tickLocation(@Nonnull Set<BlockTicker> tickers, @Nonnull Location l) {
        var blockData = StorageCacheUtils.getBlock(l);
        if (blockData == null || !blockData.isDataLoaded() || blockData.isPendingRemove()) {
            return;
        }

        SlimefunItem item = SlimefunItem.getById(blockData.getSfId());

        if (item != null && item.getBlockTicker() != null) {
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
                     *
                     * IMPORTANT: Uses runSyncAtLocation to ensure correct Folia
                     * region thread dispatch.
                     */
                    Slimefun.runSyncAtLocation(
                            () -> {
                                if (blockData.isPendingRemove()) {
                                    return;
                                }
                                tickBlock(l, item, blockData, System.nanoTime());
                            },
                            l);
                } else {
                    long timestamp = Slimefun.getProfiler().newEntry();
                    item.getBlockTicker().update();
                    tickBlock(l, item, blockData, timestamp);
                }

                tickers.add(item.getBlockTicker());
            } catch (Exception x) {
                Slimefun.runSyncAtLocation(
                        () -> {
                            reportErrors(l, item, x);
                        },
                        l);
            }
        }
    }

    /**
     * Tick a universal block location (from GuguProject with thread pool enhancement).
     * 
     * Non-synchronized tickers are submitted to the thread pool for
     * concurrent execution. On Folia, block operations inside the ticker
     * are dispatched to the correct region via runAtLocation.
     */
    @ParametersAreNonnullByDefault
    private void tickUniversalLocation(UUID uuid, Location l, @Nonnull Set<BlockTicker> tickers) {
        var data = StorageCacheUtils.getUniversalBlock(uuid);
        var item = SlimefunItem.getById(data.getSfId());

        if (item != null && item.getBlockTicker() != null) {
            if (item.isDisabledIn(l.getWorld())) {
                return;
            }

            try {
                if (item.getBlockTicker().isSynchronized()) {
                    Slimefun.getProfiler().scheduleEntries(1);
                    item.getBlockTicker().update();

                    Slimefun.runSyncAtLocation(
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

                    // Thread pool dispatch for non-synchronized tickers
                    // (from GuguProject — allows CPU-bound tickers to run concurrently)
                    Runnable func = () -> {
                        try {
                            tickBlock(l, item, data, timestamp);
                        } catch (Exception x) {
                            Slimefun.runSyncAtLocation(
                                    () -> reportErrors(l, item, x),
                                    l);
                        }
                    };

                    if (item.getBlockTicker().isConcurrent()) {
                        asyncTickerService.execute(func);
                    } else {
                        fallbackTickerService.execute(func);
                    }
                }

                tickers.add(item.getBlockTicker());
            } catch (Exception x) {
                Slimefun.runSyncAtLocation(
                        () -> reportErrors(l, item, x),
                        l);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void tickBlock(Location l, SlimefunItem item, ASlimefunDataContainer data, long timestamp) {
        try {
            if (item.getBlockTicker().isUniversal()) {
                if (data instanceof SlimefunUniversalData universalData) {
                    item.getBlockTicker().tick(l.getBlock(), item, universalData);
                } else {
                    throw new IllegalStateException("BlockTicker is universal but item is non-universal!");
                }
            } else {
                if (data instanceof SlimefunBlockData blockData) {
                    Slimefun.runSyncAtLocation(
                            () -> {
                                item.getBlockTicker().tick(l.getBlock(), item, blockData);
                            },
                            l);
                } else {
                    throw new IllegalStateException("BlockTicker is non-universal but item is universal!");
                }
            }
        } catch (Exception | LinkageError x) {
            Slimefun.runSyncAtLocation(
                    () -> {
                        reportErrors(l, item, x);
                    },
                    l);
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
            Slimefun.logger().log(Level.SEVERE, "在过去的 4 个 Tick 中发生多次错误，该方块对应的机器已被停用。");
            Slimefun.logger().log(Level.SEVERE, "请在 /plugins/Slimefun/error-reports/ 文件夹中查看错误详情。");
            Slimefun.logger().log(Level.SEVERE, "如果你要向他人求助，请向他人发送上述错误报告文件,而不是发送这个窗口的截图");
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
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
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
     * @param chunk
     *            The {@link Chunk}
     *
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
     * @param chunk
     *            {@link Chunk}
     *
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
     * @param l
     *            The {@link Location} to activate
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
     * @param l
     *            The {@link Location} to remove
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
     * DO NOT USE THIS until you cannot disable by location,
     * or enjoy extremely slow.
     *
     * @param uuid
     *            The {@link UUID} to remove
     */
    public void disableTicker(@Nonnull UUID uuid) {
        Validate.notNull(uuid, "Universal Data ID cannot be null!");

        synchronized (tickingLocations) {
            tickingLocations.values().forEach(loc -> loc.removeIf(tk -> uuid.equals(tk.getUuid())));
        }
    }
}
