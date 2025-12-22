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
        var uniDataByNBT = TaskUtil.runSyncMethod(() -> Slimefun.getBlockDataService()
                .getUniversalDataUUID(l.getBlock())
                .isPresent());

        if (uniDataByNBT) {
            return true;
        }

        return Slimefun.getDatabaseManager()
                .getBlockDataController()
                .getUniversalBlockDataFromCache(l)
                .isPresent();
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
            return blockData.getData(key);
        } else {
            var uniData = getUniversalBlock(loc.getBlock());

            if (uniData == null) {
                return null;
            }

            return uniData.getData(key);
        }
    }

    @ParametersAreNonnullByDefault
    @Nullable public static String getUniversalBlock(UUID uuid, Location loc, String key) {
        var universalData = getUniversalBlock(uuid, loc);
        return universalData == null ? null : universalData.getData(key);
    }

    @ParametersAreNonnullByDefault
    public static void setData(Location loc, String key, String val) {
        var block = getBlock(loc);
        if (block != null) {
            block.setData(key, val);
        } else {
            var uni = getUniversalBlock(loc.getBlock());

            if (uni != null) {
                uni.setData(key, val);
            }
        }
    }

    @ParametersAreNonnullByDefault
    public static void removeData(Location loc, String key) {
        var block = getBlock(loc);
        if (block != null) {
            block.removeData(key);
        } else {
            var uni = getUniversalBlock(loc.getBlock());

            if (uni != null) {
                uni.removeData(key);
            }
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
        if (hasBlock(block.getLocation())) {
            return getBlock(block.getLocation()).isPendingRemove();
        }

        if (hasUniversalBlock(block.getLocation())) {
            return getUniversalBlock(block).isPendingRemove();
        }

        return false;
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
