package com.xzavier0722.mc.plugin.slimefun4.storage.listener;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class ChunkListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Slimefun.getDatabaseManager().getBlockDataController().loadChunk(e.getChunk(), e.isNewChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        Slimefun.getDatabaseManager().getBlockDataController().onChunkUnload(e.getChunk());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent e) {
        Slimefun.getDatabaseManager().getBlockDataController().onWorldUnload(e.getWorld());
    }
}
