package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.LocationUtils;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * {@link SlimefunChunkData} 是 Slimefun 中用于存储区块内所有方块数据的容器类。
 */
public class SlimefunChunkData extends ADataContainer {
    private static final SlimefunBlockData INVALID_BLOCK_DATA = new SlimefunBlockData(
            new Location(Bukkit.getWorlds().get(0), Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE),
            "INVALID_BLOCK_DATA_SF_KEY");
    private final UUID worldId;
    private final int chunkX;
    private final int chunkZ;
    private final Map<String, SlimefunBlockData> sfBlocks;
    /**
     * 是否曾经写入或加载过任何数据
     * <p>
     * 一旦置为 true 将不再回退。用于保证冲刷空区块缓存时,
     * 不会误卸载曾存在数据的区块, 避免与挂起的异步读写任务产生竞态。
     */
    private volatile boolean everHadData = false;

    @ParametersAreNonnullByDefault
    SlimefunChunkData(Chunk chunk) {
        super(LocationUtils.getChunkKey(chunk));
        worldId = chunk.getWorld().getUID();
        chunkX = chunk.getX();
        chunkZ = chunk.getZ();
        sfBlocks = new ConcurrentHashMap<>();
    }

    /**
     * Resolves this data container's world on demand without retaining it.
     *
     * @return the currently loaded world that owns this container
     * @throws IllegalStateException if the world that owns this container is no longer loaded
     */
    @Nonnull
    public World getWorld() {
        var world = Bukkit.getWorld(worldId);
        if (world == null) {
            throw new IllegalStateException("The world for chunk data " + getKey() + " is no longer loaded");
        }
        return world;
    }

    /**
     * Resolves this data container's chunk on demand. Calling this method may synchronously load the chunk, so it must
     * only be called from the server thread.
     *
     * @return the currently loaded world's chunk at this container's coordinates
     * @throws IllegalStateException if called asynchronously or if the owning world is no longer loaded
     */
    @Nonnull
    public Chunk getChunk() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Chunk data must be resolved from the server thread");
        }
        return getWorld().getChunkAt(chunkX, chunkZ);
    }

    @Nonnull
    @ParametersAreNonnullByDefault
    public SlimefunBlockData createBlockData(Location l, String sfId) {
        var lKey = LocationUtils.getLocKey(l);
        if (getBlockCacheInternal(lKey) != null) {
            throw new IllegalStateException("There already a block in this location: " + lKey);
        }
        var re = new SlimefunBlockData(l, sfId);
        re.setIsDataLoaded(true);
        sfBlocks.put(lKey, re);
        everHadData = true;

        var preset = BlockMenuPreset.getPreset(sfId);
        if (preset != null) {
            re.setBlockMenu(new BlockMenu(preset, l));
        }

        Slimefun.getDatabaseManager().getBlockDataController().saveNewBlock(l, sfId);

        return re;
    }

    @Nullable @ParametersAreNonnullByDefault
    public SlimefunBlockData getBlockData(Location l) {
        checkData();
        return getBlockCacheInternal(LocationUtils.getLocKey(l));
    }

    @Nullable @ParametersAreNonnullByDefault
    public SlimefunBlockData removeBlockData(Location l) {
        var lKey = LocationUtils.getLocKey(l);
        var re = removeBlockDataCacheInternal(lKey);
        if (re == null) {
            if (isDataLoaded()) {
                return null;
            }
            sfBlocks.put(lKey, INVALID_BLOCK_DATA);
        }
        Slimefun.getDatabaseManager().getBlockDataController().removeBlockDirectly(l);
        return re;
    }

    void addBlockCacheInternal(SlimefunBlockData data, boolean override) {
        if (override) {
            sfBlocks.put(data.getKey(), data);
        } else {
            sfBlocks.putIfAbsent(data.getKey(), data);
        }

        if (data != INVALID_BLOCK_DATA) {
            everHadData = true;
        }
    }

    SlimefunBlockData getBlockCacheInternal(String lKey) {
        var re = sfBlocks.get(lKey);
        return re == INVALID_BLOCK_DATA ? null : re;
    }

    Set<SlimefunBlockData> getAllCacheInternal() {
        var re = new HashSet<>(sfBlocks.values());
        re.removeIf(v -> v == INVALID_BLOCK_DATA);
        return re;
    }

    void removeAllCacheInternal() {
        sfBlocks.clear();
    }

    boolean hasBlockCache(String lKey) {
        return sfBlocks.containsKey(lKey);
    }

    SlimefunBlockData removeBlockDataCacheInternal(String lKey) {
        var re = isDataLoaded() ? sfBlocks.remove(lKey) : sfBlocks.put(lKey, INVALID_BLOCK_DATA);
        return re == INVALID_BLOCK_DATA ? null : re;
    }

    @ParametersAreNonnullByDefault
    public void setData(String key, String val) {
        checkData();
        setCacheInternal(key, val, true);
        Slimefun.getDatabaseManager().getBlockDataController().scheduleDelayedChunkDataUpdate(this, key);
    }

    @ParametersAreNonnullByDefault
    public void removeData(String key) {
        if (removeCacheInternal(key) != null) {
            Slimefun.getDatabaseManager().getBlockDataController().scheduleDelayedChunkDataUpdate(this, key);
        }
    }

    public Set<SlimefunBlockData> getAllBlockData() {
        return getAllCacheInternal();
    }

    /**
     * 判断该区块缓存是否从未有过任何数据。
     * 仅当区块数据已加载后, 该判断才是可信的。
     * <p>
     * 曾存在数据 (即使已被移除) 的区块不会被判定为空,
     * 以保证卸载冲刷不会影响可能仍挂起的异步任务。
     *
     * @return 区块数据已加载且从未存在过任何方块数据与区块数据时为 true
     */
    boolean isEmpty() {
        return isDataLoaded() && !everHadData && sfBlocks.isEmpty() && !hasCacheData();
    }

    @Override
    protected void setCacheInternal(String key, String val, boolean override) {
        super.setCacheInternal(key, val, override);
        if (val != null) {
            everHadData = true;
        }
    }

    @Override
    protected void setIsDataLoaded(boolean isDataLoaded) {
        super.setIsDataLoaded(isDataLoaded);
        if (isDataLoaded) {
            sfBlocks.entrySet().removeIf(entry -> entry.getValue() == INVALID_BLOCK_DATA);
        }
    }
}
