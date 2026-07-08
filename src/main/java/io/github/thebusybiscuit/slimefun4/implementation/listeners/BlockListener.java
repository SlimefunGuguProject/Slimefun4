package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import city.norain.slimefun4.compatibillty.CompatibilityUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalBlock;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockBreakEvent;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.attributes.rotations.NotCardinallyRotatable;
import io.github.thebusybiscuit.slimefun4.core.attributes.rotations.NotDiagonallyRotatable;
import io.github.thebusybiscuit.slimefun4.core.attributes.rotations.NotRotatable;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedEnchantment;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * The {@link BlockListener} is responsible for listening to the {@link BlockPlaceEvent}
 * and {@link BlockBreakEvent}.
 *
 * @author TheBusyBiscuit
 * @author Linox
 * @author Patbox
 * @see BlockPlaceHandler
 * @see BlockBreakHandler
 * @see ToolUseHandler
 */
public class BlockListener implements Listener {

    private static final BlockFace[] CARDINAL_BLOCKFACES = new BlockFace[] {
        BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP
    };

    public BlockListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlaceExisting(BlockPlaceEvent e) {
        if (!e.canBuild()) {
            return;
        }
        Block block = e.getBlock();
        var loc = block.getLocation();

        // Fixes #2636 - This will solve the "ghost blocks" issue
        if (e.getBlockReplacedState().getType().isAir()) {
            var blockData = StorageCacheUtils.getDataContainer(loc);
            if (blockData != null && blockData.isPendingRemove()) {
                e.setCancelled(true);
                return;
            }

            SlimefunItem sfItem = StorageCacheUtils.getSlimefunItem(loc);
            if (sfItem != null) {
                for (ItemStack item : sfItem.getDrops()) {
                    if (item != null && !item.getType().isAir()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), item);
                    }
                }

                Slimefun.getDatabaseManager().getBlockDataController().removeBlock(loc);

                if (SlimefunItem.getByItem(e.getItemInHand()) != null) {
                    // Due to the delay of #clearBlockInfo, new sf block info will also be cleared. Set
                    // cancelled.
                    e.setCancelled(true);
                }
            }
        } else if (StorageCacheUtils.hasSlimefunBlock(loc)) {
            // If there is no air (e.g. grass) then don't let the block be placed
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosiveToolBlockBreak(ExplosiveToolBreakBlocksEvent e) {
        for (Block block : e.getAdditionalBlocks()) {
            checkForSensitiveBlockAbove(e.getPlayer(), block, e.getItemInHand());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!e.canBuild()) {
            return;
        }
        ItemStack item = e.getItemInHand();
        SlimefunItem sfItem = SlimefunItem.getByItem(item);

        if (sfItem != null && !(sfItem instanceof NotPlaceable)) {
            // Fixes #994, should check placed block is equals to item material or not.
            if (item.getType() != e.getBlock().getType()) {
                if (item.getType()
                        != CompatibilityUtil.getPlacementMaterial(e.getBlock().getBlockData())) {
                    return;
                }
            }

            if (!sfItem.canUse(e.getPlayer(), true)) {
                e.setCancelled(true);
            } else {
                var block = e.getBlock();

                optimizePlacement(sfItem, block, e.getPlayer().getLocation());

                var placeEvent = new SlimefunBlockPlaceEvent(e.getPlayer(), item, block, sfItem);
                Bukkit.getPluginManager().callEvent(placeEvent);

                if (placeEvent.isCancelled()) {
                    e.setCancelled(true);
                } else {
                    if (Slimefun.getBlockDataService().isTileEntity(block.getType())) {
                        Slimefun.getBlockDataService().setBlockData(block, sfItem.getId());
                    }

                    if (sfItem instanceof UniversalBlock) {
                        Slimefun.getDatabaseManager()
                                .getBlockDataController()
                                .createUniversalBlock(block.getLocation(), sfItem.getId());
                    } else {
                        Slimefun.getDatabaseManager()
                                .getBlockDataController()
                                .createBlock(block.getLocation(), sfItem.getId());
                    }

                    sfItem.callItemHandler(BlockPlaceHandler.class, handler -> handler.onPlayerPlace(e));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        // Simply ignore any events that were faked by other plugins
        if (Slimefun.getIntegrations().isEventFaked(e)) {
            return;
        }

        // Also ignore custom blocks which were placed by other plugins
        if (Slimefun.getIntegrations().isCustomBlock(e.getBlock())) {
            return;
        }

        var heldItem = e.getPlayer().getInventory().getItemInMainHand();
        var block = e.getBlock();
        var blockData = StorageCacheUtils.getDataContainer(block.getLocation());
        var sfItem = blockData == null ? null : SlimefunItem.getById(blockData.getSfId());

        // If there is a Slimefun Block here, call our BreakEvent and, if cancelled, cancel this event
        // and return
        if (blockData != null) {
            SlimefunBlockBreakEvent breakEvent =
                    new SlimefunBlockBreakEvent(e.getPlayer(), heldItem, e.getBlock(), sfItem);
            Bukkit.getPluginManager().callEvent(breakEvent);

            if (breakEvent.isCancelled()) {
                e.setCancelled(true);
                return;
            }
        }

        List<ItemStack> drops = new ArrayList<>();

        if (!heldItem.getType().isAir()) {
            int fortune = getBonusDropsWithFortune(heldItem, e.getBlock());
            callToolHandler(e, heldItem, fortune, drops);
        }

        if (!e.isCancelled()) {
            // Checks for Slimefun sensitive blocks above, using Slimefun Tags
            // TODO: merge this with the vanilla sensitive block check (when 1.18- is dropped)
            checkForSensitiveBlockAbove(e.getPlayer(), e.getBlock(), heldItem);

            if (blockData == null || blockData.isPendingRemove()) {
                dropItems(e, heldItem, block, sfItem, drops);
                return;
            }

            blockData.setPendingRemove(true);

            if (!blockData.isDataLoaded()) {
                e.setDropItems(false);
                var type = block.getType();
                StorageCacheUtils.executeAfterLoad(
                        blockData,
                        () -> {
                            callBlockHandler(e, heldItem, drops);
                            if (e.isCancelled()) {
                                block.setType(type);
                                blockData.setPendingRemove(false);
                                return;
                            }
                            e.setDropItems(true);
                            dropItems(e, heldItem, block, sfItem, drops);
                        },
                        true);
                return;
            }

            callBlockHandler(e, heldItem, drops);
            if (e.isCancelled()) {
                blockData.setPendingRemove(false);
            }
            dropItems(e, heldItem, block, sfItem, drops);

            // Checks for vanilla sensitive blocks everywhere
            // checkForSensitiveBlocks(e.getBlock(), 0, e.isDropItems());
        }
    }

    @ParametersAreNonnullByDefault
    private void callToolHandler(BlockBreakEvent e, ItemStack item, int fortune, List<ItemStack> drops) {
        SlimefunItem tool = SlimefunItem.getByItem(item);

        if (tool != null) {
            if (tool.canUse(e.getPlayer(), true)) {
                tool.callItemHandler(ToolUseHandler.class, handler -> handler.onToolUse(e, item, fortune, drops));
            } else {
                e.setCancelled(true);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void callBlockHandler(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
        var loc = e.getBlock().getLocation();
        SlimefunItem sfItem = StorageCacheUtils.getSlimefunItem(loc);

        if (sfItem != null && !sfItem.useVanillaBlockBreaking()) {
            sfItem.callItemHandler(BlockBreakHandler.class, handler -> handler.onPlayerBreak(e, item, drops));

            if (e.isCancelled()) {
                return;
            }

            drops.addAll(sfItem.getDrops());
            Slimefun.getDatabaseManager().getBlockDataController().removeBlock(loc);
        }
    }

    @ParametersAreNonnullByDefault
    private void dropItems(
            BlockBreakEvent e, ItemStack item, Block block, @Nullable SlimefunItem sfBlock, List<ItemStack> drops) {
        if (!drops.isEmpty()) {
            // TODO: properly support loading inventories within unit tests
            if (!Slimefun.instance().isUnitTest()) {
                // Notify plugins like CoreProtect
                Slimefun.getProtectionManager().logAction(e.getPlayer(), block, Interaction.BREAK_BLOCK);
            }

            // Fixes #2560
            if (e.isDropItems()) {
                // Disable slimefun block drops in vanilla way
                if (sfBlock != null) {
                    e.setDropItems(false);
                }

                // The list only contains other drops, not those from the block itself, so we still need to handle those
                for (ItemStack drop : drops) {
                    // Prevent null or air from being dropped
                    if (drop != null && drop.getType() != Material.AIR) {
                        if (e.getPlayer().getGameMode() != GameMode.CREATIVE
                                || Slimefun.getCfg().getBoolean("options.drop-block-creative")) {
                            block.getWorld().dropItemNaturally(block.getLocation(), drop);
                            drop.setAmount(0);
                        }
                    }
                }
            }
        }
    }

    /**
     * This method checks for a sensitive {@link Block}.
     * Sensitive {@link Block Blocks} are pressure plates or saplings, which should be broken
     * when the block beneath is broken as well.
     *
     * @param player The {@link Player} who broke this {@link Block}
     * @param block  The {@link Block} that was broken
     * @param item   The {@link ItemStack} that was used to break the {@link Block}
     */
    @ParametersAreNonnullByDefault
    private void checkForSensitiveBlockAbove(Player player, Block block, ItemStack item) {
        Block blockAbove = block.getRelative(BlockFace.UP);

        if (SlimefunTag.SENSITIVE_MATERIALS.isTagged(blockAbove.getType())) {
            var loc = blockAbove.getLocation();
            var blockData = StorageCacheUtils.getDataContainer(loc);
            SlimefunItem sfItem = StorageCacheUtils.getSlimefunItem(loc);

            if (sfItem != null && !sfItem.useVanillaBlockBreaking()) {
                /*
                 * We create a dummy here to pass onto the BlockBreakHandler.
                 * This will set the correct block context.
                 */
                BlockBreakEvent dummyEvent = new BlockBreakEvent(blockAbove, player);
                List<ItemStack> drops = new ArrayList<>(sfItem.getDrops(player));

                var controller = Slimefun.getDatabaseManager().getBlockDataController();
                if (blockData.isDataLoaded()) {
                    sfItem.callItemHandler(
                            BlockBreakHandler.class, handler -> handler.onPlayerBreak(dummyEvent, item, drops));
                    controller.removeBlock(loc);
                    dropItems(dummyEvent, item, block, sfItem, drops);
                } else {
                    blockData.setPendingRemove(true);
                    controller.loadDataAsync(blockData, new IAsyncReadCallback<>() {
                        @Override
                        public boolean runOnMainThread() {
                            return true;
                        }

                        @Override
                        public void onResult(ASlimefunDataContainer result) {
                            sfItem.callItemHandler(
                                    BlockBreakHandler.class, handler -> handler.onPlayerBreak(dummyEvent, item, drops));
                            controller.removeBlock(loc);
                            dropItems(dummyEvent, item, block, sfItem, drops);
                        }
                    });
                }
                blockAbove.setType(Material.AIR);
            }
        }
    }

    /**
     * This method checks recursively for any sensitive blocks
     * that are no longer supported due to this block breaking
     *
     * @param block The {@link Block} in question
     * @param count The amount of times this has been recursively called
     */
    @ParametersAreNonnullByDefault
    private void checkForSensitiveBlocks(Block block, Integer count, boolean isDropItems) {
        /**if (count >= Bukkit.getServer().getMaxChainedNeighborUpdates()) {
         * return;
         * }
         *
         * BlockState state = block.getState();
         * // We set the block to air to make use of BlockData#isSupported.
         * block.setType(Material.AIR, false);
         * for (BlockFace face : CARDINAL_BLOCKFACES) {
         * if (!isSupported(block.getRelative(face).getBlockData(), block.getRelative(face))) {
         * Block relative = block.getRelative(face);
         * if (!isDropItems) {
         * for (ItemStack drop : relative.getDrops()) {
         * block.getWorld().dropItemNaturally(relative.getLocation(), drop);
         * }
         * }
         * checkForSensitiveBlocks(relative, ++count, isDropItems);
         * }
         * }
         * // Set the BlockData back: this makes it so containers and spawners drop correctly. This is a hacky fix.
         * block.setBlockData(state.getBlockData(), false);
         * state.update(true, false);
         */
    }

    /**
     * This method checks if the {@link BlockData} would be
     * supported at the given {@link Block}.
     *
     * @param blockData The {@link BlockData} to check
     * @param block     The {@link Block} the {@link BlockData} would be at
     * @return Whether the {@link BlockData} would be supported at the given {@link Block}
     */
    @ParametersAreNonnullByDefault
    private boolean isSupported(BlockData blockData, Block block) {
        if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_19)) {
            return blockData.isSupported(block);
        } else {
            // TODO: Make 1.16-1.18 version. BlockData::isSupported is 1.19+.
            return true;
        }
    }

    private int getBonusDropsWithFortune(@Nullable ItemStack item, @Nonnull Block b) {
        int amount = 1;

        if (item != null && !item.getType().isAir() && item.hasItemMeta()) {
            /*
             * Small performance optimization:
             * ItemStack#getEnchantmentLevel() calls ItemStack#getItemMeta(), so if
             * we are handling more than one Enchantment, we should access the ItemMeta
             * directly and reuse it.
             */
            ItemMeta meta = item.getItemMeta();
            int fortuneLevel = meta.getEnchantLevel(VersionedEnchantment.FORTUNE);

            if (fortuneLevel > 0 && !meta.hasEnchant(Enchantment.SILK_TOUCH)) {
                Random random = ThreadLocalRandom.current();

                amount = Math.max(1, random.nextInt(fortuneLevel + 2) - 1);
                amount = (b.getType() == Material.LAPIS_ORE ? 4 + random.nextInt(5) : 1) * (amount + 1);
            }
        }

        return amount;
    }

    // 美化可旋转类 (如头颅) 物品放置
    private void optimizePlacement(SlimefunItem sfItem, Block block, Location l) {
        if (block.getBlockData() instanceof Rotatable rotatable
                && !(rotatable.getRotation() == BlockFace.UP || rotatable.getRotation() == BlockFace.DOWN)) {
            BlockFace rotation = null;

            if (sfItem instanceof NotCardinallyRotatable && sfItem instanceof NotDiagonallyRotatable) {
                rotation = BlockFace.NORTH;
            } else if (sfItem instanceof NotRotatable notRotatable) {
                rotation = notRotatable.getRotation();
            } else if (sfItem instanceof NotCardinallyRotatable notRotatable) {
                rotation = notRotatable.getRotation(Location.normalizeYaw(l.getYaw()));
            } else if (sfItem instanceof NotDiagonallyRotatable notRotatable) {
                rotation = notRotatable.getRotation(Location.normalizeYaw(l.getYaw()));
            }

            if (rotation != null) {
                rotatable.setRotation(rotation);
                block.setBlockData(rotatable);
            }
        }
    }
}
