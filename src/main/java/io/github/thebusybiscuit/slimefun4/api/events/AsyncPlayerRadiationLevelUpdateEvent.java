package io.github.thebusybiscuit.slimefun4.api.events;

import javax.annotation.Nonnull;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This {@link Event} is fired while ticking the radiation level of a player, the event will determine how the player's radiation level update
 * Radiation level utils can be found in {@link io.github.thebusybiscuit.slimefun4.utils.RadiationUtils}
 *
 * @author m1919810
 *
 */
@Getter
public class AsyncPlayerRadiationLevelUpdateEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    /**
     *  This returns the radiation level of the player before this tick
     *
     * @return The previous radiation level
     */
    private final int previousLevel;

    /**
     *  This returns the increment of radiation level update, the number might be negative
     *  It will take effect in a similar way to call {@link io.github.thebusybiscuit.slimefun4.utils.RadiationUtils#addExposure(Player, int)} after the event is called
     *
     * @return The level increment
     */
    private int deltaLevel;

    /**
     *  This method override the increment of radiation level update
     *
     * @param deltaLevel Level increment override value
     */
    public void setDeltaLevel(int deltaLevel) {
        this.deltaLevel = deltaLevel;
    }

    /**
     *  This returns if the player has radiation protection , e.g. wearing hazmat armor, in creative mode
     *  by default, the level will not change ({@link AsyncPlayerRadiationLevelUpdateEvent#deltaLevel} = 0) if the player has fullProtection, but overriding the delta level using {@link AsyncPlayerRadiationLevelUpdateEvent#setDeltaLevel(int)} still changes player's radiation level even if the player has full protection
     *
     * @return The fullProtection flag
     */
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
