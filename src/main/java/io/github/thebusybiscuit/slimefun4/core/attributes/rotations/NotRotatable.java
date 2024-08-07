package io.github.thebusybiscuit.slimefun4.core.attributes.rotations;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.ItemAttribute;
import org.bukkit.block.BlockFace;

/**
 * Implement this interface for any {@link SlimefunItem} to prevent
 * that {@link SlimefunItem} from being rotated.
 *
 * @author Ddggdd135
 *
 */
public interface NotRotatable extends ItemAttribute {
    default BlockFace getRotation() {
        return BlockFace.NORTH;
    }
}
