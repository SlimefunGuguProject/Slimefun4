package com.xzavier0722.mc.plugin.slimefun4.chat.listener;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerChatListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Slimefun.getChatCatcher().pollCatcher(e.getPlayer().getUniqueId()).ifPresent(handler -> {
            e.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(e.message());
            Slimefun.getSchedulerService().runFor(e.getPlayer(), () -> handler.accept(message));
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent e) {
        Slimefun.getChatCatcher().pollCatcher(e.getPlayer().getUniqueId());
    }
}
