package io.github.thebusybiscuit.slimefun4.core.attributes;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;

import javax.annotation.Nonnull;

/**
 * An empty interface that only serves the purpose of bundling together all
 * interfaces of that kind.
 * <p>
 * An {@link ItemAttribute} must be attached to a {@link SlimefunItem}.
 *
 * @author TheBusyBiscuit
 * @see SlimefunItem
 */
public interface ItemAttribute {

    /**
     * Returns the identifier of the associated {@link SlimefunItem}.
     *
     * @return the identifier of the {@link SlimefunItem}
     */
    @Nonnull
    String getId();
}