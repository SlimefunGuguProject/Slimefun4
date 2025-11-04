package io.github.thebusybiscuit.slimefun4.implementation.listeners.crafting;

import city.norain.slimefun4.SlimefunExtended;
import city.norain.slimefun4.compatibillty.VersionedEvent;
import io.github.bakedlibs.dough.versions.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import org.bukkit.block.Crafter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;

public class VanillaCrafterListener implements SlimefunCraftingListener {
    public VanillaCrafterListener(@Nonnull Slimefun plugin) {
        if (SlimefunExtended.getMinecraftVersion().isAtLeast(MinecraftVersion.parse("1.20.3")))
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrafter(InventoryClickEvent e) {
        Inventory clickedInventory = e.getClickedInventory();
        Inventory topInventory = VersionedEvent.getTopInventory(e);

        if (clickedInventory != null
                && topInventory.getType() == InventoryType.CRAFTER
                && topInventory.getHolder(false) instanceof Crafter
                && e.getWhoClicked() instanceof Player player) {

            if (e.getAction() == InventoryAction.HOTBAR_SWAP) {
                e.setCancelled(true);
                return;
            }

            if (clickedInventory.getType() == InventoryType.CRAFTER) {
                e.setCancelled(isCraftingUnallowed(SlimefunItem.getByItem(e.getCursor())));
            } else {
                e.setCancelled(isCraftingUnallowed(SlimefunItem.getByItem(e.getCurrentItem())));
            }

            if (e.getResult() == Event.Result.DENY) {
                Slimefun.getLocalization().sendMessage((Player) e.getWhoClicked(), "crafter.not-working", true);
            }
        }
    }

    @EventHandler
    public void hopperOnCrafter(InventoryMoveItemEvent e) {
        if (e.getDestination().getType() == InventoryType.CRAFTER && isCraftingUnallowed(e.getItem())) {
            e.setCancelled(true);
        }
    }
}
