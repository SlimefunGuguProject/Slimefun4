package me.mrCookieSlime.Slimefun.api.inventory;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

// This class will be deprecated, relocated and rewritten in a future version.
public class BlockMenu extends DirtyChestMenu {

    private Location location;

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
        for (int i = 0; i < inv.getSize(); i++) {
            addItem(i, inv.getItem(i));
        }
    }

    public void save(Location l) {
        if (!isDirty()) {
            return;
        }

        // To force CS-CoreLib to build the Inventory
        this.getContents();
        SlimefunBlockData blockData = StorageCacheUtils.getBlock(location);
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
