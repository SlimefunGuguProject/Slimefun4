package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import com.google.common.base.Preconditions;
import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ADataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.UniversalMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * Utils to access the cached block data.
 * It is safe to use when the target block is in a loaded chunk (such as in block events).
 * By default, please use
 * {@link com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController#getBlockData}
 */
public class StorageCacheUtils {
    private static final Set<ADataContainer> loadingData = new HashSet<>();

    @ParametersAreNonnullByDefault
    public static boolean hasBlock(Location l) {
        return getBlock(l) != null;
    }

    @ParametersAreNonnullByDefault
    public static boolean hasUniversalBlock(Location l) {
        return Slimefun.getBlockDataService().getUniversalDataUUID(l.getBlock()).isPresent();
    }

    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunBlockData getBlock(Location l) {
        return Slimefun.getDatabaseManager().getBlockDataController().getBlockDataFromCache(l);
    }

    @ParametersAreNonnullByDefault
    public static boolean isBlock(Location l, String id) {
        var blockData = getBlock(l);
        return blockData != null && id.equals(blockData.getSfId());
    }

    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunItem getSfItem(Location l) {
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
            Preconditions.checkNotNull(uni);
            uni.setData(key, val);
        }
    }

    @ParametersAreNonnullByDefault
    public static void removeData(Location loc, String key) {
        var block = getBlock(loc);
        if (block != null) {
            block.removeData(key);
        } else {
            var uni = getUniversalBlock(loc.getBlock());
            Preconditions.checkNotNull(uni);
            uni.removeData(key);
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
    @Nullable public static SlimefunUniversalData getUniversalBlock(UUID uuid) {
        var uniData = Slimefun.getDatabaseManager().getBlockDataController().getUniversalDataFromCache(uuid);

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
    @Nullable public static SlimefunUniversalData getUniversalBlock(UUID uuid, Location l) {
        var uniData = getUniversalBlock(uuid);

        if (uniData != null) {
            uniData.setLastPresent(l);
        }

        return uniData;
    }

    /**
     * Get universal data from block
     * <p>
     * You **must** call this method from sync!
     *
     * @param block {@link Block}
     * @return {@link SlimefunUniversalData}
     */
    @ParametersAreNonnullByDefault
    @Nullable public static SlimefunUniversalData getUniversalBlock(Block block) {
        var uuid = Slimefun.getBlockDataService().getUniversalDataUUID(block);

        return uuid.map(id -> getUniversalBlock(id, block.getLocation())).orElse(null);
    }

    /**
     * Get universal menu from block
     * <p>
     * You **must** call this method from sync!
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

        return uniData.getUniversalMenu();
    }

    @ParametersAreNonnullByDefault
    @Nullable public static UniversalMenu getUniversalMenu(UUID uuid, Location l) {
        var uniData = Slimefun.getDatabaseManager().getBlockDataController().getUniversalDataFromCache(uuid);

        if (uniData == null) {
            return null;
        }

        if (!uniData.isDataLoaded()) {
            requestLoad(uniData);
            return null;
        }

        uniData.setLastPresent(l);

        return uniData.getUniversalMenu();
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

    public static void requestLoad(ADataContainer data) {
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

        if (data instanceof SlimefunBlockData blockData) {
            Slimefun.getDatabaseManager()
                    .getBlockDataController()
                    .loadBlockDataAsync(blockData, new IAsyncReadCallback<>() {
                        @Override
                        public void onResult(SlimefunBlockData result) {
                            loadingData.remove(data);
                        }
                    });
        } else if (data instanceof SlimefunUniversalData uniData) {
            Slimefun.getDatabaseManager()
                    .getBlockDataController()
                    .loadUniversalDataAsync(uniData, new IAsyncReadCallback<>() {
                        @Override
                        public void onResult(SlimefunUniversalData result) {
                            loadingData.remove(data);
                        }
                    });
        }
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
