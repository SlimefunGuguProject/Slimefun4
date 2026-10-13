package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.ScopeKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.LocationUtils;
import org.bukkit.Chunk;

public class ChunkKey extends ScopeKey {
    private final Chunk chunk;

    public ChunkKey(DataScope scope, Chunk chunk) {
        super(scope);
        this.chunk = chunk;
    }

    @Override
    protected String getKeyStr() {
        return scope + "/" + LocationUtils.getChunkKey(chunk);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || (obj instanceof ChunkKey other
                        && scope == other.scope
                        && chunk.getWorld()
                                .getName()
                                .equals(other.chunk.getWorld().getName())
                        && chunk.getX() == other.chunk.getX()
                        && chunk.getZ() == other.chunk.getZ());
    }
}
