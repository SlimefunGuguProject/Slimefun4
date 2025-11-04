package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.LinkedList;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

@UtilityClass
public class InventoryUtil {
    public void openInventory(Player p, Inventory inventory) {
        if (p == null || inventory == null) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            p.openInventory(inventory);
        } else {
            Slimefun.runSync(() -> p.openInventory(inventory));
        }
    }

    /**
     * Close inventory for all viewers.
     *
     * @param inventory {@link Inventory}
     */
    public void closeInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
        } else {
            Slimefun.runSync(() -> new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory));
        }
    }

    public void closeInventory(Inventory inventory, Runnable callback) {
        closeInventory(inventory);

        if (Bukkit.isPrimaryThread()) {
            callback.run();
        } else {
            Slimefun.runSync(callback);
        }
    }
}
