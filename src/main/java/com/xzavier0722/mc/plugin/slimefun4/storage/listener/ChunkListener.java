package com.xzavier0722.mc.plugin.slimefun4.storage.listener;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkListener implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        Slimefun.getDatabaseManager().getBlockDataController().loadChunk(e.getChunk(), e.isNewChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        // 仅在插件正常运行时冲刷, 避免关闭/重载流程中控制器已销毁时抛异常
        var plugin = Slimefun.instance();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }
        Slimefun.getDatabaseManager().getBlockDataController().flushEmptyChunkData(e.getChunk());
    }
}
