package io.github.thebusybiscuit.slimefun4.core.attributes.rotations;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.ItemAttribute;
import org.bukkit.block.BlockFace;

/**
 * Implement this interface for any {@link SlimefunItem} to prevent
 * that {@link SlimefunItem} from being rotated to
 * {@link BlockFace}.NORTH_EAST
 * {@link BlockFace}.NORTH_WEST
 * {@link BlockFace}.SOUTH_EAST
 * {@link BlockFace}.SOUTH_WEST
 *
 * @author Ddggdd135
 *
 */
public interface NotDiagonallyRotatable extends ItemAttribute {
    default BlockFace getRotation(double angle) {
        if (-45 < angle && angle <= 45) return BlockFace.SOUTH;
        else if (45 < angle && angle <= 135) return BlockFace.WEST;
        else if (135 <= Math.abs(angle) && Math.abs(angle) <= 180) return BlockFace.NORTH;
        else if (-135 < angle && angle <= -45) return BlockFace.EAST;
        throw new IllegalArgumentException("angle must be number from -180 to 180");
    }
}
