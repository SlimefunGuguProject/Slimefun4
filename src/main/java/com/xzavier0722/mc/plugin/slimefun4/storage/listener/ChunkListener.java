package com.xzavier0722.mc.plugin.slimefun4.storage.listener;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

    /**
     * 世界卸载时驱逐该世界的全部区块缓存。
     * <p>
     * 注意: 必须忽略已取消的事件。若其他插件取消了 WorldUnloadEvent, 世界仍保持加载,
     * 此时驱逐缓存会导致机器停摆且不会有后续 ChunkLoadEvent 来恢复数据。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent e) {
        Slimefun.getDatabaseManager().getBlockDataController().onWorldUnload(e.getWorld());
    }
}
