package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.destroystokyo.paper.MaterialTags;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockDispenseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.papermc.lib.PaperLib;
import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;

/**
 * This {@link Listener} listens to the {@link BlockDispenseEvent} and calls the
 * {@link BlockDispenseHandler} as a result of that.
 *
 * @author TheBusyBiscuit
 * @author MisterErwin
 *
 * @see BlockDispenseHandler
 *
 */
public class DispenserListener implements Listener {
    public DispenserListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockDispensing(BlockDispenseEvent e) {
        Block b = e.getBlock();

        if (b.getType() == Material.DISPENSER && b.getRelative(BlockFace.DOWN).getType() != Material.HOPPER) {
            SlimefunItem machine = StorageCacheUtils.getSlimefunItem(b.getLocation());

            // Fixes #2959
            if (machine != null && !machine.isDisabledIn(e.getBlock().getWorld())) {
                machine.callItemHandler(BlockDispenseHandler.class, handler -> {
                    BlockState state = PaperLib.getBlockState(b, false).getState();

                    if (state instanceof Dispenser dispenser) {
                        BlockFace face = ((Directional) b.getBlockData()).getFacing();
                        Block block = b.getRelative(face);
                        handler.onBlockDispense(e, dispenser, block, machine);
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStopBucketItemReplacingSlimefunBlock(BlockDispenseEvent event) {
        if (MaterialTags.BUCKETS.isTagged(event.getItem().getType())) {
            // fix #1103 : dispenser can replace head machines with liquid
            Block b = event.getBlock();
            BlockData blockData = b.getBlockData();
            if (blockData instanceof org.bukkit.block.data.type.Dispenser dispenser) {
                Block facingBlock = b.getRelative(dispenser.getFacing());
                if (StorageCacheUtils.hasSlimefunBlock(facingBlock.getLocation())) {
                    // just cancel this, do not care about other action
                    event.setCancelled(true);
                }
            }
        }
    }
}
