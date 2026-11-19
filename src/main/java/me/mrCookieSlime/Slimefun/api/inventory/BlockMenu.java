package me.mrCookieSlime.Slimefun.api.inventory;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.LocationUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

// This class will be deprecated, relocated and rewritten in a future version.
public class BlockMenu extends DirtyChestMenu {

    /**
     * 当前正被玩家打开的 {@link BlockMenu} 视图, 按区块键分组。
     * <p>
     * 菜单的虚拟容器独立于原版区块, 区块卸载时不会自动关闭。驱逐区块缓存前必须通过
     * {@link #closeAllInChunk(String)} 主动关闭这些视图, 否则玩家在卸载后继续操作菜单,
     * 其改动既无法落库, 也可能造成物品丢失或复制。
     */
    private static final Map<String, Set<BlockMenu>> openMenusByChunk = new ConcurrentHashMap<>();

    private Location location;

    /**
     * 标记该菜单所属区块缓存已被驱逐。
     * <p>
     * 附属可能继续持有旧菜单引用并对其操作。冻结后: 读取一律返回空、写入一律无效,
     * 防止旧菜单与数据库状态背离 (旧菜单取出物品转存真实容器 → 重载后物品复活 = 复制)。
     */
    private volatile boolean detached = false;

    private static String serializeLocation(Location l) {
        return l.getWorld().getName() + ';' + l.getBlockX() + ';' + l.getBlockY() + ';' + l.getBlockZ();
    }

    public BlockMenu(BlockMenuPreset preset, Location l) {
        super(preset);
        this.location = l;

        preset.clone(this);
        this.getContents();
    }

    @Deprecated
    public BlockMenu(BlockMenuPreset preset, Location l, Config cfg) {
        super(preset);
        this.location = l;

        for (int i = 0; i < 54; i++) {
            if (cfg.contains(String.valueOf(i))) {
                addItem(i, cfg.getItem(String.valueOf(i)));
            }
        }

        preset.clone(this);

        if (preset.getSize() > -1
                && !preset.getPresetSlots().contains(preset.getSize() - 1)
                && cfg.contains(String.valueOf(preset.getSize() - 1))) {
            addItem(preset.getSize() - 1, cfg.getItem(String.valueOf(preset.getSize() - 1)));
        }

        this.getContents();
    }

    public BlockMenu(BlockMenuPreset preset, Location l, ItemStack[] contents) {
        super(preset);
        this.location = l;

        for (int i = 0; i < contents.length; i++) {
            var item = contents[i];
            if (item == null) {
                continue;
            }
            addItem(i, item);
        }

        preset.clone(this);
        this.getContents();
    }

    public BlockMenu(BlockMenuPreset preset, Location l, Inventory inv) {
        super(preset);
        this.location = l;
        this.inventory = inv;
    }

    public void save(Location l) {
        if (!isDirty()) {
            return;
        }

        if (detached) {
            // The chunk was unloaded and this menu no longer belongs to any cached
            // block data. Persisting it would either fail or write stale contents.
            return;
        }

        // To force CS-CoreLib to build the Inventory
        this.getContents();
        SlimefunBlockData blockData = StorageCacheUtils.getBlock(location);
        if (blockData == null) {
            return;
        }
        Slimefun.getDatabaseManager().getBlockDataController().saveBlockInventory(blockData);

        changes = 0;
    }

    /**
     * Reload this {@link BlockMenu} based on its {@link BlockMenuPreset}.
     */
    public void reload() {
        this.preset.clone(this);
    }

    public Block getBlock() {
        return location.getBlock();
    }

    public Location getLocation() {
        return location;
    }

    /**
     * 记录一个 {@link BlockMenu} 视图已被玩家打开。
     * 由 {@link MenuListener} 在 {@link org.bukkit.event.inventory.InventoryOpenEvent} 时调用。
     */
    public static void registerOpen(@Nonnull BlockMenu menu) {
        if (menu.isDetached()) {
            // 已被驱逐冻结的菜单不应重新登记 (例如关闭事件被第三方取消导致视图复活)
            return;
        }
        var location = menu.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        openMenusByChunk
                .computeIfAbsent(LocationUtils.getChunkKey(location), k -> ConcurrentHashMap.newKeySet())
                .add(menu);
    }

    /**
     * 注销一个 {@link BlockMenu} 视图: 当最后一个观看者关闭菜单时调用。
     */
    public static void unregisterOpen(@Nonnull BlockMenu menu) {
        var location = menu.getLocation();
        if (location == null || location.getWorld() == null) {
            return;
        }
        var chunkKey = LocationUtils.getChunkKey(location);
        var menus = openMenusByChunk.get(chunkKey);
        if (menus != null) {
            menus.remove(menu);
            if (menus.isEmpty()) {
                openMenusByChunk.remove(chunkKey);
            }
        }
    }

    /**
     * 关闭指定区块内所有仍被玩家打开的 {@link BlockMenu} 视图, 并冻结这些菜单。
     * <p>
     * 在区块缓存驱逐前调用: 关闭事件会同步触发, 使玩家最后的操作经由
     * 关闭回调/脏数据保存流程落库, 避免卸载后继续操作已脱离缓存的菜单。
     * 关闭完成后立即冻结, 使附属持有的旧菜单引用无法再读写, 杜绝物品复制。
     */
    public static void closeAllInChunk(@Nonnull String chunkKey) {
        var menus = openMenusByChunk.remove(chunkKey);
        if (menus == null) {
            return;
        }
        for (var menu : menus) {
            menu.closeAllViewers();
            menu.detach();
        }
    }

    /**
     * 冻结该菜单: 此后读取一律返回空、写入一律无效。
     * 用于区块驱逐后使附属持有的旧引用失效。
     */
    public void detach() {
        detached = true;
    }

    public boolean isDetached() {
        return detached;
    }

    @Override
    public ItemStack getItemInSlot(int slot) {
        if (detached) {
            return null;
        }
        return super.getItemInSlot(slot);
    }

    @Override
    public void replaceExistingItem(int slot, ItemStack item, boolean event) {
        if (detached) {
            return;
        }
        super.replaceExistingItem(slot, item, event);
    }

    @Override
    @Nullable public ItemStack pushItem(ItemStack item, int... slots) {
        if (detached) {
            // Nothing can be inserted into a detached menu: return the item unchanged,
            // so the caller keeps it in the source inventory instead of losing it.
            return item;
        }
        return super.pushItem(item, slots);
    }

    /**
     * 关闭当前菜单的所有观看者视图。
     */
    public void closeAllViewers() {
        var inventory = toInventory();
        if (inventory == null) {
            return;
        }
        var viewers = inventory.getViewers();
        for (var viewer : viewers.toArray(new org.bukkit.entity.HumanEntity[0])) {
            viewer.closeInventory();
        }
    }

    /**
     * This method drops the contents of this {@link BlockMenu} on the ground at the given
     * {@link Location}.
     *
     * @param l
     *            Where to drop these items
     * @param slots
     *            The slots of items that should be dropped
     */
    public void dropItems(Location l, int... slots) {
        for (int slot : slots) {
            ItemStack item = getItemInSlot(slot);

            if (item != null) {
                l.getWorld().dropItemNaturally(l, item);
                replaceExistingItem(slot, null);
            }
        }
    }

    @Deprecated
    public void delete(Location l) {
        Slimefun.logger()
                .log(
                        Level.WARNING,
                        () -> "BlockMenu#delete(Location l) is not supported anymore. l is " + serializeLocation(l));
    }
}
