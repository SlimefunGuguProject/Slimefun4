package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.WitherProofBlock;
import javax.annotation.Nonnull;
import org.bukkit.block.Block;
import org.bukkit.entity.Wither;
import org.bukkit.event.entity.EntityChangeBlockEvent;

/**
 * This Interface, when attached to a class that inherits from {@link SlimefunItem}, marks
 * the Item as "Wither-Proof".
 * Wither-Proof blocks cannot be destroyed by a {@link Wither}.
 *
 * @author TheBusyBiscuit
 *
 * @see WitherProofBlock
 *
 */
public interface WitherProof extends ItemAttribute {

    /**
     * This method is called when a {@link Wither} tried to attack the given {@link Block}.
     * You can use this method to play particles or even damage the {@link Wither}.
     * This method is only called from {@link WitherProof#onAttackEvent(EntityChangeBlockEvent)}
     *
     * @param block
     *            The {@link Block} which was attacked.
     * @param wither
     *            The {@link Wither} who attacked.
     */
    void onAttack(@Nonnull Block block, @Nonnull Wither wither);

    /**
     * This method is called when a {@link Wither} tried to attack the block.
     * You can use this method to handle the {@link EntityChangeBlockEvent}.
     *
     * @param event
     *            The {@link EntityChangeBlockEvent} which was involved.
     */
    default void onAttackEvent(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Wither wither) {
            event.setCancelled(true);
            onAttack(event.getBlock(), wither);
        }
    }
}
