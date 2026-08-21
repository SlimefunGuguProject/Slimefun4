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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    @Setter
    private int tickRate;

    private boolean halted = false;
    private boolean running = false;

    @Setter
    private volatile boolean paused = false;

    private final Deque<WaitingEntry> waiting = new ConcurrentLinkedDeque<>();

    private final int PAGE_SIZE = 10;

    @Setter
    private volatile boolean tickFreeze = false;

    @Setter
    private volatile Predicate<WaitingEntry> tickFreezePredicate = entry -> false;

    @Data
    public static class WaitingEntry {
        private static final AtomicLong ID = new AtomicLong(0);
        private final Location location;
        private final SlimefunItem item;
        private final ASlimefunDataContainer data;
        private final long timestamp;
        private final boolean sync;
        private final long id;

        public WaitingEntry(
                Location location, SlimefunItem item, ASlimefunDataContainer data, long timestamp, boolean sync) {
            this.location = location;
            this.item = item;
            this.data = data;
            this.timestamp = timestamp;
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
        if (paused) {
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

        int length = waiting.size();
        for (int i = 0; i < length; i++) {
            timedTickBlock();
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

        if (item != null && item.getBlockTicker() != null) {
            if (item.isDisabledIn(l.getWorld())) {
                return;
            }

            try {
                if (item.getBlockTicker().isSynchronized()) {
                    Slimefun.getProfiler().scheduleEntries(1);
                    item.getBlockTicker().update();

                    timedTickBlock(l, item, blockData, System.nanoTime(), true);
                } else {
                    long timestamp = Slimefun.getProfiler().newEntry();
                    item.getBlockTicker().update();
                    timedTickBlock(l, item, blockData, timestamp, false);
                }

                tickers.add(item.getBlockTicker());
            } catch (Exception x) {
                reportErrors(l, item, x);
            }
        }
    }

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

                    /**
                     * We are inserting a new timestamp because synchronized actions
                     * are always ran with a 50ms delay (1 game tick)
                     */
                    Slimefun.runSync(() -> {
                        if (data.isPendingRemove()) {
                            return;
                        }
                        timedTickBlock(l, item, data, System.nanoTime(), true);
                    });
                } else {
                    long timestamp = Slimefun.getProfiler().newEntry();
                    item.getBlockTicker().update();
                    timedTickBlock(l, item, data, timestamp, false);
                }

                tickers.add(item.getBlockTicker());
            } catch (Exception x) {
                reportErrors(l, item, x);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void timedTickBlock(Location l, SlimefunItem item, ASlimefunDataContainer data, long timestamp) {
        timedTickBlock(l, item, data, timestamp, true); // fallback
    }

    @ParametersAreNonnullByDefault
    private void timedTickBlock(
            Location l, SlimefunItem item, ASlimefunDataContainer data, long timestamp, boolean sync) {
        var entry = new WaitingEntry(l, item, data, timestamp, sync);
        waiting.add(entry);
        if (tickFreezePredicate.test(entry)) {
            tickFreeze = true;
        }
        if (!tickFreeze) {
            timedTickBlock();
        }
    }

    private void timedTickBlock() {
        WaitingEntry entry = waiting.poll();
        if (entry == null) {
            return;
        }

        if (entry.isSync()) {
            /**
             * We are inserting a new timestamp because synchronized actions
             * are always ran with a 50ms delay (1 game tick)
             */
            Slimefun.runSync(() -> {
                ASlimefunDataContainer blockData = entry.getData();
                if (blockData.isPendingRemove()) {
                    return;
                }
                timedTickBlock(entry);
            });
        } else {
            timedTickBlock(entry);
        }
    }

    private void timedTickBlock(WaitingEntry entry) {
        timedTickBlock(entry, 10, TimeUnit.SECONDS); // default timeout
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
        long timestamp = entry.timestamp;
        try {
            if (entry.isSync()) {
                // Bukkit 自带的 Watchdog 会检测超时，不需要我们处理
                tickBlock(entry);
            } else {
                CompletableFuture.runAsync(() -> {
                            tickBlock(entry);
                        })
                        .get(timeout, timeUnit);
            }
        } catch (TimeoutException e) {
            reportTimeout(l, item, timeout, timeUnit, e);
        } catch (Exception | LinkageError x) {
            reportErrors(l, item, x);
        } finally {
            Slimefun.getProfiler().closeEntry(l, item, timestamp);
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
                    .append(Component.text()
                            .append(Component.text(head))
                            .hoverEvent(Component.text("点击步过").clickEvent(ClickEvent.callback(p2 -> {
                                while (!waiting.isEmpty() && waiting.peek().id <= entry.id) {
                                    timedTickBlock();
                                }
                                showWaitingList();
                            }))))
                    .append(Component.text(" ".repeat(Math.max(0, 12 - head.length()))))
                    .append(Component.text("[运行到] ")
                            .hoverEvent(Component.text("点击运行到此并停止"))
                            .clickEvent(ClickEvent.callback(p2 -> {
                                while (!waiting.isEmpty() && waiting.peek().id < entry.id) {
                                    timedTickBlock();
                                }
                                showWaitingList();
                            })))
                    .append(Component.text("[步过] ")
                            .hoverEvent(Component.text("点击步过"))
                            .clickEvent(ClickEvent.callback(p2 -> {
                                while (!waiting.isEmpty() && waiting.peek().id <= entry.id) {
                                    timedTickBlock();
                                }
                                showWaitingList();
                            })))
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
