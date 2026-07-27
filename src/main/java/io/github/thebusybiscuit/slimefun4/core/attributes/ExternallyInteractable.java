package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.interactions.InteractionResult;
import javax.annotation.Nonnull;
import org.bukkit.Location;

/**
 * Implement this interface for any {@link SlimefunItem} to provide methods for
 * external code to 'interact' with the item when placed as a block in the world.
 *
 * @author Sefiraat
 */
@FunctionalInterface
@SlimefunAPI
public interface ExternallyInteractable {

    /**
     * This method should be used by the implementing class to fulfill the actions needed
     * when being interacted with returning the result of the interaction.
     *
     * @param location
     *                 The {@link Location} of the Block being targeted for the interaction.
     *
     * @return The {@link InteractionResult} denoting the result of the interaction.
     */
    @Nonnull
    InteractionResult onInteract(@Nonnull Location location);
}
