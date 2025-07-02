package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.LinkedList;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

public class InventoryUtil {
    /**
     * Close inventory for all viewers.
     *
     * @param inventory {@link Inventory}
     */
    public static void closeInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        if (!Slimefun.folia().isFolia() && Bukkit.isPrimaryThread()) {
            new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
        } else {
            Slimefun.runSync(() -> new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory));
        }
    }

    public static void closeInventory(Inventory inventory, Runnable callback) {
        closeInventory(inventory);

        if (!Slimefun.folia().isFolia() && Bukkit.isPrimaryThread()) {
            callback.run();
        } else {
            Slimefun.runSync(callback);
        }
    }
}
