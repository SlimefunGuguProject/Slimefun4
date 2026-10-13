package com.xzavier0722.mc.plugin.slimefun4.storage.listener;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Slimefun.getDatabaseManager().getBlockDataController().loadChunk(e.getChunk(), e.isNewChunk());
    }
}
