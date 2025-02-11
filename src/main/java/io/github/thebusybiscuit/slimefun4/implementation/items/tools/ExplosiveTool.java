package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import city.norain.slimefun4.compatibillty.VersionedEvent;
import com.molean.folia.adapter.Folia;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import dev.lone.itemsadder.api.CustomBlock;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.ExplosiveToolBreakBlocksEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.DamageableItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ToolUseHandler;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link SlimefunItem} is a super class for items like the {@link ExplosivePickaxe} or {@link ExplosiveShovel}.
 *
 * @author TheBusyBiscuit
 * @see ExplosivePickaxe
 * @see ExplosiveShovel
 */
public class ExplosiveTool extends SimpleSlimefunItem<ToolUseHandler> implements NotPlaceable, DamageableItem {

    private final ItemSetting<Boolean> damageOnUse = new ItemSetting<>(this, "damage-on-use", true);
    private final ItemSetting<Boolean> callExplosionEvent = new ItemSetting<>(this, "call-explosion-event", false);

    @ParametersAreNonnullByDefault
    public ExplosiveTool(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemSetting(damageOnUse, callExplosionEvent);
    }

    @Nonnull
    @Override
    public ToolUseHandler getItemHandler() {
        return (e, tool, fortune, drops) -> {
            Player p = e.getPlayer();

            if (!p.isSneaking()) {
                Block b = e.getBlock();

                b.getWorld().createExplosion(b.getLocation(), 0);
                SoundEffect.EXPLOSIVE_TOOL_EXPLODE_SOUND.playAt(b);

                List<Block> blocks = findBlocks(b);

                breakBlocks(e, p, tool, b, blocks, drops);
            }
        };
    }

    @ParametersAreNonnullByDefault
    private void breakBlocks(
            BlockBreakEvent e, Player p, ItemStack item, Block b, List<Block> blocks, List<ItemStack> drops) {
        List<Block> blocksToDestroy = new CopyOnWriteArrayList<>();

        if (callExplosionEvent.getValue()) {
            BlockExplodeEvent blockExplodeEvent = VersionedEvent.newBlockExplodeEvent(b, blocks, 0);
            Folia.getPluginManager().ce(blockExplodeEvent);

            if (!blockExplodeEvent.isCancelled()) {
                for (Block block : blockExplodeEvent.blockList()) {
                    if (canBreak(p, block)) {
                        if (Slimefun.getIntegrations().isCustomBlock(block)) {
                            drops.addAll(CustomBlock.byAlreadyPlaced(block).getLoot());
                            CustomBlock.remove(block.getLocation());
                        }
                        blocksToDestroy.add(block);
                    }
                }
            }
        } else {
            for (Block block : blocks) {
                if (canBreak(p, block)) {
                    if (Slimefun.getIntegrations().isCustomBlock(block)) {
                        drops.addAll(CustomBlock.byAlreadyPlaced(block).getLoot());
                        CustomBlock.remove(block.getLocation());
                    }
                    blocksToDestroy.add(block);
                }
            }
        }

        ExplosiveToolBreakBlocksEvent event = new ExplosiveToolBreakBlocksEvent(p, b, blocksToDestroy, item, this);
        Folia.getPluginManager().ce(event);

        /*
         * 修复: https://github.com/SlimefunGuguProject/Slimefun4/issues/853
         *
         * 为了修复该问题应该对该列表进行排序，确保头颅先被处理，具体为什么可以看下方 breakBlock 方法。
         */
        if (Bukkit.getPluginManager().isPluginEnabled("ExoticGarden")) {
            blocksToDestroy.sort((block1, block2) -> Boolean.compare(
                    block2.getType().equals(Material.PLAYER_HEAD),
                    block1.getType().equals(Material.PLAYER_HEAD)));
        }

        if (!event.isCancelled()) {
            for (Block block : blocksToDestroy) {
                breakBlock(e, p, item, block, drops);
            }
        }
    }

    @Nonnull
    private List<Block> findBlocks(@Nonnull Block b) {
        List<Block> blocks = new ArrayList<>(26);

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    // We can skip the center block since that will break as usual
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    blocks.add(b.getRelative(x, y, z));
                }
            }
        }

        return blocks;
    }

    @Override
    public boolean isDamageable() {
        return damageOnUse.getValue();
    }

    protected boolean canBreak(@Nonnull Player p, @Nonnull Block b) {
        if (b.isEmpty() || b.isLiquid()) {
            return false;
        } else if (SlimefunTag.UNBREAKABLE_MATERIALS.isTagged(b.getType())) {
            return false;
        } else if (!b.getWorld().getWorldBorder().isInside(b.getLocation())) {
            return false;
        } else {
            return Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.BREAK_BLOCK);
        }
    }

    @ParametersAreNonnullByDefault
    private void breakBlock(BlockBreakEvent event, Player player, ItemStack item, Block block, List<ItemStack> drops) {
        Slimefun.getProtectionManager().logAction(player, block, Interaction.BREAK_BLOCK);
        Material material = block.getType();

        block.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, material);
        Location blockLocation = block.getLocation();

        Optional<SlimefunItem> blockItem = Optional.ofNullable(StorageCacheUtils.getSfItem(blockLocation));

        AtomicBoolean isUseVanillaBlockBreaking = new AtomicBoolean(true);

        blockItem.ifPresentOrElse(
                sfItem -> {
                    /*
                     * 修复: https://github.com/SlimefunGuguProject/Slimefun4/issues/853
                     *
                     * 该问题源于 ExoticGarden MagicalEssence/ExoticGardenFruit useVanillaBlockBreaking 为 true，
                     * 将调用 breakNaturally 方法而非将其作为 SlimefunItem 进行处理。
                     *
                     * 此前将 blocks 进行排序，以确保头颅为最先处理的对象，检查头颅的 Y - 1 方块是否为叶子，
                     * 若为叶子则尝试获取该处的 SlimefunItem，若能获取得到则此处应为异域花园植物，将叶子处直接设置为 AIR 并移除该处 Slimefun 方块数据。
                     */
                    if (Bukkit.getPluginManager().isPluginEnabled("ExoticGarden")
                            && block.getType().equals(Material.PLAYER_HEAD)) {
                        Location leavesLocation = blockLocation.clone();
                        leavesLocation.setY(leavesLocation.getY() - 1);

                        Block leaveBlock = leavesLocation.getBlock();
                        Material leaveBlockType = leaveBlock.getType();

                        if (Tag.LEAVES.isTagged(leaveBlockType)) {
                            Optional<SlimefunItem> optionalLeavesBlockSfItem =
                                    Optional.ofNullable(StorageCacheUtils.getSfItem(leavesLocation));

                            optionalLeavesBlockSfItem.ifPresent(leavesSfItem -> {
                                Collection<ItemStack> sfItemDrops = sfItem.getDrops();
                                Collection<ItemStack> leavesSfItemDrops = leavesSfItem.getDrops();

                                if (Arrays.equals(sfItemDrops.toArray(), leavesSfItemDrops.toArray())) {
                                    leaveBlock.setType(Material.AIR);
                                    Slimefun.getDatabaseManager()
                                            .getBlockDataController()
                                            .removeBlock(leavesLocation);

                                    isUseVanillaBlockBreaking.set(false);
                                }
                            });
                        }
                    }

                    if (isUseVanillaBlockBreaking.get()) {
                        isUseVanillaBlockBreaking.set(sfItem.useVanillaBlockBreaking());
                    }

                    if (isUseVanillaBlockBreaking.get()) {
                        block.breakNaturally(item);
                    } else {
                        /*
                         * Fixes #2989
                         * We create a dummy here to pass onto the BlockBreakHandler.
                         * This will set the correct block context.
                         */
                        BlockBreakEvent dummyEvent = new BlockBreakEvent(block, event.getPlayer());

                        /*
                         * Fixes #3036 and handling in general.
                         * Call the BlockBreakHandler if the block has one to allow for proper handling.
                         */
                        sfItem.callItemHandler(
                                BlockBreakHandler.class, handler -> handler.onPlayerBreak(dummyEvent, item, drops));

                        // Make sure the event wasn't cancelled by the BlockBreakHandler.
                        if (!dummyEvent.isCancelled()) {
                            drops.addAll(sfItem.getDrops(player));
                            block.setType(Material.AIR);
                            Slimefun.getDatabaseManager()
                                    .getBlockDataController()
                                    .removeBlock(blockLocation);
                        }
                    }
                },
                () -> block.breakNaturally(item));

        damageItem(player, item);
    }
}
