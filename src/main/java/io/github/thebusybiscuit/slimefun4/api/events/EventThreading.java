package io.github.thebusybiscuit.slimefun4.api.events;

import io.github.thebusybiscuit.slimefun4.core.services.scheduling.FoliaSupport;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Resolves the asynchronous flag for events fired by Slimefun machine or entity tick work.
 */
final class EventThreading {

    private EventThreading() {}

    /**
     * Returns whether a custom Slimefun event should use Bukkit's asynchronous-event flag.
     *
     * <p>Folia treats events fired from region and global-region tick contexts as synchronous even though there is no
     * single main thread. Slimefun's machine and player tick events are routed through those owned schedulers, so they
     * must not be marked asynchronous on Folia. Paper keeps its historical primary-thread check.
     *
     * @return whether the event should be marked asynchronous
     */
    static boolean isCurrentThreadAsynchronous(Location location) {
        if (Bukkit.getServer() == null) {
            return true;
        }

        return FoliaSupport.isFolia()
                ? !Bukkit.isOwnedByCurrentRegion(location)
                : !Bukkit.isPrimaryThread();
    }

    static boolean isCurrentThreadAsynchronous(Entity entity) {
        if (Bukkit.getServer() == null) {
            return true;
        }

        return FoliaSupport.isFolia()
                ? !Bukkit.isOwnedByCurrentRegion(entity)
                : !Bukkit.isPrimaryThread();
    }

    static boolean isCurrentThreadAsynchronous() {
        // Preserve legacy behavior when events are instantiated before a server is available, such as API tests.
        return Bukkit.getServer() == null || !Bukkit.isPrimaryThread();
    }
}
