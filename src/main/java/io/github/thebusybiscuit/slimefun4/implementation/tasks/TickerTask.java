package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalBlock;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.bakedlibs.dough.blocks.ChunkPosition;
import io.github.thebusybiscuit.slimefun4.api.ErrorReport;
import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunInternal;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.services.scheduling.TaskHandle;
import io.github.thebusybiscuit.slimefun4.core.services.stability.MachineCircuitBreaker;
import io.github.thebusybiscuit.slimefun4.core.ticker.TickLocation;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
 * The {@link TickerTask} is responsible for ticking every {@link BlockTicker}, synchronous or not.
 *
 * <p>Paper retains the historical split between asynchronous and synchronized tickers. On Folia, every block tick is
 * dispatched to the region that owns its chunk. The asynchronous coordinator only snapshots registrations and queues
 * region work; it never reads or mutates Bukkit world state.
 *
 * @author TheBusyBiscuit
 *
 * @see BlockTicker
 */
@SlimefunInternal
public class TickerTask implements Runnable {

    /**
     * This Map holds all currently actively ticking locations.
     * The value of this map (Set entries) MUST be thread-safe and mutable.
     */
    private final Map<ChunkPosition, Set<TickLocation>> tickingLocations = new ConcurrentHashMap<>();

    /**
     * This Map tracks how many bugs have occurred in a given Location.
     */
    private final Map<BlockPosition, Integer> bugs = new ConcurrentHashMap<>();

    /**
     * Locations whose {@link me.mrCookieSlime.Slimefun.api.inventory.BlockMenu} is currently being viewed.
     */
    private final Set<BlockPosition> viewedInventories = ConcurrentHashMap.newKeySet();
    private final Set<BlockPosition> queuedSynchronousTicks = ConcurrentHashMap.newKeySet();
    private final Map<BlockTicker, Object> foliaTickerLocks = new ConcurrentHashMap<>();
    private final MachineCircuitBreaker<BlockPosition> circuitBreaker = new MachineCircuitBreaker<>();

    private static final int CIRCUIT_FAILURE_THRESHOLD = 4;

    private final AtomicBoolean running = new AtomicBoolean();
    private int tickRate;
    private TaskHandle scheduledTask;
    private volatile boolean halted;

    @Setter
    private volatile boolean paused;

    /**
     * Starts the asynchronous coordinator. On Folia the coordinator only schedules location-owned work.
     *
     * @param plugin the Slimefun plugin instance
     */
    public void start(@Nonnull Slimefun plugin) {
        this.tickRate = Slimefun.getCfg().getInt("URID.custom-ticker-delay");

        if (scheduledTask != null) {
            scheduledTask.cancel();
        }

        scheduledTask = Slimefun.getSchedulerService().runAsyncAtFixedRate(this, 100L, tickRate);
    }

    @Override
    public void run() {
        if (paused || halted || !running.compareAndSet(false, true)) {
            return;
        }

        try {
            Slimefun.getProfiler().start();
            Set<Map.Entry<ChunkPosition, Set<TickLocation>>> snapshot = snapshotTickingLocations();

            if (Slimefun.getSchedulerService().isFolia()) {
                runFoliaCycle(snapshot);
            } else {
                runPaperCycle(snapshot);
            }
        } catch (Exception | LinkageError throwable) {
            failCycle(throwable);
        }
    }

    private Set<Map.Entry<ChunkPosition, Set<TickLocation>>> snapshotTickingLocations() {
        Set<Map.Entry<ChunkPosition, Set<TickLocation>>> snapshot = new HashSet<>();

        synchronized (tickingLocations) {
            for (Map.Entry<ChunkPosition, Set<TickLocation>> entry : tickingLocations.entrySet()) {
                snapshot.add(Map.entry(entry.getKey(), new HashSet<>(entry.getValue())));
            }
        }

        return snapshot;
    }

    private void runPaperCycle(Set<Map.Entry<ChunkPosition, Set<TickLocation>>> snapshot) {
        Set<BlockTicker> tickers = new HashSet<>();

        for (Map.Entry<ChunkPosition, Set<TickLocation>> entry : snapshot) {
            tickChunk(entry.getKey(), tickers, entry.getValue(), false);
        }

        finishCycle(tickers);
    }

    private void runFoliaCycle(Set<Map.Entry<ChunkPosition, Set<TickLocation>>> snapshot) {
        TickCycle cycle = new TickCycle(snapshot.size());
        if (snapshot.isEmpty()) {
            cycle.finish();
            return;
        }

        for (Map.Entry<ChunkPosition, Set<TickLocation>> entry : snapshot) {
            Set<TickLocation> locations = entry.getValue();
            if (locations.isEmpty()) {
                cycle.completeChunk();
                continue;
            }

            Location anchor = locations.iterator().next().getLocation();
            try {
                TaskHandle handle = Slimefun.getSchedulerService().runAt(anchor, () -> {
                    try {
                        tickChunk(entry.getKey(), cycle.tickers, locations, true);
                    } finally {
                        cycle.completeChunk();
                    }
                });

                if (handle.isCancelled()) {
                    cycle.completeChunk();
                }
            } catch (RuntimeException | LinkageError throwable) {
                Slimefun.logger().log(Level.SEVERE, "Failed to schedule a Folia-owned machine chunk tick.", throwable);
                cycle.completeChunk();
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void tickChunk(
            ChunkPosition chunk, Set<BlockTicker> tickers, Set<TickLocation> locations, boolean regionOwned) {
        try {
            if (locations.isEmpty()) {
                return;
            }

            Location anchor = locations.iterator().next().getLocation();
            if (regionOwned && !Slimefun.getSchedulerService().isOwnedByCurrentRegion(anchor)) {
                Slimefun.logger().log(
                        Level.SEVERE,
                        "Skipped a machine chunk tick because Folia ownership was not held for {0}.",
                        new BlockPosition(anchor));
                return;
            }

            // On Folia this check executes on the owning region. Paper preserves the legacy asynchronous check.
            if (chunk.isLoaded()) {
                for (TickLocation tickLocation : locations) {
                    Location location = tickLocation.getLocation();
                    if (tickLocation.isUniversal()) {
                        tickUniversalLocation(tickLocation.getUuid(), location, tickers, regionOwned);
                    } else {
                        tickLocation(tickers, location, regionOwned);
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
            Slimefun.logger().log(
                    Level.SEVERE,
                    exception,
                    () -> "An Exception has occurred while trying to resolve Chunk: " + chunk);
        }
    }

    private void tickLocation(@Nonnull Set<BlockTicker> tickers, @Nonnull Location location, boolean regionOwned) {
        BlockPosition position = new BlockPosition(location);
        var data = StorageCacheUtils.getBlock(location);
        if (data == null || !data.isDataLoaded() || data.isPendingRemove()) {
            clearFailureState(position);
            return;
        }

        tickData(tickers, location, position, data, regionOwned);
    }

    @ParametersAreNonnullByDefault
    private void tickUniversalLocation(
            UUID uuid, Location location, Set<BlockTicker> tickers, boolean regionOwned) {
        BlockPosition position = new BlockPosition(location);
        var data = StorageCacheUtils.getUniversalBlock(uuid);
        if (data == null || !data.isDataLoaded() || data.isPendingRemove()) {
            clearFailureState(position);
            return;
        }

        tickData(tickers, location, position, data, regionOwned);
    }

    @ParametersAreNonnullByDefault
    private void tickData(
            Set<BlockTicker> tickers,
            Location location,
            BlockPosition position,
            ASlimefunDataContainer data,
            boolean regionOwned) {
        SlimefunItem item = SlimefunItem.getById(data.getSfId());
        if (item == null || item.getBlockTicker() == null || item.isDisabledIn(location.getWorld())) {
            return;
        }
        if (!canAttemptTick(position)) {
            return;
        }

        BlockTicker ticker = item.getBlockTicker();
        boolean owedProfilerEntry = false;

        try {
            if (regionOwned) {
                if (!queuedSynchronousTicks.add(position)) {
                    return;
                }

                try {
                    // Addons commonly keep mutable state on a shared BlockTicker instance. Paper's historical
                    // coordinator effectively serialized those callbacks; retain that guarantee across Folia regions.
                    synchronized (foliaTickerLocks.computeIfAbsent(ticker, ignored -> new Object())) {
                        ticker.update();
                        long timestamp = Slimefun.getProfiler().newEntry();
                        owedProfilerEntry = timestamp != 0;
                        if (tickBlock(location, item, data, timestamp)) {
                            markTickSuccess(position);
                        }
                        owedProfilerEntry = false;
                    }
                } finally {
                    queuedSynchronousTicks.remove(position);
                }
            } else if (ticker.isSynchronized() || isInventoryViewed(location)) {
                if (!queuedSynchronousTicks.add(position)) {
                    return;
                }

                boolean profilerScheduled = Slimefun.getProfiler().isProfiling();
                if (profilerScheduled) {
                    Slimefun.getProfiler().scheduleEntries(1);
                    owedProfilerEntry = true;
                }
                ticker.update();

                Slimefun.getSchedulerService().runAt(location, () -> {
                    try {
                        if (!data.isDataLoaded() || data.isPendingRemove()) {
                            clearFailureState(position);
                            if (profilerScheduled) {
                                Slimefun.getProfiler().cancelScheduledEntry();
                            }
                            return;
                        }

                        long timestamp = profilerScheduled ? System.nanoTime() : 0L;
                        if (tickBlock(location, item, data, timestamp)) {
                            markTickSuccess(position);
                        }
                    } finally {
                        queuedSynchronousTicks.remove(position);
                    }
                });
                owedProfilerEntry = false;
            } else {
                long timestamp = Slimefun.getProfiler().newEntry();
                owedProfilerEntry = timestamp != 0;
                ticker.update();
                if (tickBlock(location, item, data, timestamp)) {
                    markTickSuccess(position);
                }
                owedProfilerEntry = false;
            }

            tickers.add(ticker);
        } catch (Exception | LinkageError throwable) {
            queuedSynchronousTicks.remove(position);
            if (owedProfilerEntry) {
                Slimefun.getProfiler().cancelScheduledEntry();
            }
            reportErrors(location, item, throwable);
        }
    }

    private void clearFailureState(BlockPosition position) {
        circuitBreaker.clear(position);
        bugs.remove(position);
    }

    @ParametersAreNonnullByDefault
    private boolean tickBlock(Location location, SlimefunItem item, ASlimefunDataContainer data, long timestamp) {
        try {
            item.getBlockTicker().tick(location.getBlock(), item, data);
            return true;
        } catch (Exception | LinkageError throwable) {
            reportErrors(location, item, throwable);
            return false;
        } finally {
            Slimefun.getProfiler().closeEntry(location, item, timestamp);
        }
    }

    private void failCycle(Throwable throwable) {
        Slimefun.logger().log(
                Level.SEVERE,
                throwable,
                () -> "An Exception was caught while ticking the Block Tickers Task for Slimefun v"
                        + Slimefun.getVersion());
        finishCycle(Collections.emptySet());
    }

    private void finishCycle(Set<BlockTicker> tickers) {
        for (BlockTicker ticker : tickers) {
            try {
                ticker.startNewTick();
            } catch (RuntimeException | LinkageError throwable) {
                Slimefun.logger().log(Level.SEVERE, "A BlockTicker failed while starting a new tick cycle.", throwable);
            }
        }

        running.set(false);
        if (Slimefun.getProfiler().isProfiling()) {
            Slimefun.getProfiler().stop();
        }
    }

    private final class TickCycle {

        private final Set<BlockTicker> tickers = ConcurrentHashMap.newKeySet();
        private final AtomicInteger remainingChunks;
        private final AtomicBoolean finished = new AtomicBoolean();

        private TickCycle(int chunkCount) {
            remainingChunks = new AtomicInteger(chunkCount);
        }

        private void completeChunk() {
            if (remainingChunks.decrementAndGet() == 0) {
                finish();
            }
        }

        private void finish() {
            if (finished.compareAndSet(false, true)) {
                finishCycle(tickers);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void reportErrors(Location l, SlimefunItem item, Throwable x) {
        BlockPosition position = new BlockPosition(l);
        if (circuitBreaker.isOpen(position)) {
            long cooldownSeconds = getCircuitCooldownSeconds();
            circuitBreaker.open(position, System.currentTimeMillis() + cooldownSeconds * 1000L);
            bugs.remove(position);
            Slimefun.logger().log(Level.SEVERE,
                    "The retry for machine {0} at {1}, {2}, {3} failed; its circuit has been reopened for {4} seconds.",
                    new Object[] {item.getId(), l.getBlockX(), l.getBlockY(), l.getBlockZ(), cooldownSeconds});
            return;
        }

        int errors = bugs.merge(position, 1, Integer::sum);

        if (errors == 1) {
            new ErrorReport<>(x, l, item);
        }

        if (errors >= CIRCUIT_FAILURE_THRESHOLD) {
            long cooldownSeconds = getCircuitCooldownSeconds();
            circuitBreaker.open(position, System.currentTimeMillis() + cooldownSeconds * 1000L);
            bugs.remove(position);

            Slimefun.logger().log(Level.SEVERE, "X: {0} Y: {1} Z: {2} ({3})", new Object[] {
                l.getBlockX(), l.getBlockY(), l.getBlockZ(), item.getId()
            });
            Slimefun.logger().log(Level.SEVERE,
                    "This machine failed {0} consecutive ticks and has been paused for {1} seconds.",
                    new Object[] {CIRCUIT_FAILURE_THRESHOLD, cooldownSeconds});
            Slimefun.logger().log(Level.SEVERE,
                    "It will be retried automatically. The ticker registration and stored machine data were preserved.");
        }
    }

    private long getCircuitCooldownSeconds() {
        int configured = Slimefun.getCfg().getInt("stability.machine-circuit-breaker-cooldown-seconds");
        return configured > 0 ? Math.max(30L, configured) : 300L;
    }

    private boolean canAttemptTick(BlockPosition position) {
        return circuitBreaker.canAttempt(position, System.currentTimeMillis());
    }

    private void markTickSuccess(BlockPosition position) {
        bugs.remove(position);
        circuitBreaker.clear(position);
    }

    public boolean retryMachine(@Nonnull Location location) {
        BlockPosition position = new BlockPosition(location);
        bugs.remove(position);
        queuedSynchronousTicks.remove(position);
        return circuitBreaker.clear(position);
    }

    public int retryAllMachines() {
        int count = circuitBreaker.clearAll();
        bugs.clear();
        return count;
    }

    public int getPausedMachineCount() {
        return circuitBreaker.size();
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isHalted() {
        return halted;
    }

    public void halt() {
        halted = true;

        if (scheduledTask != null) {
            scheduledTask.cancel();
            scheduledTask = null;
        }
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

        BlockPosition position = new BlockPosition(l);
        viewedInventories.remove(position);
        queuedSynchronousTicks.remove(position);
        circuitBreaker.clear(position);
        bugs.remove(position);

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

    /**
     * Marks a machine inventory as viewed or unviewed.
     *
     * @param location
     *            The machine location
     * @param viewed
     *            Whether at least one player is viewing it
     */
    public void setInventoryViewed(@Nonnull Location location, boolean viewed) {
        Validate.notNull(location, "Location cannot be null!");

        BlockPosition position = new BlockPosition(location);
        if (viewed) {
            viewedInventories.add(position);
        } else {
            viewedInventories.remove(position);
        }
    }

    /**
     * Returns whether the machine inventory at this location is currently open.
     * This is a cheap concurrent-set lookup and does not load block data.
     */
    public boolean isInventoryViewed(@Nonnull Location location) {
        Validate.notNull(location, "Location cannot be null!");
        return !viewedInventories.isEmpty() && viewedInventories.contains(new BlockPosition(location));
    }
}
