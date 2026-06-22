package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;

final class ChunkReference {
    private final String chunkKey;
    private final String worldName;

    ChunkReference(Chunk chunk) {
        chunkKey = LocationUtils.getChunkKey(chunk);
        worldName = chunk.getWorld().getName();
    }

    Chunk getChunk() {
        var world = Bukkit.getWorld(worldName);
        if (world == null) {
            throw new IllegalStateException("World is not loaded: " + worldName);
        }

        return LocationUtils.toChunk(world, chunkKey);
    }
}
