package city.norain.slimefun4.api.menu;

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
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
        this(preset, uuid, (Location) null);
    }

    public UniversalMenu(@Nonnull UniversalMenuPreset preset, @Nonnull UUID uuid, @Nullable Location lastPresent) {
        super(preset);
        this.uuid = uuid;

        preset.clone(this, lastPresent);
        this.getContents();
    }

    public UniversalMenu(
            @Nonnull UniversalMenuPreset preset,
            @Nonnull UUID uuid,
            @Nullable Location lastPresent,
            ItemStack[] contents) {
        super(preset);
        this.uuid = uuid;

        for (int i = 0; i < contents.length; i++) {
            var item = contents[i];
            if (item == null || item.getType().isAir()) {
                continue;
            }
            addItem(i, item);
        }

        preset.clone(this, lastPresent);
        this.getContents();
    }

    public UniversalMenu(@Nonnull UniversalMenuPreset preset, @Nonnull UUID uuid, ItemStack[] contents) {
        this(preset, uuid, null, contents);
    }

    public void update(@Nullable Location lastPresent) {
        ((UniversalMenuPreset) preset).clone(this, lastPresent);
    }

    /**
     * This method drops the contents of this {@link BlockMenu} on the ground at the given
     * {@link Location}.
     *
     * @param l     Where to drop these items
     * @param slots The slots of items that should be dropped
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
