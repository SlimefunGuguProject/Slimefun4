package io.github.thebusybiscuit.slimefun4.implementation.tasks;

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
import io.github.thebusybiscuit.slimefun4.utils.ParticleUtil;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * The {@link TickerTask} is responsible for ticking every {@link BlockTicker},
 * synchronous or not.
 *
 * @author TheBusyBiscuit
 *
 * @see BlockTicker
 *
 */
@Getter
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

    private int count = 0;

    /**
     * -- GETTER --
     *  This returns the delay between ticks
     *
     * @return The tick delay
     */
    @Setter
    private int tickRate;

    private boolean halted = false;
    private boolean running = false;

    @Setter
    private volatile boolean paused = false;

    private final Deque<WaitingEntry> waiting = new ConcurrentLinkedDeque<>();

    private final ExecutorService tickExecutor =
            Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Slimefun - Ticker Thread"));

    /**
     * @see #stepTo(WaitingEntry, boolean)
     */
    private final ExecutorService operateExecutor =
            Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "Slimefun - Ticker Operation Thread"));

    /**
     * 防抖
     * 表示是否有一段步进任务已在 {@link #operateExecutor} 上跑。见 {@link #stepTo}。
     */
    private final AtomicBoolean stepping = new AtomicBoolean(false);

    private final int PAGE_SIZE = 10;

    @Setter
    private volatile boolean tickFreeze = false;

    // false -> 不暂停， true -> 暂停
    private volatile Predicate<WaitingEntry> tickFreezePredicate = entry -> false;

    public void setTickFreezePredicate(Predicate<WaitingEntry> predicate) {
        this.tickFreezePredicate = predicate != null ? predicate : entry -> false;
    }

    @Data
    public static class WaitingEntry {
        private static final AtomicLong ID = new AtomicLong(0);
        private final Location location;
        private final SlimefunItem item;
        private final ASlimefunDataContainer data;
        private final boolean sync;
        private final long id;

        public WaitingEntry(Location location, SlimefunItem item, ASlimefunDataContainer data, boolean sync) {
            this.location = location;
            this.item = item;
            this.data = data;
            this.sync = sync;
            this.id = ID.getAndIncrement();
        }
    }

    /**
     * This method starts the {@link TickerTask} on an asynchronous schedule.
     *
     * @param plugin
     *            The instance of our {@link Slimefun}
     */
    public void start(@Nonnull Slimefun plugin) {
        this.tickRate = Slimefun.getCfg().getInt("URID.custom-ticker-delay");

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTaskTimerAsynchronously(plugin, this, 100L, 1);
    }

    /**
     * This method resets this {@link TickerTask} to run again.
     */
    private void reset() {
        running = false;
    }

    @Override
    public void run() {
        if (paused || stepping.get()) { // 步过期间不执行
            return;
        }

        if (tickFreeze && !waiting.isEmpty()) {
            return;
        }

        count += 1;
        if (count < tickRate) {
            return;
        }
        count = 0;

        // If this method is actually still running... DON'T
        if (running) {
            return;
        }

        running = true;

        try {
            flushWaitingEntries(); // 上轮 tick freeze 没执行的
            Slimefun.getProfiler().start();
            Set<BlockTicker> tickers = new HashSet<>();

            // Run our ticker code
            if (!halted) {
                Set<Map.Entry<ChunkPosition, Set<TickLocation>>> loc;

                synchronized (tickingLocations) {
                    loc = new HashSet<>(tickingLocations.entrySet());
                }

                for (Map.Entry<ChunkPosition, Set<TickLocation>> entry : loc) {
                    // 将所有机器添加到 waiting queue 中，在这里会检查 tickFreeze
                    tickChunk(entry.getKey(), tickers, new HashSet<>(entry.getValue()));
                }
            }

            // 遍历阶段结束，本轮机器才真正开始执行
            if (!tickFreeze) {
                flushWaitingEntries();
            }

            // Start a new tick cycle for every BlockTicker
            for (BlockTicker ticker : tickers) {
                ticker.startNewTick();
            }

            reset();
            Slimefun.getProfiler().stop();

            if (tickFreeze) {
                showWaitingList();
            }
        } catch (Exception | LinkageError x) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "An Exception was caught while ticking the Block Tickers Task for Slimefun v"
                                    + Slimefun.getVersion());
            reset();

            int discarded = clearWaitingEntries();
            if (discarded > 0) {
                Slimefun.logger().log(Level.SEVERE, "有 " + discarded + " 个机器未能成功 Tick，已从待执行队列丢弃");
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void tickChunk(ChunkPosition chunk, Set<BlockTicker> tickers, Set<TickLocation> locations) {
        // Only continue if the Chunk is actually loaded
        if (!chunk.isLoaded()) return;

        try {
            for (TickLocation l : locations) {
                if (l.isUniversal()) {
                    tickUniversalLocation(l.getUuid(), l.getLocation(), tickers);
                } else {
                    tickLocation(tickers, l.getLocation());
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
        if (item != null && item.getBlockTicker() != null && !item.isDisabledIn(l.getWorld())) {
            enqueueTick(l, item, blockData, tickers);
        }
    }

    @ParametersAreNonnullByDefault
    private void tickUniversalLocation(UUID uuid, Location l, @Nonnull Set<BlockTicker> tickers) {
        var data = StorageCacheUtils.getUniversalBlock(uuid);
        var item = SlimefunItem.getById(data.getSfId());
        if (item != null && item.getBlockTicker() != null && !item.isDisabledIn(l.getWorld())) {
            enqueueTick(l, item, data, tickers);
        }
    }

    /**
     * 放入 waiting
     * 真正的执行在 {@link #flushWaitingEntries()}。
     */
    @ParametersAreNonnullByDefault
    private void enqueueTick(Location l, SlimefunItem item, ASlimefunDataContainer data, Set<BlockTicker> tickers) {
        try {
            boolean sync = item.getBlockTicker().isSynchronized();

            item.getBlockTicker().update();
            waiting.add(new WaitingEntry(l, item, data, sync));

            tickers.add(item.getBlockTicker());
        } catch (Exception | LinkageError x) {
            reportErrors(l, item, x);
        }
    }

    private void timedTickBlock(boolean checkTickFreeze) {
        if (checkTickFreeze && tickFreeze) return;

        WaitingEntry entry = waiting.poll();
        if (entry == null) return;

        if (checkTickFreeze && tickFreezePredicate.test(entry)) {
            waiting.addFirst(entry);
            tickFreeze = true;
            return;
        }

        timedTickBlock(entry);
    }

    /**
     * 执行堆积 waiting 的全部 tick 任务
     */
    private void flushWaitingEntries() {
        while (!waiting.isEmpty() && !tickFreeze) {
            timedTickBlock(true);
        }
    }

    /**
     * {@link ClickEvent#callback} 会运行在主线程上，步过/运行到 操作可能导致阻塞主线程
     * 使用 {@code operateExecutor} 避免阻塞主线程
     */
    private void stepTo(WaitingEntry target, boolean inclusive) {
        // 防抖：禁止两段步进同时跑
        if (!stepping.compareAndSet(false, true)) {
            return;
        }

        try {
            operateExecutor.execute(() -> {
                try {
                    while (true) {
                        WaitingEntry head = waiting.peek();

                        // 队列已空，或者队首已经越过目标（被别的线程推进过），就收手
                        if (head == null || (inclusive ? head.id > target.id : head.id >= target.id)) {
                            break;
                        }

                        timedTickBlock(false);
                    }
                } finally {
                    stepping.set(false);
                    showWaitingList();
                }
            });
        } catch (RejectedExecutionException x) {
            stepping.set(false);
            Slimefun.logger().log(Level.SEVERE, x, () -> "Operate Executor 不可用，无法步进");
        }
    }

    private int clearWaitingEntries() {
        int discarded = waiting.size();
        waiting.clear();
        return discarded;
    }

    /**
     * 无论是否 tickFreeze，都会调用到此方法
     */
    private void timedTickBlock(WaitingEntry entry) {
        boolean profiling = Slimefun.getProfiler().isProfiling();
        timedTickBlock(entry, 10, TimeUnit.SECONDS, profiling); // default timeout
    }

    @ParametersAreNonnullByDefault
    private void timedTickBlock(WaitingEntry entry, long timeout, TimeUnit timeUnit, boolean profiling) {
        if (entry.data.isPendingRemove()) return; // waiting 期间机器可能会被拆除

        Location l = entry.location;
        SlimefunItem item = entry.item;

        if (entry.isSync()) {
            Slimefun.runSync(() -> {
                long timestamp = 0;
                try {
                    if (profiling) {
                        Slimefun.getProfiler().scheduleEntries(1);
                        timestamp = System.nanoTime();
                    }

                    // Bukkit 自带的 Watchdog 会检测超时，不需要我们处理
                    tickBlock(entry);
                } catch (Exception | LinkageError x) {
                    reportErrors(l, item, x);
                } finally {
                    if (profiling) {
                        Slimefun.getProfiler().closeEntry(l, item, timestamp);
                    }
                }
            });
        } else {
            long timestamp = profiling ? Slimefun.getProfiler().newEntry() : 0;

            try {
                CompletableFuture.runAsync(() -> tickBlock(entry), tickExecutor).get(timeout, timeUnit);
            } catch (TimeoutException e) {
                reportTimeout(l, item, timeout, timeUnit, e);
            } catch (Exception | LinkageError x) {
                reportErrors(l, item, x);
            } finally {
                if (profiling) {
                    Slimefun.getProfiler().closeEntry(l, item, timestamp);
                }
            }
        }
    }

    @Internal
    @ParametersAreNonnullByDefault
    private void tickBlock(WaitingEntry entry) {
        Location l = entry.location;
        SlimefunItem item = entry.item;
        ASlimefunDataContainer data = entry.data;
        if (item.getBlockTicker().isUniversal()) {
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
    }

    @ParametersAreNonnullByDefault
    private void timedTickBlock(WaitingEntry entry, long timeout, TimeUnit timeUnit) {
        if (entry.data.isPendingRemove()) return; // waiting 期间机器可能会被拆除

        Location l = entry.location;
        SlimefunItem item = entry.item;

        try {
            if (entry.isSync()) {
                // Bukkit 自带的 Watchdog 会检测超时，不需要我们处理
                tickBlock(entry);
            } else {
                CompletableFuture.runAsync(
                                () -> {
                                    tickBlock(entry);
                                },
                                tickExecutor)
                        .get(timeout, timeUnit);
            }
        } catch (TimeoutException e) {
            reportTimeout(l, item, timeout, timeUnit, e);
        } catch (Exception | LinkageError x) {
            reportErrors(l, item, x);
        }
    }

    public static String getChineseTimeUnit(TimeUnit unit) {
        return switch (unit) {
            case NANOSECONDS -> "纳秒";
            case MICROSECONDS -> "微秒";
            case MILLISECONDS -> "毫秒";
            case SECONDS -> "秒";
            case MINUTES -> "分钟";
            case HOURS -> "小时";
            case DAYS -> "天";
        };
    }

    private void reportTimeout(Location l, SlimefunItem item, long timeout, TimeUnit timeUnit, TimeoutException e) {
        Slimefun.logger().log(Level.SEVERE, "World: {0} X: {1} Y: {2} Z: {3} ({4})", new Object[] {
            l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ(), item.getId()
        });
        Slimefun.logger()
                .log(
                        Level.SEVERE,
                        "该方块对应的机器在上一个 Tick 中运行超过 " + timeout + getChineseTimeUnit(timeUnit) + "，可能会导致粘液刻严重被拖慢！");
        reportErrors(l, item, e);
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

    public void halt() {
        halted = true;
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
            // 顺手清掉被搬空的 Set
            var iterator = tickingLocations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ChunkPosition, Set<TickLocation>> entry = iterator.next();
                entry.getValue().removeIf(tk -> uuid.equals(tk.getUuid()));

                if (entry.getValue().isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    public void showWaitingList() {
        showWaitingList(1);
    }

    public void showWaitingList(int page) {
        var builder = Component.text()
                .color(TextColor.color(0xFFD700))
                .append(Component.text("===== Ticker 等待列表 ====="))
                .appendNewline();

        int j = 0;
        for (var entry :
                waiting.stream().skip((page - 1) * PAGE_SIZE).limit(PAGE_SIZE).toList()) {
            int number = (page - 1) * PAGE_SIZE + j + 1;
            String head = number + ". " + entry.item.getItemName() + " ";
            builder.color(TextColor.color(0x00B7B7))
                    .append(Component.text(head)
                            .hoverEvent(
                                    Component.text("点击步过").clickEvent(ClickEvent.callback(p2 -> stepTo(entry, true)))))
                    .append(Component.text(" ".repeat(Math.max(0, 12 - head.length()))))
                    .append(Component.text("[运行到] ")
                            .hoverEvent(Component.text("点击运行到此并停止"))
                            .clickEvent(ClickEvent.callback(p2 -> stepTo(entry, false))))
                    .append(Component.text("[步过] ")
                            .hoverEvent(Component.text("点击步过"))
                            .clickEvent(ClickEvent.callback(p2 -> stepTo(entry, true))))
                    .append(Component.text("[高亮] ")
                            .hoverEvent(Component.text("点击高亮方块"))
                            .clickEvent(ClickEvent.callback(p2 -> {
                                if (p2 instanceof Player p) {
                                    if (p.getLocation().getWorld()
                                            == entry.getLocation().getWorld()) {
                                        ParticleUtil.highlightBlock(p, entry.getLocation(), 3);
                                    } else {
                                        Slimefun.getLocalization().sendMessage(p, "messages.wrong-world");
                                    }
                                }
                            })))
                    .appendNewline();
            j++;
        }
        int totalPage = (waiting.size() - 1) / PAGE_SIZE + 1;
        builder.color(TextColor.color(0xFFD700))
                .append(Component.text()
                        .append(Component.text("=== 上一页 < ")
                                .hoverEvent(Component.text("点击跳转到上一页 (" + (page - 1) + ")"))
                                .clickEvent(ClickEvent.callback(p2 -> {
                                    if (page - 1 < 1) {
                                        return;
                                    }
                                    showWaitingList(page - 1);
                                })))
                        .append(Component.text(page + " / " + totalPage)
                                .append(Component.text(" > 下一页 ===")
                                        .hoverEvent(Component.text("点击跳转到下一页 (" + (page + 1) + ")"))
                                        .clickEvent(ClickEvent.callback(p2 -> {
                                            if (page + 1 > totalPage) {
                                                return;
                                            }
                                            showWaitingList(page + 1);
                                        })))));

        Component text = builder.build();
        Bukkit.getServer().getOnlinePlayers().stream().filter(Player::isOp).forEach(player -> {
            player.sendMessage(text);
        });
    }
}
