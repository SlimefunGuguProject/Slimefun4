package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class BackpackCache {
    private final Map<String, Map<Integer, PlayerBackpack>> numCache;
    private final Map<String, PlayerBackpack> uuidCache;

    BackpackCache() {
        numCache = new ConcurrentHashMap<>();
        uuidCache = new ConcurrentHashMap<>();
    }

    void put(PlayerBackpack backpack) {
        numCache.computeIfAbsent(
                        backpack.getOwner().getUniqueId().toString(), k -> Collections.synchronizedMap(new HashMap<>()))
                .put(backpack.getId(), backpack);
        uuidCache.put(backpack.getUniqueId().toString(), backpack);
    }

    PlayerBackpack get(String pUuid, int num) {
        var map = numCache.get(pUuid);
        return map == null ? null : map.get(num);
    }

    PlayerBackpack get(String uuid) {
        return uuidCache.get(uuid);
    }

    void invalidate(String pUuid) {
        var cache = numCache.remove(pUuid);
        if (cache == null) {
            return;
        }

        cache.values().forEach(bp -> {
            uuidCache.remove(bp.getUniqueId().toString());
            bp.markInvalid();
        });
    }

    void clean() {
        numCache.clear();
        uuidCache.clear();
    }
}
