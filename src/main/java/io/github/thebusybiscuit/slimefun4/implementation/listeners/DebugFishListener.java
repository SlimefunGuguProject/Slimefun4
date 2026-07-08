package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import city.norain.slimefun4.utils.TaskUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.LocationUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.EnergyRegulator;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * This {@link Listener} is responsible for handling our debugging tool, the debug fish.
 * This is where the functionality of this item is implemented.
 *
 * @author TheBusyBiscuit
 *
 */
public class DebugFishListener implements Listener {

    private final String greenCheckmark;
    private final String redCross;

    public DebugFishListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        greenCheckmark = "&2\u2714";
        redCross = "&4\u2718";
    }

    @EventHandler
    public void onDebug(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL || e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player p = e.getPlayer();

        if (SlimefunUtils.isItemSimilar(e.getItem(), SlimefunItems.DEBUG_FISH, true, false)) {
            e.setCancelled(true);

            if (p.hasPermission("slimefun.debugging")) {
                if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                    onLeftClick(p, e.getClickedBlock(), e);
                } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    onRightClick(p, e.getClickedBlock(), e.getBlockFace());
                }
            } else {
                Slimefun.getLocalization().sendMessage(p, "messages.no-permission", true);
            }
        }
    }

    @ParametersAreNonnullByDefault
    private void onLeftClick(Player p, Block b, PlayerInteractEvent e) {
        if (p.isSneaking()) {
            var controller = Slimefun.getDatabaseManager().getBlockDataController();
            var loc = b.getLocation();
            if (controller.getBlockDataFromCache(loc) != null) {
                controller.removeBlock(loc);
            }
        } else {
            e.setCancelled(false);
        }
    }

    @ParametersAreNonnullByDefault
    private void onRightClick(Player p, Block b, BlockFace face) {
        if (p.isSneaking()) {
            // Fixes #2655 - Delaying the placement to prevent a new event from being fired
            Slimefun.runSync(
                    () -> {
                        Block block = b.getRelative(face);
                        block.setType(Material.PLAYER_HEAD);

                        PlayerHead.setSkin(block, HeadTexture.MISSING_TEXTURE.getAsSkin(), true);
                        SoundEffect.DEBUG_FISH_CLICK_SOUND.playFor(p);
                    },
                    2L);
            return;
        }

        if (StorageCacheUtils.hasSlimefunBlock(b.getLocation())) {
            var data = StorageCacheUtils.getDataContainer(b.getLocation());

            try {
                if (data == null) {
                    TaskUtil.runSyncMethod(() -> Slimefun.getBlockDataService()
                            .getUniversalDataUUID(b)
                            .ifPresentOrElse(
                                    (uuid) -> {
                                        p.sendMessage(ChatColors.color(
                                                "&c检测到损坏的通用数据物品, UUID: " + uuid + ", 请检查数据库对应数据是否存在!"));
                                        sendVanillaInfo(p, b);
                                    },
                                    () -> sendVanillaInfo(p, b)));

                    return;
                }

                if (data.isDataLoaded()) {
                    sendInfo(p, b, data);
                } else {
                    if (data instanceof SlimefunBlockData blockData) {
                        Slimefun.getDatabaseManager()
                                .getBlockDataController()
                                .loadBlockDataAsync(blockData, new IAsyncReadCallback<>() {
                                    @Override
                                    public boolean runOnMainThread() {
                                        return true;
                                    }

                                    @Override
                                    public void onResult(SlimefunBlockData result) {
                                        sendInfo(p, b, result);
                                    }
                                });
                    } else {
                        SlimefunUniversalData universalData = (SlimefunUniversalData) data;
                        Slimefun.getDatabaseManager()
                                .getBlockDataController()
                                .loadUniversalDataAsync(universalData, new IAsyncReadCallback<>() {
                                    @Override
                                    public boolean runOnMainThread() {
                                        return true;
                                    }

                                    @Override
                                    public void onResult(SlimefunUniversalData result) {
                                        sendInfo(p, b, result);
                                    }
                                });
                    }
                }
            } catch (Exception x) {
                Slimefun.logger().log(Level.SEVERE, "An Exception occurred while using a Debug-Fish", x);
            }
        } else {
            sendVanillaInfo(p, b);
        }
    }

    private void sendVanillaInfo(Player p, Block b) {
        // Read applicable Slimefun tags
        Set<SlimefunTag> tags = EnumSet.noneOf(SlimefunTag.class);

        for (SlimefunTag tag : SlimefunTag.values()) {
            if (tag.isTagged(b.getType())) {
                tags.add(tag);
            }
        }

        if (!tags.isEmpty()) {
            p.sendMessage(" ");
            p.sendMessage(
                    ChatColors.color("&dSlimefun tags for: &e") + b.getType().name());

            for (SlimefunTag tag : tags) {
                p.sendMessage(ChatColors.color("&d* &e") + tag.name());
            }

            p.sendMessage(" ");
        }
    }

    @ParametersAreNonnullByDefault
    private void sendInfo(Player p, Block b, ASlimefunDataContainer data) {
        SlimefunItem item = SlimefunItem.getById(data.getSfId());

        p.sendMessage(" ");
        p.sendMessage(
                ChatColors.color("&d" + b.getType() + " &e@ X: " + b.getX() + " Y: " + b.getY() + " Z: " + b.getZ()));
        p.sendMessage(ChatColors.color("&dId: " + "&e" + item.getId()));
        p.sendMessage(ChatColors.color("&dPlugin: " + "&e" + item.getAddon().getName()));

        if (b.getState() instanceof Skull) {
            p.sendMessage(ChatColors.color("&dSkull: " + greenCheckmark));

            // Check if the skull is a wall skull, and if so use Directional instead of Rotatable.
            if (b.getType() == Material.PLAYER_WALL_HEAD) {
                p.sendMessage(ChatColors.color("  &dFacing: &e" + ((Directional) b.getBlockData()).getFacing()));
            } else {
                p.sendMessage(ChatColors.color("  &dRotation: &e" + ((Rotatable) b.getBlockData()).getRotation()));
            }
        }

        if ((data instanceof SlimefunBlockData bd && bd.getBlockMenu() != null)
                || (data instanceof SlimefunUniversalData ud && ud.getMenu() != null)) {
            p.sendMessage(ChatColors.color("&dInventory: " + greenCheckmark));
        } else {
            p.sendMessage(ChatColors.color("&dInventory: " + redCross));
        }

        if (data instanceof SlimefunUniversalData universalData) {
            p.sendMessage(ChatColors.color("&dUniversal Item: " + greenCheckmark));
            p.sendMessage(ChatColors.color("    &dUUID: " + universalData.getUUID()));
            p.sendMessage(ChatColors.color("    &dTrait: " + universalData.getTraits()));
        }

        if (item.isTicking()) {
            p.sendMessage(ChatColors.color("&dTicker: " + greenCheckmark));
            p.sendMessage(ChatColors.color(
                    "  &dAsync: &e" + (item.getBlockTicker().isSynchronized() ? redCross : greenCheckmark)));
        } else if (item instanceof EnergyNetProvider) {
            p.sendMessage(ChatColors.color("&dTicker: &3Indirect (Generator)"));
        } else {
            p.sendMessage(ChatColors.color("&dTicker: " + redCross));
        }

        Slimefun.getTickerTask().getTickLocations(p.getLocation().getChunk()).stream()
                .filter(l -> l.getLocation().equals(b.getLocation()))
                .findFirst()
                .ifPresent(tickLoc -> p.sendMessage(ChatColors.color(
                        "&dIn Ticker Queue " + (tickLoc.isUniversal() ? "(Universal)" : "") + ": " + greenCheckmark)));

        if (Slimefun.getProfiler().hasTimings(b)) {
            p.sendMessage(
                    ChatColors.color("  &dTimings: &e" + Slimefun.getProfiler().getTime(b)));
            p.sendMessage(ChatColors.color(
                    "  &dTotal Timings: &e" + Slimefun.getProfiler().getTime(item)));
            p.sendMessage(ChatColors.color(
                    "  &dChunk Timings: &e" + Slimefun.getProfiler().getTime(b.getChunk())));
        }

        if (item instanceof EnergyRegulator) {
            p.sendMessage(ChatColors.color("&dEnergy Regulator"));
            EnergyNet network = EnergyNet.getNetworkFromLocationOrCreate(b.getLocation());
            p.sendMessage(ChatColors.color("&dNetwork range: " + network.getRange()));
            p.sendMessage(ChatColors.color("&dNetwork components:"));
            p.sendMessage(ChatColors.color("  &d- Network capacitors:"));
            network.getCapacitors()
                    .forEach((loc, component) -> p.sendMessage(
                            ChatColors.color("&d " + component.getId() + " - " + LocationUtils.locationToString(loc))));
            p.sendMessage(ChatColors.color("  &d- Network consumers:"));
            network.getConsumers()
                    .forEach((loc, component) -> p.sendMessage(
                            ChatColors.color("&d " + component.getId() + " - " + LocationUtils.locationToString(loc))));
            p.sendMessage(ChatColors.color("  &d- Network generators:"));
            network.getGenerators()
                    .forEach((loc, component) -> p.sendMessage(
                            ChatColors.color("&d " + component.getId() + " - " + LocationUtils.locationToString(loc))));
        }

        if (item instanceof EnergyNetComponent component) {
            p.sendMessage(ChatColors.color("&dEnergyNet Component"));
            p.sendMessage(ChatColors.color("  &dType: &e" + component.getEnergyComponentType()));

            if (component.isChargeable()) {
                p.sendMessage(ChatColors.color("  &dChargeable: " + greenCheckmark));
                p.sendMessage(ChatColors.color("  &dEnergy: &e" + component.getChargeLong(b.getLocation()) + " / "
                        + component.getCapacityLong()));
            } else {
                p.sendMessage(ChatColors.color("&dChargeable: " + redCross));
            }
        }

        data.getAllData().forEach((k, v) -> p.sendMessage(ChatColors.color("&6" + k + ": " + v)));
        p.sendMessage(" ");
    }
}
