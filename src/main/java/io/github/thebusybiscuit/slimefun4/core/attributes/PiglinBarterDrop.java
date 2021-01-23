package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.slimefun4.implementation.listeners.entity.PiglinListener;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import org.bukkit.entity.Piglin;

/**
 * This interface, when attached to a {@link SlimefunItem}, provides a variable (0-100%) chance for
 * a {@link SlimefunItem} to be dropped by a {@link Piglin} on {@link EntityItemDropEvent}.
 *
 * @author dNiym
 * @see PiglinListener
 * @see RandomMobDrop
 */
public interface PiglinBarterDrop extends ItemAttribute {

    /**
     * Implement this method to make this {@link SlimefunItem} have a variable chance
     * of being dropped by a {@link Piglin} when bartering with them. This interface
     * should be used with the {@link RecipeType#BARTER_DROP}.
     * <p>
     * It is recommended that this chance is kept reasonably low to feel like
     * a vanilla drop as a 100% chance will completely override all {@link Piglin}
     * barter drops. (NOTE: this feature only exists in 1.16+)
     *
     * @return The integer chance (1-99%) this {@link SlimefunItem} has to drop.
     */
    int getBarteringLootChance();

}