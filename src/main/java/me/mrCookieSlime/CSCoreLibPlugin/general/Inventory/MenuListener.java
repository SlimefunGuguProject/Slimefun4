package me.mrCookieSlime.CSCoreLibPlugin.general.Inventory;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.AdvancedMenuClickHandler;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu.MenuClickHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        // getHolder() involves Block.getState() in BlockInventory cases
        var holder = e.getInventory().getHolder(false);

        if (holder instanceof ChestMenu menu) {
            try {
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
