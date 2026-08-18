package com.xzavier0722.mc.plugin.slimefun4.storage.event;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunChunkData;
import javax.annotation.Nonnull;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SlimefunChunkDataLoadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final SlimefunChunkData chunkData;

    public SlimefunChunkDataLoadEvent(SlimefunChunkData chunkData) {
        this.chunkData = chunkData;
    }

    public SlimefunChunkData getChunkData() {
        return chunkData;
    }

    public World getWorld() {
        return chunkData.getWorld();
    }

    public Chunk getChunk() {
        return chunkData.getChunk();
    }

    public static @Nonnull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @Nonnull HandlerList getHandlers() {
        return getHandlerList();
    }
}
