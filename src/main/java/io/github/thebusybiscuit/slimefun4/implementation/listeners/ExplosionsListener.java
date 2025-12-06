package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
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
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.ExplosionResult;
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
        while (blocks.hasNext()) {
            Block block = blocks.next();
            var loc = block.getLocation();
            var blockData = StorageCacheUtils.hasBlock(loc)
                    ? StorageCacheUtils.getBlock(loc)
                    : StorageCacheUtils.getUniversalBlock(loc);
            SlimefunItem item = blockData == null ? null : SlimefunItem.getById(blockData.getSfId());

            if (item != null) {
                blocks.remove();

                var controller = Slimefun.getDatabaseManager().getBlockDataController();
                if (!(item instanceof WitherProof) && !callBreakHandler(item, blockData, block, controller)) {
                    controller.removeBlock(loc);
                    block.setType(Material.AIR);
                    // Update all networks connected to this location if the item is a network component
                    if (isNetworkComponent(item)) {
                        Slimefun.getNetworkManager().updateAllNetworks(loc);
                    }
                }
            }
        }
    }

    private boolean callBreakHandler(
            @Nonnull SlimefunItem item,
            @Nonnull SlimefunUniversalData blockData,
            @Nonnull Block block,
            @Nonnull BlockDataController controller) {
        return item.callItemHandler(BlockBreakHandler.class, handler -> {
            if (blockData.isDataLoaded()) {
                handleExplosion(handler, item, block);
            } else {
                if (blockData instanceof SlimefunBlockData sbd) {
                    controller.loadBlockDataAsync(sbd, new IAsyncReadCallback<>() {
                        @Override
                        public boolean runOnMainThread() {
                            return true;
                        }

                        @Override
                        public void onResult(SlimefunBlockData result) {
                            handleExplosion(handler, item, block);
                        }
                    });
                } else if (blockData instanceof SlimefunUniversalBlockData ubd) {
                    controller.loadBlockDataAsync(ubd, new IAsyncReadCallback<>() {
                        @Override
                        public boolean runOnMainThread() {
                            return true;
                        }

                        @Override
                        public void onResult(SlimefunUniversalData result) {
                            handleExplosion(handler, item, block);
                        }
                    });
                }
            }
        });
    }

    @ParametersAreNonnullByDefault
    private void handleExplosion(BlockBreakHandler handler, SlimefunItem item, Block block) {
        if (handler.isExplosionAllowed(block)) {
            block.setType(Material.AIR);

            List<ItemStack> drops = new ArrayList<>();
            handler.onExplode(block, drops);
            Slimefun.getDatabaseManager().getBlockDataController().removeBlock(block.getLocation());

            // Update all networks connected to this location if the item is a network component
            if (isNetworkComponent(item)) {
                Slimefun.getNetworkManager().updateAllNetworks(block.getLocation());
            }

            for (ItemStack drop : drops) {
                if (drop != null && !drop.getType().isAir()) {
                    block.getWorld().dropItemNaturally(block.getLocation(), drop);
                }
            }
        }
    }

    /**
     * Checks if the given {@link SlimefunItem} is a network component.
     * Network components include energy network components and cargo nodes.
     *
     * @param item The {@link SlimefunItem} to check
     * @return true if the item is a network component, false otherwise
     */
    private boolean isNetworkComponent(@Nonnull SlimefunItem item) {
        return item instanceof EnergyNetComponent || item instanceof CargoNode;
    }
}
