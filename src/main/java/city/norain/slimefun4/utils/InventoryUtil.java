package city.norain.slimefun4.utils;

import com.molean.folia.adapter.Folia;
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

        if (Bukkit.isPrimaryThread()) {
            new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
        } else {
            Folia.runSync(
                    () -> new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory),
                    inventory.getLocation());
        }
    }

    public static void closeInventory(Inventory inventory, Runnable callback) {
        closeInventory(inventory);

        if (Bukkit.isPrimaryThread()) {
            callback.run();
        } else {
            Folia.runSync(callback, inventory.getLocation());
        }
    }
}
