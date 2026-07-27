package me.mrCookieSlime.CSCoreLibPlugin.general.Inventory;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.AdvancedMenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * An old {@link Listener} for CS-CoreLib
 *
 * @deprecated This is an old remnant of CS-CoreLib, the last bits of the past. They will be removed once everything is
 * updated.
 */
@Deprecated
public class MenuListener implements Listener {

    public MenuListener(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // getHolder() involves Block.getState() in BlockInventory cases
        var holder = e.getInventory().getHolder(false);

        if (holder instanceof ChestMenu menu) {
            menu.removeViewer(e.getPlayer().getUniqueId());
            menu.getMenuCloseHandler().onClose((Player) e.getPlayer());

            if (menu instanceof BlockMenu blockMenu) {
                /*
                 * InventoryCloseEvent fires while Bukkit is still finalizing the view.
                 * Re-check next tick and only return the machine to async ticking when
                 * no viewer remains. This prevents an async machine tick from racing
                 * the last inventory click during close.
                 */
                Slimefun.runSyncAt(blockMenu.getLocation(), () -> Slimefun.getTickerTask()
                        .setInventoryViewed(blockMenu.getLocation(), blockMenu.hasViewer()));
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // getHolder() involves Block.getState() in BlockInventory cases
        var holder = e.getInventory().getHolder(false);

        if (holder instanceof ChestMenu menu) {
            try {
                /*
                 * COLLECT_TO_CURSOR scans the whole inventory view and bypasses
                 * individual slot handlers. Without this guard a player can collect
                 * regenerated display/output items from protected slots.
                 */
                if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR
                        && collectWouldTouchProtectedSlot(e.getCursor(), e.getInventory(), menu)) {
                    e.setCancelled(true);
                    return;
                }

                if (e.getRawSlot() < e.getInventory().getSize()) {
                    MenuClickHandler handler = menu.getMenuClickHandler(e.getSlot());

                    if (handler == null) {
                        e.setCancelled(!menu.isEmptySlotsClickable()
                                && (e.getCurrentItem() == null
                                        || e.getCurrentItem().getType() == Material.AIR));
                    } else {
                        handleEvent(e, handler);
                    }

                } else {
                    MenuClickHandler playerInventoryHandler = menu.getPlayerInventoryClickHandler();
                    if (playerInventoryHandler != null) {
                        handleEvent(e, playerInventoryHandler);
                    }
                }
            } catch (Throwable thrown) {
                e.setCancelled(true);
                Slimefun.logger().log(Level.SEVERE, "An exception thrown while handling the click: ", thrown);
            }
        }
    }

    /**
     * Inventory drags bypass normal per-slot click handlers. Cancel drags that
     * touch handler-backed menu slots while still allowing normal input slots
     * and the player's inventory.
     */
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        var holder = e.getInventory().getHolder(false);

        if (holder instanceof ChestMenu menu
                && dragTouchesProtectedSlot(e.getRawSlots(), e.getInventory().getSize(), menu)) {
            e.setCancelled(true);
        }
    }

    static boolean collectWouldTouchProtectedSlot(ItemStack cursor, Inventory top, ChestMenu menu) {
        if (cursor == null || cursor.getType().isAir()) {
            return false;
        }

        for (int slot = 0; slot < top.getSize(); slot++) {
            if (menu.getMenuClickHandler(slot) == null) {
                continue;
            }

            ItemStack item = top.getItem(slot);
            if (item != null && !item.getType().isAir() && cursor.isSimilar(item)) {
                return true;
            }
        }

        return false;
    }

    static boolean dragTouchesProtectedSlot(Iterable<Integer> rawSlots, int topSize, ChestMenu menu) {
        for (int rawSlot : rawSlots) {
            if (rawSlot < topSize && menu.getMenuClickHandler(rawSlot) != null) {
                return true;
            }
        }

        return false;
    }

    private void handleEvent(@Nonnull InventoryClickEvent e, @Nonnull MenuClickHandler handler) {
        if (handler instanceof AdvancedMenuClickHandler advancedHandler) {
            e.setCancelled(!advancedHandler.onClick(
                    e,
                    (Player) e.getWhoClicked(),
                    e.getSlot(),
                    e.getCursor(),
                    new ClickAction(e.isRightClick(), e.isShiftClick())));
        } else {
            e.setCancelled(!handler.onClick(
                    (Player) e.getWhoClicked(),
                    e.getSlot(),
                    e.getCurrentItem(),
                    new ClickAction(e.isRightClick(), e.isShiftClick())));
        }
    }
}
