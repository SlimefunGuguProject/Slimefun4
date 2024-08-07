package me.mrCookieSlime.Slimefun.api.inventory;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * This class represents a universal chest menu
 * which a menu located by certain identify id instead of location.
 */
public class UniversalMenu extends DirtyChestMenu {
    @Getter
    private final UUID uuid;

    public UniversalMenu(@Nonnull UniversalMenuPreset preset, @Nonnull UUID uuid) {
        super(preset);
        this.uuid = uuid;

        preset.clone(this);
        this.getContents();
    }

    public UniversalMenu(@Nonnull UniversalMenuPreset preset, @Nonnull UUID uuid, ItemStack[] contents) {
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
        this.getContents();
    }

    public UniversalMenu(
            @Nonnull UniversalMenuPreset preset, @Nonnull SlimefunUniversalData universalData, ItemStack[] contents) {
        super(preset);
        this.uuid = universalData.getUUID();

        for (int i = 0; i < contents.length; i++) {
            var item = contents[i];
            if (item == null) {
                continue;
            }
            addItem(i, item);
        }

        preset.clone(this, universalData.getLastPresent());
        this.getContents();
    }

    public void update() {
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
