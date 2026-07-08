package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.Cooler;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun4.utils.ThreadUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.Bukkit;
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
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
    // Stores the player uuid maps to the opening backpack uuid
    private final Set<UUID> openingPlayers = new HashSet<>();
    private final Map<UUID, UUID> backpacks = new HashMap<>();
    private final Map<UUID, SlimefunBackpack> backpackInstances = new HashMap<>();

    public void register(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if (e.getInventory().getHolder(false) instanceof PlayerBackpack backpack) {
            openingPlayers.remove(p.getUniqueId());
            backpacks.remove(p.getUniqueId());
            backpackInstances.remove(p.getUniqueId());
            // The changedSlot computation and refreshSnapshot is moved to the
            // ProfileDataController#saveBackpackInventory
            Slimefun.getDatabaseManager().getProfileDataController().saveBackpackInventory(backpack);
            SoundEffect.BACKPACK_CLOSE_SOUND.playFor(p);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent e) {
        if (!openingPlayers.isEmpty() && openingPlayers.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            return;
        }
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
        if (!openingPlayers.isEmpty() && openingPlayers.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            return;
        }
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

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent atEntityEvent) {
        if (!openingPlayers.isEmpty()
                && openingPlayers.contains(atEntityEvent.getPlayer().getUniqueId())) {
            atEntityEvent.setCancelled(true);
            return;
        }
        var player = atEntityEvent.getPlayer();
        if (!backpacks.containsKey(player.getUniqueId())) {
            return;
        }
        atEntityEvent.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent atEntityEvent) {
        if (!openingPlayers.isEmpty()
                && openingPlayers.contains(atEntityEvent.getPlayer().getUniqueId())) {
            atEntityEvent.setCancelled(true);
            return;
        }
        var player = atEntityEvent.getPlayer();
        if (!backpacks.containsKey(player.getUniqueId())) {
            return;
        }
        atEntityEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!openingPlayers.isEmpty()
                && openingPlayers.contains(e.getWhoClicked().getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        SlimefunBackpack slimefunBackpack =
                backpackInstances.get(e.getWhoClicked().getUniqueId());
        if (slimefunBackpack != null) {
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
                    // Fixes #3664 - Do not swap any of these backpacks to your off hand.
                    if (e.getCurrentItem() != null && SlimefunItem.getByItem(e.getCurrentItem()) == slimefunBackpack) {
                        e.setCancelled(true);
                    }
                }
            } else if (!isAllowed(slimefunBackpack, e.getCurrentItem())) {
                e.setCancelled(true);
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
        if (backpack.canUse(p, true) && !PlayerProfile.get(p, profile -> openBackpackInternal(p, item, backpack))) {
            Slimefun.getLocalization().sendMessage(p, "messages.opening-backpack");
        }
    }

    @ParametersAreNonnullByDefault
    private void openBackpackInternal(Player p, ItemStack item, SlimefunBackpack backpackItem) {
        if (item.getAmount() != 1) {
            Slimefun.getLocalization().sendMessage(p, "backpack.no-stack", true);
            return;
        }
        var meta = item.getItemMeta();
        // Check if the backpack owner is online
        if (!PlayerBackpack.isOwnerOnline(meta)) {
            Slimefun.getLocalization().sendMessage(p, "backpack.not-backpack-owner");
            return;
        }

        if (PlayerBackpack.getBackpackUUID(meta).isEmpty()
                && PlayerBackpack.getBackpackID(meta).isEmpty()) {
            // Create backpack
            Slimefun.getLocalization().sendMessage(p, "backpack.set-name", true);
            UUID puuid = p.getUniqueId();
            ItemStack itemCopy = item.clone();
            Slimefun.getChatCatcher().scheduleCatcher(puuid, name -> {
                Player player = Bukkit.getPlayer(puuid);
                // Don't let player quit server during the input
                if (player == null) return;
                var pInv = player.getInventory();
                // Check if the player change the amount of item
                if (item.getAmount() != 1) {
                    Slimefun.getLocalization().sendMessage(player, "backpack.no-stack", true);
                    return;
                }
                // Check if the item is modified during the chat input
                if (!Objects.equals(itemCopy, item)) {
                    Slimefun.getLocalization().sendMessage(player, "backpack.not-original-item", true);
                    return;
                }
                // Check if the player moves the item
                if (!item.equals(pInv.getItemInMainHand()) && !item.equals(pInv.getItemInOffHand())) {
                    Slimefun.getLocalization().sendMessage(player, "backpack.not-original-item", true);
                    return;
                }
                // Create the backpack, and bind
                PlayerProfile.get(player, profile -> {
                    PlayerBackpack.bindItem(
                            item,
                            Slimefun.getDatabaseManager()
                                    .getProfileDataController()
                                    .createBackpack(player, name, profile.nextBackpackNum(), backpackItem.getSize()));
                });
            });
            return;
        }

        /*
         * If the current Player is already viewing a backpack (for whatever reason),
         * terminate that view.
         */
        if (backpacks.containsKey(p.getUniqueId())) {
            p.closeInventory();
        }
        openingPlayers.add(p.getUniqueId());
        PlayerBackpack.getAsync(item)
                .thenAcceptAsync(
                        bp -> {
                            openingPlayers.remove(p.getUniqueId());
                            // Check if the backpack item contains invalid backpack data
                            if (bp == null || bp.isInvalid()) {
                                return;
                            }
                            // Check if someone else is currently viewing this backpack
                            if (backpacks.containsValue(bp.getUniqueId())
                                    || !bp.getInventory().getViewers().isEmpty()) {
                                Slimefun.getLocalization().sendMessage(p, "backpack.already-open", true);
                                return;
                            }
                            SoundEffect.BACKPACK_OPEN_SOUND.playAt(p.getLocation(), SoundCategory.PLAYERS);
                            bp.open(p);
                            backpacks.put(p.getUniqueId(), bp.getUniqueId());
                            backpackInstances.put(p.getUniqueId(), backpackItem);
                        },
                        ThreadUtils.getMainThreadExecutor());
    }
}
