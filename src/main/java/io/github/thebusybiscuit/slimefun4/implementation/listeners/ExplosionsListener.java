package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.WitherProof;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.CargoNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.ExplosionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The {@link ExplosionsListener} is a {@link Listener} which listens to any explosion events.
 * Any {@link WitherProof} block is excluded from these explosions and this {@link Listener} also
 * calls the explosive part of the {@link BlockBreakHandler}.
 *
 * @author TheBusyBiscuit
 * @see BlockBreakHandler
 * @see WitherProof
 */
public class ExplosionsListener implements Listener {

    public ExplosionsListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        /**
         * Wind charge **doesn't** break block but spigot still give us break list,
         * so we just ignore it.
         */
        if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_21)
                && (e.getEntityType() == EntityType.WIND_CHARGE
                        || e.getEntityType() == EntityType.BREEZE_WIND_CHARGE)) {
            return;
        }

        removeResistantBlocks(e.blockList().iterator());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_21)
                && e.getExplosionResult() == ExplosionResult.TRIGGER_BLOCK) {
            return;
        }

        removeResistantBlocks(e.blockList().iterator());
    }

    private void removeResistantBlocks(@Nonnull Iterator<Block> blocks) {
        // For explosion handling, only update network once for a random affected block to improve performance.
        AtomicBoolean networkUpdated = new AtomicBoolean(false);

        while (blocks.hasNext()) {
            Block block = blocks.next();
            var loc = block.getLocation();
            var blockData = StorageCacheUtils.getDataContainer(loc);
            SlimefunItem item = blockData == null ? null : SlimefunItem.getById(blockData.getSfId());

            if (item != null) {
                blocks.remove();

                if (!(item instanceof WitherProof) && !callBreakHandler(item, blockData, block, networkUpdated)) {
                    Slimefun.getDatabaseManager().getBlockDataController().removeBlock(loc);
                    block.setType(Material.AIR);
                    updateNearbyNetwork(item, loc, networkUpdated);
                }
            }
        }
    }

    private boolean callBreakHandler(
            SlimefunItem item, ASlimefunDataContainer blockData, Block block, AtomicBoolean updateRef) {
        return !item.callItemHandler(BlockBreakHandler.class, handler -> {
            if (blockData.isDataLoaded()) {
                handleExplosion(handler, block, item, updateRef);
            } else {
                Slimefun.getDatabaseManager()
                        .getBlockDataController()
                        .loadDataAsync(blockData, new IAsyncReadCallback<>() {
                            @Override
                            public boolean runOnMainThread() {
                                return true;
                            }

                            @Override
                            public void onResult(ASlimefunDataContainer result) {
                                handleExplosion(handler, block, item, updateRef);
                            }
                        });
            }
        });
    }

    @ParametersAreNonnullByDefault
    private void handleExplosion(BlockBreakHandler handler, Block block, SlimefunItem item, AtomicBoolean ref) {
        if (handler.isExplosionAllowed(block)) {
            block.setType(Material.AIR);

            List<ItemStack> drops = new ArrayList<>();
            handler.onExplode(block, drops);
            Slimefun.getDatabaseManager().getBlockDataController().removeBlock(block.getLocation());

            for (ItemStack drop : drops) {
                if (drop != null && !drop.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }

            updateNearbyNetwork(item, block.getLocation(), ref);
        }
    }

    @ParametersAreNonnullByDefault
    private void updateNearbyNetwork(SlimefunItem item, Location loc, AtomicBoolean updated) {
        if (updated.get()) {
            return;
        }

        if (!(item instanceof EnergyNetComponent) && !(item instanceof CargoNode)) {
            return;
        }

        Slimefun.getNetworkManager().updateAllNetworks(loc);

        updated.getAndSet(true);
    }
}
