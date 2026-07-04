package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.LinkedList;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * 物品栏操作工具类, 提供跨 Paper/Folia 的线程安全 openInventory/closeInventory。
 *
 * <h3>Folia 适配</h3>
 * <p>
 * Paper 上 {@code Bukkit.isPrimaryThread()} 判断主线程;
 * Folia 上没有主线程, 此类使用 {@code Slimefun.isFolia()} 检测环境,
 * 并通过 {@code runSyncAtLocation(player.getLocation())} 确保
 * {@code player.openInventory()} 在玩家所在区域线程上执行。
 *
 * @author SlimefunGuguProject
 * @author qzgeek (Folia 线程安全修复)
 */
@UtilityClass
public class InventoryUtil {
    public void openInventory(Player p, Inventory inventory) {
        if (p == null || inventory == null) {
            return;
        }

        if (!Slimefun.isFolia() && Bukkit.isPrimaryThread()) {
            p.openInventory(inventory);
        } else if (Slimefun.isFolia()) {
            Slimefun.runSyncAtLocation(() -> p.openInventory(inventory), p.getLocation());
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

        if (!Slimefun.isFolia() && Bukkit.isPrimaryThread()) {
            new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory);
        } else if (Slimefun.isFolia()) {
            // On Folia, close inventory on each viewer's region
            for (HumanEntity viewer : new LinkedList<>(inventory.getViewers())) {
                if (viewer instanceof Player p) {
                    Slimefun.runSyncAtLocation(() -> p.closeInventory(), p.getLocation());
                }
            }
        } else {
            Slimefun.runSync(() -> new LinkedList<>(inventory.getViewers()).forEach(HumanEntity::closeInventory));
        }
    }

    public void closeInventory(Inventory inventory, Runnable callback) {
        closeInventory(inventory);

        if (!Slimefun.isFolia() && Bukkit.isPrimaryThread()) {
            callback.run();
        } else if (Slimefun.isFolia()) {
            // For Folia, callback needs a location; use first viewer's location as context
            var viewers = new LinkedList<>(inventory.getViewers());
            if (!viewers.isEmpty() && viewers.getFirst() instanceof Player p) {
                Slimefun.runSyncAtLocation(callback, p.getLocation());
            } else {
                Slimefun.runSync(callback);
            }
        } else {
            Slimefun.runSync(callback);
        }
    }
}
