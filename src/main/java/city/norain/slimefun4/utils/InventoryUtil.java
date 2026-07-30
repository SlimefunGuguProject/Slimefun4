package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.LinkedList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

@UtilityClass
public class InventoryUtil {
    public void openInventory(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            return;
        }

        if (Slimefun.getSchedulerService().isOwnedByCurrentRegion(player)) {
            player.openInventory(inventory);
        } else {
            Slimefun.runSyncFor(player, () -> player.openInventory(inventory));
        }
    }

    /**
     * Closes an inventory for all current viewers using viewer-owned entity schedulers.
     *
     * @param inventory the inventory to close
     */
    public void closeInventory(Inventory inventory) {
        if (inventory == null) {
            return;
        }

        runWithInventoryOwner(inventory, () -> {
            List<HumanEntity> viewers = new LinkedList<>(inventory.getViewers());
            for (HumanEntity viewer : viewers) {
                if (!Slimefun.getSchedulerService().isOwnedByCurrentRegion(viewer)) {
                    Slimefun.getSchedulerService().runFor(viewer, viewer::closeInventory);
                } else {
                    viewer.closeInventory();
                }
            }
        });
    }

    public void closeInventory(Inventory inventory, Runnable callback) {
        if (inventory == null) {
            callback.run();
            return;
        }

        runWithInventoryOwner(inventory, () -> {
            List<HumanEntity> viewers = new LinkedList<>(inventory.getViewers());
            for (HumanEntity viewer : viewers) {
                if (!Slimefun.getSchedulerService().isOwnedByCurrentRegion(viewer)) {
                    Slimefun.getSchedulerService().runFor(viewer, viewer::closeInventory);
                } else {
                    viewer.closeInventory();
                }
            }
            callback.run();
        });
    }

    private void runWithInventoryOwner(Inventory inventory, Runnable task) {
        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Entity entity) {
            if (Slimefun.getSchedulerService().isOwnedByCurrentRegion(entity)) {
                task.run();
            } else {
                Slimefun.getSchedulerService().runFor(entity, task);
            }
        } else if (holder instanceof BlockState blockState) {
            if (Slimefun.getSchedulerService().isOwnedByCurrentRegion(blockState.getLocation())) {
                task.run();
            } else {
                Slimefun.getSchedulerService().runAt(blockState.getLocation(), task);
            }
        } else {
            // Custom plugin inventories are not tied to a world chunk. Route the viewer snapshot through Paper's
            // primary thread or Folia's global region, then close every viewer on its own entity scheduler.
            Slimefun.getSchedulerService().run(task);
        }
    }
}
