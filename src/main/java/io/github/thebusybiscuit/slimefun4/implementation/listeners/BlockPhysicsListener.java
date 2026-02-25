package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.WitherProof;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import io.papermc.lib.PaperLib;
import io.papermc.lib.features.blockstatesnapshot.BlockStateSnapshotResult;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.block.data.type.Piston;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link Listener} is responsible for listening to any physics-based events, such
 * as {@link EntityChangeBlockEvent} or a {@link BlockPistonEvent}.
 * <p>
 * This ensures that a {@link Piston} cannot be abused to break Slimefun blocks.
 *
 * @author VoidAngel
 * @author Poslovitch
 * @author TheBusyBiscuit
 * @author AccelShark
 */
public class BlockPhysicsListener implements Listener {

    public BlockPhysicsListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFall(EntityChangeBlockEvent e) {
        var blockData = StorageCacheUtils.getDataContainer(e.getBlock().getLocation());
        if (blockData == null) {
            return;
        }

        switch (e.getEntity().getType()) {
            case FALLING_BLOCK -> {
                e.setCancelled(true);
                var block = (FallingBlock) e.getEntity();

                if (block.getDropItem()) {
                    block.getWorld()
                            .dropItemNaturally(
                                    block.getLocation(),
                                    new ItemStack(block.getBlockData().getMaterial(), 1));
                }
            }

            case WITHER -> {
                // fix issue 1126
                // the wither break is handled in the WitherListener, then the data is removed there, so it will
                // conflict with this listener
                var block = e.getBlock();
                var item = SlimefunItem.getById(blockData.getSfId());
                var controller = Slimefun.getDatabaseManager().getBlockDataController();
                if (item != null) {
                    if (item instanceof WitherProof witherProof) {
                        witherProof.onAttackEvent(e);
                        if (!e.isCancelled()) {
                            return;
                        }
                    }
                    controller.removeBlock(block.getLocation());
                    block.setType(Material.AIR);
                    for (var drop : item.getDrops()) {
                        if (drop != null && !drop.getType().isAir()) {
                            block.getWorld().dropItemNaturally(block.getLocation(), drop);
                        }
                    }
                }
            }
                // fix: issue 1126 there is not such EntityChangeBlockEvent about WitherSkull
                // Don't move my machine :|
            case ENDERMAN -> e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (StorageCacheUtils.hasSlimefunBlock(e.getBlock().getLocation())) {
            e.setCancelled(true);
        } else {
            for (Block b : e.getBlocks()) {
                if (StorageCacheUtils.hasSlimefunBlock(b.getLocation())
                        || (b.getRelative(e.getDirection()).getType() == Material.AIR
                                && StorageCacheUtils.hasSlimefunBlock(
                                        b.getRelative(e.getDirection()).getLocation()))) {
                    e.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        if (StorageCacheUtils.hasSlimefunBlock(e.getBlock().getLocation())) {
            e.setCancelled(true);
        } else if (e.isSticky()) {
            for (Block b : e.getBlocks()) {
                if (StorageCacheUtils.hasSlimefunBlock(b.getLocation())
                        || (b.getRelative(e.getDirection()).getType() == Material.AIR
                                && StorageCacheUtils.hasSlimefunBlock(
                                        b.getRelative(e.getDirection()).getLocation()))) {
                    e.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent e) {
        Block block = e.getToBlock();
        Material type = block.getType();

        // Check if this Material can be destroyed by fluids
        if (SlimefunTag.FLUID_SENSITIVE_MATERIALS.isTagged(type)) {
            // Check if this Block holds any data
            if (StorageCacheUtils.hasSlimefunBlock(block.getLocation())) {
                e.setCancelled(true);
            }
            return;
        }

        BlockStateSnapshotResult state = PaperLib.getBlockState(block, false);

        // Check the skull if it had lost its data, but name still remained.
        if (state.getState() instanceof Skull) {
            Skull skull = (Skull) state.getState();

            if (skull.hasOwner() && Objects.equals(skull.getOwningPlayer().getName(), "CS-CoreLib")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBucketUse(PlayerBucketEmptyEvent e) {
        // Fix for placing water on player heads
        Location l = e.getBlockClicked().getRelative(e.getBlockFace()).getLocation();

        if (StorageCacheUtils.hasSlimefunBlock(l)) {
            e.setCancelled(true);
        }
    }
}
