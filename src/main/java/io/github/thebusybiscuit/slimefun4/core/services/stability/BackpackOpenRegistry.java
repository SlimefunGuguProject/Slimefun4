package io.github.thebusybiscuit.slimefun4.core.services.stability;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

/**
 * Atomically reserves backpack open requests by player and backpack identity.
 *
 * <p>The two-level reservation prevents both repeated requests from one player and stale-snapshot races where two
 * different players attempt to open the same backpack at once.
 */
public final class BackpackOpenRegistry {

    private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    private final Set<String> backpackKeys = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> reservations = new ConcurrentHashMap<>();

    public boolean reserve(@Nonnull UUID playerId, @Nonnull String backpackKey) {
        if (!players.add(playerId)) {
            return false;
        }

        if (!backpackKeys.add(backpackKey)) {
            players.remove(playerId);
            return false;
        }

        reservations.put(playerId, backpackKey);
        return true;
    }

    public void release(@Nonnull UUID playerId, @Nonnull String backpackKey) {
        if (reservations.remove(playerId, backpackKey)) {
            backpackKeys.remove(backpackKey);
            players.remove(playerId);
        }
    }

    public void release(@Nonnull UUID playerId) {
        String backpackKey = reservations.remove(playerId);
        if (backpackKey != null) {
            backpackKeys.remove(backpackKey);
        }
        players.remove(playerId);
    }

    public boolean isOpening(@Nonnull UUID playerId) {
        return players.contains(playerId);
    }
}
