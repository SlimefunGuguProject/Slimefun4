package io.github.thebusybiscuit.slimefun4.core.attributes.rotations;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.ItemAttribute;
import org.bukkit.block.BlockFace;

/**
 * Implement this interface for any {@link SlimefunItem} to prevent
 * that {@link SlimefunItem} from being rotated to
 * {@link BlockFace}.NORTH
 * {@link BlockFace}.EAST
 * {@link BlockFace}.SOUTH
 * {@link BlockFace}.WEST
 *
 * @author Ddggdd135
 *
 */
public interface NotCardinallyRotatable extends ItemAttribute {
    default BlockFace getRotation(double angle) {
        if (0 < angle && angle <= 90) return BlockFace.SOUTH_WEST;
        else if (90 < angle && angle <= 180) return BlockFace.NORTH_WEST;
        else if (-180 <= angle && angle <= -90) return BlockFace.NORTH_EAST;
        else if (-90 < angle && angle <= 0) return BlockFace.SOUTH_EAST;
        throw new IllegalArgumentException("angle must be number from -180 to 180");
    }
}
