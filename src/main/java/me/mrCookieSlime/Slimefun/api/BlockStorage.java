package me.mrCookieSlime.Slimefun.api;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataConfigWrapper;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunChunkData;
import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

// This class really needs a VERY big overhaul

/**
 * Deprecated: use {@link com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController} instead
 */
@Deprecated
@SlimefunAPI
public class BlockStorage {

    private static final Config emptyData = new EmptyBlockData();

    @Nullable public static BlockStorage getStorage(@Nonnull World world) {
        return null;
    }

    public static void store(Block block, ItemStack item) {
        SlimefunItem sfitem = SlimefunItem.getByItem(item);

        if (sfitem != null) {
            Slimefun.getDatabaseManager().getBlockDataController().createBlock(block.getLocation(), sfitem.getId());
        }
    }

    public static void store(Block block, String item) {
        Slimefun.getDatabaseManager().getBlockDataController().createBlock(block.getLocation(), item);
    }

    /**
     * Retrieves the SlimefunItem's ItemStack from the specified Block.
     * If the specified Block is registered in BlockStorage,
     * its data will be erased from it, regardless of the returned value.
     *
     * @param block
     *            the block to retrieve the ItemStack from
     *
     * @return the SlimefunItem's ItemStack corresponding to the block if it has one, otherwise null
     */
    @Nullable public static ItemStack retrieve(@Nonnull Block block) {
        SlimefunItem item = check(block);

        if (item == null) {
            return null;
        } else {
            clearBlockInfo(block);
            return item.getItem();
        }
    }

    private static SlimefunBlockData getBlockData(Location l) {
        var re = Slimefun.getDatabaseManager().getBlockDataController().getBlockData(l);
        if (re == null) {
            return null;
        }

        if (!re.isDataLoaded()) {
            Slimefun.getDatabaseManager().getBlockDataController().loadBlockData(re);
        }
        return re;
    }

    public static String getLocationInfo(Location l, String key) {
        var data = getBlockData(l);
        return data == null ? null : "id".equals(key) ? data.getSfId() : data.getData(key);
    }

    public static void addBlockInfo(Location l, String key, String value) {
        addBlockInfo(l, key, value, false);
    }

    public static void addBlockInfo(Block block, String key, String value) {
        addBlockInfo(block.getLocation(), key, value);
    }

    public static void addBlockInfo(Block block, String key, String value, boolean updateTicker) {
        addBlockInfo(block.getLocation(), key, value, updateTicker);
    }

    public static void addBlockInfo(Location l, String key, String value, boolean updateTicker) {
        if ("id".equals(key)) {
            Slimefun.getDatabaseManager().getBlockDataController().createBlock(l, value);
            return;
        }
        var data = getBlockData(l);
        if (data != null) {
            if (value == null) {
                data.removeData(key);
            } else {
                data.setData(key, value);
            }
        }
    }

    public static boolean hasBlockInfo(Block block) {
        return hasBlockInfo(block.getLocation());
    }

    public static boolean hasBlockInfo(Location l) {
        return getBlockData(l) != null;
    }

    public static void clearBlockInfo(Block block) {
        clearBlockInfo(block.getLocation());
    }

    public static void clearBlockInfo(Location l) {
        clearBlockInfo(l, true);
    }

    public static void clearBlockInfo(Block b, boolean destroy) {
        clearBlockInfo(b.getLocation(), destroy);
    }

    public static void clearBlockInfo(Location l, boolean destroy) {
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(l);
    }

    @Nullable public static SlimefunItem check(@Nonnull Block b) {
        String id = checkID(b);
        return id == null ? null : SlimefunItem.getById(id);
    }

    @Nullable public static SlimefunItem check(@Nonnull Location l) {
        String id = checkID(l);
        return id == null ? null : SlimefunItem.getById(id);
    }

    public static boolean check(Block block, String slimefunItem) {
        String id = checkID(block);
        return id != null && id.equals(slimefunItem);
    }

    @Nullable public static String checkID(@Nonnull Block b) {
        return checkID(b.getLocation());
    }

    @Nullable public static String checkID(@Nonnull Location l) {
        return getLocationInfo(l, "id");
    }

    public static boolean check(@Nonnull Location l, @Nullable String slimefunItem) {
        if (slimefunItem == null) {
            return false;
        }

        String id = checkID(l);
        return id != null && id.equals(slimefunItem);
    }

    public static BlockMenu getInventory(Block b) {
        return getInventory(b.getLocation());
    }

    public static boolean hasInventory(Block b) {
        var data = getBlockData(b.getLocation());
        return data != null && data.getBlockMenu() != null;
    }

    public static BlockMenu getInventory(Location l) {
        var data = getBlockData(l);
        return data == null ? null : data.getBlockMenu();
    }

    private static SlimefunChunkData getChunkData(Chunk c) {
        return Slimefun.getDatabaseManager().getBlockDataController().getChunkData(c);
    }

    public static void setChunkInfo(World world, int x, int z, String key, String value) {
        getChunkData(world.getChunkAt(x, z)).setData(key, value);
    }

    public static boolean hasChunkInfo(World world, int x, int z) {
        return !getChunkData(world.getChunkAt(x, z)).getAllData().isEmpty();
    }

    public static String getChunkInfo(World world, int x, int z, String key) {
        return getChunkData(world.getChunkAt(x, z)).getData(key);
    }

    public static Config getLocationInfo(Location location) {
        var re = getBlockData(location);
        return re == null ? emptyData : new BlockDataConfigWrapper(re);
    }

    public static void deleteLocationInfoUnsafely(Location l, boolean destroy) {
        var blockData = getBlockData(l);

        if (blockData == null) {
            return;
        }

        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(l);
    }
}
