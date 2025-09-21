package io.github.thebusybiscuit.slimefun4.core.services;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Iterator;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * This Service is responsible for automatically saving {@link Player} and {@link Block}
 * data.
 *
 * @author TheBusyBiscuit
 *
 */
public class AutoSavingService {

    private int interval;

    /**
     * This method starts the {@link AutoSavingService} with the given interval.
     *
     * @param plugin
     *            The current instance of Slimefun
     * @param interval
     *            The interval in which to run this task
     */
    public void start(@Nonnull Slimefun plugin, int interval) {
        this.interval = interval;

        plugin.getServer().getScheduler().runTaskTimer(plugin, this::saveAllPlayers, 2000L, interval * 60L * 20L);
        plugin.getServer()
                .getScheduler()
                .runTaskTimerAsynchronously(
                        plugin,
                        () -> {
                            Slimefun.getDatabaseManager()
                                    .getBlockDataController()
                                    .saveAllBlockInventories();

                            Slimefun.getDatabaseManager()
                                    .getBlockDataController()
                                    .saveAllUniversalInventories();
                        },
                        2000L,
                        interval * 60L * 20L);
    }

    /**
     * This method saves every {@link PlayerProfile} in memory and removes profiles
     * that were marked for deletion.
     */
    private void saveAllPlayers() {
        Iterator<PlayerProfile> iterator = PlayerProfile.iterator();
        int players = 0;

        while (iterator.hasNext()) {
            PlayerProfile profile = iterator.next();

            if (profile.isDirty()) {
                players++;
                profile.save();
            }

            if (profile.isMarkedForDeletion()) {
                iterator.remove();
            }
        }

        if (players > 0) {
            Slimefun.logger().log(Level.INFO, "成功保存了 {0} 个玩家的数据!", players);
        }
    }
}
