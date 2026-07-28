package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link Listener} is responsible for handling any {@link Soulbound} items.
 * A {@link Soulbound} {@link ItemStack} will not drop upon a {@link Player Player's} death.
 * Instead the {@link ItemStack} is saved and given back to the {@link Player} when they respawn.
 *
 * @author TheBusyBiscuit
 *
 */
public class SoulboundListener implements Listener {

    private final Map<UUID, Map<Integer, ItemStack>> soulbound = new ConcurrentHashMap<>();

    public SoulboundListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(PlayerDeathEvent e) {
        Player p = e.getEntity();

        // keepInventory already preserves every slot. Caching Soulbound items here would duplicate them on respawn.
        if (e.getKeepInventory()) {
            soulbound.remove(p.getUniqueId());
            return;
        }

        Map<Integer, ItemStack> items = new HashMap<>();

        for (int slot = 0; slot < p.getInventory().getSize(); slot++) {
            ItemStack item = p.getInventory().getItem(slot);

            // Store soulbound items for later retrieval
            if (SlimefunUtils.isSoulbound(item, p.getWorld())) {
                items.put(slot, item.clone());
            }
        }

        // There shouldn't even be any items in there, but let's be extra safe!
        //        Map<Integer, ItemStack> existingItems = soulbound.get(p.getUniqueId());
        // fix issue #964 dupe using soulbound
        //
        //        if (existingItems == null) {
        //        } else {
        //            existingItems.putAll(items);
        //        }
        if (items.isEmpty()) {
            soulbound.remove(p.getUniqueId());
        } else {
            soulbound.put(p.getUniqueId(), Map.copyOf(items));
        }

        // Remove soulbound items from our drops
        e.getDrops().removeIf(itemStack -> SlimefunUtils.isSoulbound(itemStack, p.getWorld()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        returnSoulboundItems(e.getPlayer());
    }

    private void returnSoulboundItems(@Nonnull Player p) {
        Map<Integer, ItemStack> items = soulbound.remove(p.getUniqueId());

        if (items != null) {
            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                p.getInventory().setItem(entry.getKey(), entry.getValue());
            }
        }
    }
}
