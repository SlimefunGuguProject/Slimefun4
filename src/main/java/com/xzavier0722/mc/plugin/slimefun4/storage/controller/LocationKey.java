package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.ScopeKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.LocationUtils;
import org.bukkit.Location;

public class LocationKey extends ScopeKey {
    private final Location location;

    public LocationKey(DataScope scope, Location location) {
        super(scope);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    protected String getKeyStr() {
        return scope + "/" + LocationUtils.getLocKey(location);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
                || (obj instanceof LocationKey other
                        && scope == other.scope
                        && location.getWorld()
                                .getName()
                                .equals(other.location.getWorld().getName())
                        && location.getBlockX() == other.location.getBlockX()
                        && location.getBlockY() == other.location.getBlockY()
                        && location.getBlockZ() == other.location.getBlockZ());
    }
}
