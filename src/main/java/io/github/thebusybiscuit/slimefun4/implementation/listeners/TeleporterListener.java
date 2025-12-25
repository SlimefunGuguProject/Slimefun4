package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.elevator.ElevatorPlate;
import io.github.thebusybiscuit.slimefun4.implementation.items.teleporter.AbstractTeleporterPlate;
import io.github.thebusybiscuit.slimefun4.implementation.items.teleporter.Teleporter;
import io.github.thebusybiscuit.slimefun4.implementation.items.teleporter.TeleporterPylon;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * This {@link Listener} is responsible for the {@link Teleporter} (and {@link ElevatorPlate}).
 *
 * @author TheBusyBiscuit
 * @author Walshy
 * @author Sfiguz7
 * @author SoSeDiK
 *
 */
public class TeleporterListener implements Listener {

    // @formatter:off
    private final BlockFace[] faces = {
        BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST,
        BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST
    };

    // @formatter:on

    public TeleporterListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPressurePlateEnter(PlayerInteractEvent e) {
        if (e.getAction() != Action.PHYSICAL || e.getClickedBlock() == null) {
            return;
        }

        Block b = e.getClickedBlock();
        SlimefunItem item = StorageCacheUtils.getSlimefunItem(b.getLocation());
        Player p = e.getPlayer();

        // Fixes #2966 - Check if Players can use these
        if (item == null || !item.canUse(p, true)) {
            return;
        }

        if (item instanceof ElevatorPlate) {
            // Pressure plate was an elevator
            ElevatorPlate elevator = SlimefunItems.ELEVATOR_PLATE.getItem(ElevatorPlate.class);
            elevator.openInterface(p, b);
        } else if (item instanceof AbstractTeleporterPlate teleporterPlate && teleporterPlate.hasAccess(p, b)) {
            // Pressure plate was a teleporter
            var blockData =
                    StorageCacheUtils.getBlock(b.getRelative(BlockFace.DOWN).getLocation());
            if (blockData == null) {
                return;
            }
            SlimefunItem teleporter = SlimefunItem.getById(blockData.getSfId());

            if (teleporter instanceof Teleporter && checkForPylons(b.getRelative(BlockFace.DOWN))) {
                Block block = b.getRelative(BlockFace.DOWN);
                if (blockData.isDataLoaded()) {
                    teleport(blockData.getData("owner"), p, block);
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
                                    teleport(blockData.getData("owner"), p, block);
                                }
                            });
                }
            }
        }
    }

    private void teleport(String ownerUid, Player p, Block b) {
        Slimefun.getGPSNetwork().getTeleportationManager().openTeleporterGUI(p, UUID.fromString(ownerUid), b);
    }

    /**
     * This methoc checks if the given teleporter {@link Block} is surrounded
     * by all the necessary {@link TeleporterPylon}s.
     *
     * @param teleporter
     *            The teleporter {@link Block}
     *
     * @return Whether the teleporter is surrounded by pylons.
     */
    private boolean checkForPylons(@Nonnull Block teleporter) {
        for (BlockFace face : faces) {
            if (!(StorageCacheUtils.getSlimefunItem(teleporter.getRelative(face).getLocation())
                    instanceof TeleporterPylon)) {
                return false;
            }
        }

        return true;
    }
}
