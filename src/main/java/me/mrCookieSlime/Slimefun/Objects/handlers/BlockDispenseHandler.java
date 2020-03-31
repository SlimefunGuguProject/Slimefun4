package me.mrCookieSlime.Slimefun.Objects.handlers;

import io.github.thebusybiscuit.slimefun4.api.exceptions.IncompatibleItemHandlerException;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.BlockPlacer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.NotPlaceable;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Dispenser;
import org.bukkit.event.block.BlockDispenseEvent;

import java.util.Optional;

/**
 * This {@link ItemHandler} is triggered when the {@link SlimefunItem} it was assigned to
 * is a {@link Dispenser} and was triggered.
 * <p>
 * This {@link ItemHandler} is used for the {@link BlockPlacer}.
 *
 * @author TheBusyBiscuit
 * @see ItemHandler
 * @see BlockPlacer
 */
@FunctionalInterface
public interface BlockDispenseHandler extends ItemHandler {

    void onBlockDispense(BlockDispenseEvent e, Dispenser dispenser, Block facedBlock, SlimefunItem machine);

    @Override
    default Optional<IncompatibleItemHandlerException> validate(SlimefunItem item) {
        if (item instanceof NotPlaceable || item.getItem().getType() != Material.DISPENSER) {
            return Optional.of(new IncompatibleItemHandlerException("Only dispensers that are not marked as 'NotPlaceable' can have a BlockDispenseHandler.", item, this));
        }

        return Optional.empty();
    }

    @Override
    default Class<? extends ItemHandler> getIdentifier() {
        return BlockDispenseHandler.class;
    }
}
