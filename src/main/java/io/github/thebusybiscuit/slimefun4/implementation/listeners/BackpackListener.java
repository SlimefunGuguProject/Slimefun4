package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.InvStorageUtils;
import io.github.bakedlibs.dough.collections.Pair;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.Cooler;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link Listener} is responsible for all events centered around a {@link SlimefunBackpack}.
 * This also includes the {@link Cooler}
 *
 * @author TheBusyBiscuit
 * @author Walshy
 * @author NihilistBrew
 * @author AtomicScience
 * @author VoidAngel
 * @author John000708
 *
 * @see SlimefunBackpack
 * @see PlayerBackpack
 *
 */
public class BackpackListener implements Listener {

    private final Map<UUID, ItemStack> backpacks = new HashMap<>();
    private final Map<UUID, List<Pair<ItemStack, Integer>>> invSnapshot = new HashMap<>();

    public void register(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if (e.getInventory().getHolder(false) instanceof PlayerBackpack backpack) {
            backpacks.remove(p.getUniqueId());
            saveBackpackInv(backpack);
            SoundEffect.BACKPACK_CLOSE_SOUND.playFor(p);
        }
    }

    private void saveBackpackInv(PlayerBackpack bp) {
        var snapshot = invSnapshot.remove(bp.getUniqueId());
        if (snapshot == null) {
            return;
        }

        var changed =
                InvStorageUtils.getChangedSlots(snapshot, bp.getInventory().getContents());
        if (changed.isEmpty()) {
            return;
        }
        Slimefun.getDatabaseManager().getProfileDataController().saveBackpackInventory(bp, changed);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (backpacks.containsKey(e.getPlayer().getUniqueId())) {
            ItemStack item = e.getItemDrop().getItemStack();
            SlimefunItem sfItem = SlimefunItem.getByItem(item);

            if (sfItem instanceof SlimefunBackpack) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerSwap(PlayerSwapHandItemsEvent e) {
        var player = e.getPlayer();
        if (!backpacks.containsKey(player.getUniqueId())) {
            return;
        }

        ItemStack item = player.getInventory().getItemInOffHand();
        if (item == null || item.getType().isAir()) {
            return;
        }

        SlimefunItem backpack = SlimefunItem.getByItem(item);
        if (backpack instanceof SlimefunBackpack) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        ItemStack item = backpacks.get(e.getWhoClicked().getUniqueId());

        if (item != null) {
            SlimefunItem backpack = SlimefunItem.getByItem(item);

            if (backpack instanceof SlimefunBackpack slimefunBackpack) {
                if (e.getClick() == ClickType.NUMBER_KEY) {
                    // Prevent disallowed items from being moved using number keys.
                    if (e.getClickedInventory().getType() != InventoryType.PLAYER) {
                        ItemStack hotbarItem = e.getWhoClicked().getInventory().getItem(e.getHotbarButton());

                        if (!isAllowed(slimefunBackpack, hotbarItem)) {
                            e.setCancelled(true);
                        }
                    }
                } else if (e.getClick() == ClickType.SWAP_OFFHAND) {
                    if (e.getClickedInventory().getType() != InventoryType.PLAYER) {
                        // Fixes #3265 - Don't move disallowed items using the off hand.
                        ItemStack offHandItem = e.getWhoClicked().getInventory().getItemInOffHand();

                        if (!isAllowed(slimefunBackpack, offHandItem)) {
                            e.setCancelled(true);
                        }
                    } else {
                        // Fixes #3664 - Do not swap the backpack to your off hand.
                        if (e.getCurrentItem() != null && e.getCurrentItem().isSimilar(item)) {
                            e.setCancelled(true);
                        }
                    }
                } else if (!isAllowed(slimefunBackpack, e.getCurrentItem())) {
                    e.setCancelled(true);
                }
            }
        }
    }

    private boolean isAllowed(@Nonnull SlimefunBackpack backpack, @Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return true;
        }

        return backpack.isItemAllowed(item, SlimefunItem.getByItem(item));
    }

    @ParametersAreNonnullByDefault
    public void openBackpack(Player p, ItemStack item, SlimefunBackpack backpack) {
        if (item.getAmount() == 1) {
            if (backpack.canUse(p, true)
                    && !PlayerProfile.get(p, profile -> openBackpack(p, item, profile, backpack.getSize()))) {
                Slimefun.getLocalization().sendMessage(p, "messages.opening-backpack");
            }
        } else {
            Slimefun.getLocalization().sendMessage(p, "backpack.no-stack", true);
        }
    }

    @ParametersAreNonnullByDefault
    private void openBackpack(Player p, ItemStack item, PlayerProfile profile, int size) {
        var meta = item.getItemMeta();
        if (PlayerBackpack.getBackpackUUID(meta).isEmpty()
                && PlayerBackpack.getBackpackID(meta).isEmpty()) {
            // Create backpack
            Slimefun.getLocalization().sendMessage(p, "backpack.set-name", true);
            Slimefun.getChatCatcher().scheduleCatcher(p.getUniqueId(), name -> {
                var pInv = p.getInventory();
                if (!item.equals(pInv.getItemInMainHand()) && !item.equals(pInv.getItemInOffHand())) {
                    Slimefun.getLocalization().sendMessage(p, "backpack.not-original-item", true);
                    return;
                }
                if (item.getAmount() > 1) {
                    Slimefun.getLocalization().sendMessage(p, "backpack.no-stack", true);
                    return;
                }
                PlayerBackpack.bindItem(
                        item,
                        Slimefun.getDatabaseManager()
                                .getProfileDataController()
                                .createBackpack(p, name, profile.nextBackpackNum(), size));
            });
        }

        /*
         * If the current Player is already viewing a backpack (for whatever reason),
         * terminate that view.
         */
        if (backpacks.containsKey(p.getUniqueId())) {
            p.closeInventory();
        }

        // Check if someone else is currently viewing this backpack
        if (!backpacks.containsValue(item)) {
            SoundEffect.BACKPACK_OPEN_SOUND.playAt(p.getLocation(), SoundCategory.PLAYERS);
            PlayerBackpack.getAsync(
                    item,
                    backpack -> {
                        // fix the issue #978 dupe with fast-click backpack
                        backpack.open(p);
                        backpacks.put(p.getUniqueId(), item);
                        invSnapshot.put(
                                backpack.getUniqueId(),
                                InvStorageUtils.getInvSnapshot(
                                        backpack.getInventory().getContents()));
                    },
                    true);

        } else {
            Slimefun.getLocalization().sendMessage(p, "backpack.already-open", true);
        }
    }
}
