package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Bukkit;

/**
 * Resolves the asynchronous flag for events that may be fired by a Slimefun machine ticker on either thread.
 */
final class EventThreading {

    private EventThreading() {}

    /**
     * Paper requires an event's asynchronous flag to match the thread that calls it. Machine tickers normally run
     * asynchronously, but are moved onto the primary thread while their inventory is being viewed.
     *
     * @return whether an event constructed on the current thread must be marked asynchronous
     */
    static boolean isCurrentThreadAsynchronous() {
        // Preserve the legacy asynchronous behavior when events are instantiated before a server is available,
        // such as in lightweight API tests.
        return Bukkit.getServer() == null || !Bukkit.isPrimaryThread();
    }
}
