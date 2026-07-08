package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalBlock;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalDataTrait;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link Listener} listens to the {@link PlayerInteractEvent}.
 * It is also responsible for calling our {@link PlayerRightClickEvent} and triggering any
 * {@link ItemUseHandler} or {@link BlockUseHandler} for the clicked {@link ItemStack} or {@link Block}.
 *
 * @author TheBusyBiscuit
 * @author Liruxo
 * @see PlayerRightClickEvent
 * @see ItemUseHandler
 * @see BlockUseHandler
 */
public class SlimefunItemInteractListener implements Listener {

    public SlimefunItemInteractListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Exclude the Debug Fish here because it is handled in a seperate Listener
            if (SlimefunUtils.isItemSimilar(e.getItem(), SlimefunItems.DEBUG_FISH, true)) {
                return;
            }

            // Fixes #4087 - Prevents players from interacting with a block that is about to be deleted
            // We especially don't want to open inventories as that can cause duplication
            if (e.getClickedBlock() != null && StorageCacheUtils.isBlockPendingRemove(e.getClickedBlock())) {
                e.setCancelled(true);
                return;
            }

            // Fire our custom Event
            PlayerRightClickEvent event = new PlayerRightClickEvent(e);
            Bukkit.getPluginManager().callEvent(event);

            boolean itemUsed = e.getHand() == EquipmentSlot.OFF_HAND;

            // Only handle the Item if it hasn't been denied
            if (event.useItem() != Result.DENY) {
                rightClickItem(e, event, itemUsed);
            }

            if (!itemUsed && event.useBlock() != Result.DENY && !rightClickBlock(event)) {
                return;
            }

            /**
             * If the original Event was not denied but the custom one was,
             * we also want to deny the original one.
             * This only applies for non-denied events because we do not want to
             * override any protective checks.
             */
            if (e.useInteractedBlock() != Result.DENY) {
                e.setUseInteractedBlock(event.useBlock());
            }

            if (e.useItemInHand() != Result.DENY) {
                e.setUseItemInHand(event.useItem());
            }
        }
    }

    @ParametersAreNonnullByDefault
    private boolean rightClickItem(PlayerInteractEvent e, PlayerRightClickEvent event, boolean defaultValue) {
        Optional<SlimefunItem> optional = event.getSlimefunItem();

        if (optional.isPresent()) {
            SlimefunItem sfItem = optional.get();

            if (sfItem.canUse(e.getPlayer(), true)) {
                return sfItem.callItemHandler(ItemUseHandler.class, handler -> handler.onRightClick(event));
            } else {
                event.setUseItem(Result.DENY);
            }
        }

        return defaultValue;
    }

    @ParametersAreNonnullByDefault
    private boolean rightClickBlock(PlayerRightClickEvent event) {
        Optional<SlimefunItem> optional = event.getSlimefunBlock();

        if (optional.isPresent()) {
            SlimefunItem sfItem = optional.get();

            if (!sfItem.canUse(event.getPlayer(), true)) {
                event.getInteractEvent().setCancelled(true);
                return false;
            }

            boolean interactable =
                    sfItem.callItemHandler(BlockUseHandler.class, handler -> handler.onRightClick(event));

            if (!interactable) {
                Player p = event.getPlayer();

                if (BlockMenuPreset.isInventory(sfItem.getId())) {
                    openInventory(p, sfItem, event.getInteractEvent().getClickedBlock(), event);
                    return false;
                }
            }
        }

        return true;
    }

    @ParametersAreNonnullByDefault
    private void openInventory(Player p, SlimefunItem item, Block clickedBlock, PlayerRightClickEvent event) {
        try {
            if (!p.isSneaking() || event.getItem().getType() == Material.AIR) {
                event.getInteractEvent().setCancelled(true);

                if (!(item instanceof UniversalBlock)) {
                    var blockData = StorageCacheUtils.getBlock(clickedBlock.getLocation());

                    if (blockData == null) {
                        return;
                    }

                    if (blockData.isDataLoaded()) {
                        openMenu(blockData.getBlockMenu(), clickedBlock, p);
                    } else {
                        Slimefun.getDatabaseManager()
                                .getBlockDataController()
                                .loadBlockDataAsync(blockData, new IAsyncReadCallback<>() {
                                    @Override
                                    public boolean runOnMainThread() {
                                        return true;
                                    }

                                    @Override
                                    public void onResult(SlimefunBlockData result) {
                                        if (!p.isOnline()) {
                                            return;
                                        }

                                        openMenu(result.getBlockMenu(), clickedBlock, p);
                                    }
                                });
                    }
                } else {
                    var uniData = StorageCacheUtils.getUniversalBlock(clickedBlock);

                    if (uniData == null) {
                        return;
                    }

                    // Fix: on some case universal block may lose its location info
                    // We added a manual patch by identify its pdc info to fix it.
                    if (uniData.getData(UniversalDataTrait.BLOCK.getReservedKey()) == null) {
                        uniData.setLastPresent(clickedBlock.getLocation());

                        if (item.isTicking()) {
                            Slimefun.getTickerTask().enableTicker(clickedBlock.getLocation(), uniData.getUUID());
                        }
                    }

                    if (uniData.isDataLoaded()) {
                        openMenu(uniData.getMenu(), clickedBlock, p);
                    } else {
                        Slimefun.getDatabaseManager()
                                .getBlockDataController()
                                .loadUniversalDataAsync(uniData, new IAsyncReadCallback<>() {
                                    @Override
                                    public boolean runOnMainThread() {
                                        return true;
                                    }

                                    @Override
                                    public void onResult(SlimefunUniversalData result) {
                                        if (!p.isOnline()) {
                                            return;
                                        }

                                        openMenu(result.getMenu(), clickedBlock, p);
                                    }
                                });
                    }
                }
            }
        } catch (Exception | LinkageError x) {
            item.error("An Exception was caught while trying to open the Inventory", x);
        }
    }

    private void openMenu(DirtyChestMenu menu, Block b, Player p) {
        if (menu != null) {
            if (p.hasPermission("slimefun.inventory.bypass") || menu.canOpen(b, p)) {
                menu.open(p);
            } else {
                Slimefun.getLocalization().sendMessage(p, "inventory.no-access", true);
            }
        }
    }
}
