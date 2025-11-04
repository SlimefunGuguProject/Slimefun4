package city.norain.slimefun4.api.menu;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;

public abstract class UniversalMenuPreset extends BlockMenuPreset {
    /**
     * Creates a new ChestMenu with the specified
     * Title
     *
     * @param title The title of the Menu
     */
    public UniversalMenuPreset(@Nonnull String id, @Nonnull String title) {
        super(id, title);
    }

    /**
     * 创建一个新的菜单实例
     *
     * @param menu {@link UniversalMenu} 通用菜单
     * @param b 当前实例对应的方块，方块可能为空
     */
    public void newInstance(@Nonnull UniversalMenu menu, @Nullable Block b) {
        // This method can optionally be overridden by implementations
    }

    @Override
    protected void clone(@Nonnull DirtyChestMenu menu) {
        if (menu instanceof UniversalMenu universalMenu) {
            var uniData = StorageCacheUtils.getUniversalBlock(universalMenu.getUuid());

            if (uniData == null) {
                return;
            }

            clone(universalMenu, uniData.getLastPresent().toLocation());
        }
    }

    protected void clone(@Nonnull UniversalMenu menu, @Nullable Location lastPresent) {
        menu.setPlayerInventoryClickable(true);

        for (int slot : occupiedSlots) {
            menu.addItem(slot, getItemInSlot(slot));
        }

        if (isSizeAutomaticallyInferred()) {
            menu.addItem(getSize() - 1, null);
        }

        Block b = null;

        if (lastPresent != null) {
            b = lastPresent.getBlock();
        }

        newInstance(menu, b);

        for (int slot = 0; slot < 54; slot++) {
            if (getMenuClickHandler(slot) != null) {
                menu.addMenuClickHandler(slot, getMenuClickHandler(slot));
            }
        }

        menu.addMenuOpeningHandler(getMenuOpeningHandler());
        menu.addMenuCloseHandler(getMenuCloseHandler());
    }

    @Nullable public static UniversalMenuPreset getPreset(@Nullable String id) {
        if (id == null) {
            return null;
        } else {
            var preset = Slimefun.getRegistry().getMenuPresets().get(id);
            if (preset instanceof UniversalMenuPreset uniPreset) {
                return uniPreset;
            } else {
                return null;
            }
        }
    }
}
