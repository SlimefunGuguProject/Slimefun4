package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import javax.annotation.Nonnull;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Implement this interface for any {@link SlimefunItem} to prevent
 * cargo using only its ID when comparing. #canStack is used when
 * comparing stacks
 *
 * @author Sefiraat
 */
@SlimefunAPI
public interface DistinctiveItem extends ItemAttribute {

    /**
     * This method is called by {@link SlimefunUtils#isItemSimilar} when two {@link SlimefunItemStack}
     * IDs match on a DistinctiveItem and should return if the two items can stack
     * with one another.
     *
     * @param itemMetaOne
     *                    The {@link ItemMeta} of the first stack being compared.
     * @param itemMetaTwo
     *                    The {@link ItemMeta} of the second stack being compared.
     *
     * @return Whether the two {@link ItemMeta}s are stackable
     */
    boolean canStack(@Nonnull ItemMeta itemMetaOne, @Nonnull ItemMeta itemMetaTwo);
}
