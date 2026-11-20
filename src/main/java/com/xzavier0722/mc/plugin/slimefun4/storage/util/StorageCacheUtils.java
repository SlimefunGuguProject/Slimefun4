package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import city.norain.slimefun4.api.menu.UniversalMenu;
import city.norain.slimefun4.utils.TaskUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ADataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * Utils to access the cached block data.
 * It is safe to use when the target block is in a loaded chunk (such as in block events).
 * By default, please use
 * {@link com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController#getBlockData}
 */
public class StorageCacheUtils {
    private static final Set<ADataContainer> loadingData = new CopyOnWriteArraySet<>();

    @ParametersAreNonnullByDefault
    public static boolean hasSlimefunBlock(Location l) {
        return hasBlock(l) || hasUniversalBlock(l);
    }

    @ParametersAreNonnullByDefault
    public static boolean hasBlock(Location l) {
        return getBlock(l) != null;
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUniversalBlock(Location l) {
        if (Slimefun.getDatabaseManager()
                .getBlockDataController()
                .getUniversalBlockDataFromCache(l)
                .isPresent()) {
            return true;
        }

        var block = l.getBlock();
        if (!Slimefun.getBlockDataService().isTileEntity(block.getType())) {
            return false;
        }

        return TaskUtil.runSyncMethod(
                () -> Slimefun.getBlockDataService().getUniversalDataUUID(block).isPresent());
    }

    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunBlockData getBlock(Location l) {
        return Slimefun.getDatabaseManager().getBlockDataController().getBlockDataFromCache(l);
    }

    @ParametersAreNonnullByDefault
    @Nullable public static ASlimefunDataContainer getDataContainer(Location l) {
        var blockData = getBlock(l);
        if (blockData != null) {
            return blockData;
        }

        return getUniversalBlock(l);
    }

    @ParametersAreNonnullByDefault
    public static boolean isBlock(Location l, String id) {
        var blockData = getBlock(l);
        return blockData != null && id.equals(blockData.getSfId());
    }

    /**
     * @deprecated use {@link #getSlimefunItem(Location)} instead
     */
    @Deprecated(forRemoval = true)
    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunItem getSfItem(Location l) {
        return getSlimefunItem(l);
    }

    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunItem getSlimefunItem(Location l) {
        var blockData = getBlock(l);

        if (blockData != null) {
            return SlimefunItem.getById(blockData.getSfId());
        } else {
            var universalData = getUniversalBlock(l.getBlock());
            return universalData == null ? null : SlimefunItem.getById(universalData.getSfId());
        }
    }

    @ParametersAreNonnullByDefault
    @Nullable public static String getData(Location loc, String key) {
        var blockData = getBlock(loc);

        if (blockData != null) {
            if (!blockData.isDataLoaded()) {
                requestLoad(blockData);
                return null;
            }
            return blockData.getData(key);
        } else {
            var uniData = getUniversalBlock(loc.getBlock());

            if (uniData == null) {
                return null;
            }

            if (!uniData.isDataLoaded()) {
                requestLoad(uniData);
                return null;
            }
            return uniData.getData(key);
        }
    }

    @ParametersAreNonnullByDefault
    @Nullable public static String getUniversalBlockData(UUID uuid, Location loc, String key) {
        var universalData = getUniversalBlock(uuid, loc);
        return universalData == null ? null : universalData.getData(key);
    }

    @ParametersAreNonnullByDefault
    public static void setData(Location loc, String key, String val) {
        var block = getBlock(loc);
        if (block != null) {
            if (!block.isDataLoaded()) {
                // The data container is not loaded yet (e.g. the chunk was just re-created
                // after an unload). Defer the write and re-bind it to the currently live
                // container once loading completes, so an eviction mid-load cannot drop it.
                replayBlockDataWrite(loc, key, val);
                return;
            }
            block.setData(key, val);
        } else {
            var uni = getUniversalBlock(loc.getBlock());

            if (uni != null) {
                if (!uni.isDataLoaded()) {
                    replayUniversalDataWrite(uni.getUUID(), key, val);
                    return;
                }
                uni.setData(key, val);
            }
        }
    }

    @ParametersAreNonnullByDefault
    public static void removeData(Location loc, String key) {
        var block = getBlock(loc);
        if (block != null) {
            if (!block.isDataLoaded()) {
                replayBlockDataWrite(loc, key, null);
                return;
            }
            block.removeData(key);
        } else {
            var uni = getUniversalBlock(loc.getBlock());

            if (uni != null) {
                if (!uni.isDataLoaded()) {
                    replayUniversalDataWrite(uni.getUUID(), key, null);
                    return;
                }
                uni.removeData(key);
            }
        }
    }

    /**
     * 把一次方块数据写入绑定到"当前存活"的容器实例上。
     * <p>
     * 若容器在加载完成前被区块驱逐, 旧实例永远不会变为已加载, 直接丢弃会导致
     * 玩家设置静默丢失。这里在每次回调时重新解析缓存中的存活实例并换绑。
     */
    private static void replayBlockDataWrite(@Nonnull Location loc, @Nonnull String key, @Nullable String val) {
        var target = getBlock(loc);
        if (target == null) {
            // 区块当前完全未加载, 没有可写入的容器; 写入丢弃。
            // 正常交互流程都会在区块加载后进行, 此分支仅在极端时序下发生。
            return;
        }

        if (target.isDataLoaded()) {
            writeBlockData(target, key, val);
        } else {
            executeAfterLoad(
                    target,
                    () -> {
                        if (target.isDataLoaded()) {
                            writeBlockData(target, key, val);
                        } else {
                            // 加载完成前又被驱逐: 换绑到当前存活实例, 而不是丢弃写入
                            replayBlockDataWrite(loc, key, val);
                        }
                    },
                    false);
        }
    }

    private static void writeBlockData(@Nonnull SlimefunBlockData data, @Nonnull String key, @Nullable String val) {
        if (val == null) {
            data.removeData(key);
        } else {
            data.setData(key, val);
        }
    }

    /**
     * 把一次通用数据写入绑定到当前缓存中的通用数据实例。
     * 通用数据不随区块驱逐, 但可能在写入时尚未加载完成, 同样需要换绑式回放。
     */
    private static void replayUniversalDataWrite(@Nonnull UUID uuid, @Nonnull String key, @Nullable String val) {
        var target = Slimefun.getDatabaseManager().getBlockDataController().getUniversalBlockDataFromCache(uuid);
        if (target == null) {
            // 通用数据当前不在缓存中, 没有可写入的容器
            return;
        }

        if (target.isDataLoaded()) {
            writeUniversalData(target, key, val);
        } else {
            executeAfterLoad(
                    target,
                    () -> {
                        if (target.isDataLoaded()) {
                            writeUniversalData(target, key, val);
                        } else {
                            replayUniversalDataWrite(uuid, key, val);
                        }
                    },
                    false);
        }
    }

    private static void writeUniversalData(
            @Nonnull SlimefunUniversalData data, @Nonnull String key, @Nullable String val) {
        if (val == null) {
            data.removeData(key);
        } else {
            data.setData(key, val);
        }
    }

    @ParametersAreNonnullByDefault
    @Nullable public static BlockMenu getMenu(Location loc) {
        var blockData = getBlock(loc);
        if (blockData == null) {
            return null;
        }

        if (!blockData.isDataLoaded()) {
            requestLoad(blockData);
            return null;
        }

        return blockData.getBlockMenu();
    }

    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunUniversalBlockData getUniversalBlock(UUID uuid) {
        var uniData = Slimefun.getDatabaseManager().getBlockDataController().getUniversalBlockDataFromCache(uuid);

        if (uniData == null) {
            return null;
        }

        if (!uniData.isDataLoaded()) {
            requestLoad(uniData);
            return null;
        }

        return uniData;
    }

    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunUniversalBlockData getUniversalBlock(UUID uuid, Location l) {
        return getUniversalBlock(uuid, l, true);
    }

    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunUniversalBlockData getUniversalBlock(UUID uuid, Location l, boolean updateLastPresent) {
        var uniData = getUniversalBlock(uuid);

        if (uniData != null && updateLastPresent) {
            uniData.setLastPresent(new BlockPosition(l));
        }

        return uniData;
    }

    /**
     * Get universal data from location
     *
     * @param location {@link Location}
     * @return {@link SlimefunUniversalBlockData}
     */
    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunUniversalBlockData getUniversalBlock(Location location) {
        return Slimefun.getDatabaseManager()
                .getBlockDataController()
                .getUniversalBlockDataFromCache(location)
                .orElse(null);
    }

    /**
     * Get universal data from block
     *
     * @param block {@link Block}
     * @return {@link SlimefunUniversalBlockData}
     */
    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunUniversalBlockData getUniversalBlock(Block block) {
        return getUniversalBlock(block.getLocation());
    }

    /**
     * Get universal menu from block
     *
     * @param block {@link Block}
     * @return {@link SlimefunUniversalData}
     */
    @ParametersAreNonnullByDefault
    @Nullable public static UniversalMenu getUniversalMenu(Block block) {
        var uniData = getUniversalBlock(block);

        if (uniData == null) {
            return null;
        }

        return uniData.getMenu();
    }

    @ParametersAreNonnullByDefault
    @Nullable public static UniversalMenu getUniversalMenu(UUID uuid, Location l) {
        var uniData = Slimefun.getDatabaseManager().getBlockDataController().getUniversalBlockDataFromCache(uuid);

        if (uniData == null) {
            return null;
        }

        if (!uniData.isDataLoaded()) {
            requestLoad(uniData);
            return null;
        }

        uniData.setLastPresent(new BlockPosition(l));

        return uniData.getMenu();
    }

    public static boolean isBlockPendingRemove(@Nonnull Block block) {
        var blockData = getBlock(block.getLocation());
        if (blockData != null) {
            return blockData.isPendingRemove();
        }

        var uniData = getUniversalBlock(block);
        return uniData != null && uniData.isPendingRemove();
    }

    public static void requestLoad(ASlimefunDataContainer data) {
        if (data instanceof SlimefunBlockData blockData) {
            requestLoad(blockData);
        } else if (data instanceof SlimefunUniversalData uniData) {
            requestLoad(uniData);
        }
    }

    public static void requestLoad(SlimefunBlockData data) {
        if (data.isDataLoaded()) {
            return;
        }

        if (loadingData.contains(data)) {
            return;
        }

        synchronized (loadingData) {
            if (loadingData.contains(data)) {
                return;
            }
            loadingData.add(data);
        }

        Slimefun.getDatabaseManager().getBlockDataController().loadBlockDataAsync(data, new IAsyncReadCallback<>() {
            @Override
            public void onResult(SlimefunBlockData result) {
                loadingData.remove(data);
            }
        });
    }

    public static void requestLoad(SlimefunUniversalBlockData data) {
        if (data.isDataLoaded()) {
            return;
        }

        if (loadingData.contains(data)) {
            return;
        }

        synchronized (loadingData) {
            if (loadingData.contains(data)) {
                return;
            }
            loadingData.add(data);
        }

        Slimefun.getDatabaseManager().getBlockDataController().loadUniversalDataAsync(data, new IAsyncReadCallback<>() {
            @Override
            public void onResult(SlimefunUniversalData result) {
                loadingData.remove(data);
            }
        });
    }

    public static void move(ASlimefunDataContainer data, Location to) {
        Slimefun.getDatabaseManager().getBlockDataController().move(data, to);
    }

    public static void executeAfterLoad(ASlimefunDataContainer data, Runnable execute, boolean runOnMainThread) {
        if (data instanceof SlimefunBlockData blockData) {
            executeAfterLoad(blockData, execute, runOnMainThread);
        } else if (data instanceof SlimefunUniversalData universalData) {
            executeAfterLoad(universalData, execute, runOnMainThread);
        }
    }

    public static void executeAfterLoad(SlimefunBlockData data, Runnable execute, boolean runOnMainThread) {
        if (data.isDataLoaded()) {
            execute.run();
            return;
        }

        Slimefun.getDatabaseManager().getBlockDataController().loadBlockDataAsync(data, new IAsyncReadCallback<>() {
            @Override
            public boolean runOnMainThread() {
                return runOnMainThread;
            }

            @Override
            public void onResult(SlimefunBlockData result) {
                execute.run();
            }
        });
    }

    public static void executeAfterLoad(SlimefunUniversalData data, Runnable execute, boolean runOnMainThread) {
        if (data.isDataLoaded()) {
            execute.run();
            return;
        }

        Slimefun.getDatabaseManager().getBlockDataController().loadUniversalDataAsync(data, new IAsyncReadCallback<>() {
            @Override
            public boolean runOnMainThread() {
                return runOnMainThread;
            }

            @Override
            public void onResult(SlimefunUniversalData result) {
                execute.run();
            }
        });
    }
}
