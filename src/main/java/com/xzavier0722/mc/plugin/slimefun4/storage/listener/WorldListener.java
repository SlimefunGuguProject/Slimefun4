package com.xzavier0722.mc.plugin.slimefun4.storage.listener;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {

    @EventHandler
    public void onChunkLoad(WorldLoadEvent e) {
        Slimefun.getDatabaseManager().getBlockDataController().loadWorld(e.getWorld());
    }
}
