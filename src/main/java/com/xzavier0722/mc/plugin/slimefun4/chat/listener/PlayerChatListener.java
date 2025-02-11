package com.xzavier0722.mc.plugin.slimefun4.chat.listener;

import com.molean.folia.adapter.Folia;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Slimefun.getChatCatcher().pollCatcher(e.getPlayer().getUniqueId()).ifPresent(h -> {
            e.setCancelled(true);
            Folia.runSync(() -> h.accept(e.getMessage()), e.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent e) {
        Slimefun.getChatCatcher().pollCatcher(e.getPlayer().getUniqueId());
    }
}
