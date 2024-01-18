package me.mrCookieSlime.Slimefun.api.inventory;

import java.util.UUID;
import javax.annotation.Nonnull;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * This class represents a universal chest menu
 * which a menu located by certain identify id instead of location.
 */
public class UniversalChestMenu extends DirtyChestMenu {
    private final UUID uuid;

    public UniversalChestMenu(@Nonnull UniversalMenuPreset preset, @Nonnull UUID uuid) {
        super(preset);
        this.uuid = uuid;
    }

    public UniversalChestMenu(@Nonnull UniversalMenuPreset preset, @Nonnull UUID uuid, ItemStack[] contents) {
        super(preset);
        this.uuid = uuid;

        for (int i = 0; i < contents.length; i++) {
            var item = contents[i];
            if (item == null) {
                continue;
            }
            addItem(i, item);
        }

        preset.clone(this);
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
}