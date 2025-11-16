package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class AsyncPlayerRadiationLevelUpdateEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int previousLevel;

    @Setter
    private int deltaLevel;

    private final boolean fullProtection;

    public AsyncPlayerRadiationLevelUpdateEvent(Player player, int previousLevel, int delta, boolean hasProtection) {
        super(player, !Bukkit.isPrimaryThread());

        this.previousLevel = previousLevel;
        this.deltaLevel = delta;
        this.fullProtection = hasProtection;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return getHandlerList();
    }
}
