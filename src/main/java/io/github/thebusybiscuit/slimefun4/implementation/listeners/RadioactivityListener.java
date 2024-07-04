package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.tasks.armor.RadiationTask;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;
import javax.annotation.Nonnull;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * {@link RadioactivityListener} handles radioactivity level resets
 * on death
 *
 * @author Semisol
 */
public class RadioactivityListener implements Listener {

    public RadioactivityListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(@Nonnull PlayerDeathEvent e) {
        addGracePeriod(e.getEntity());
    }

    @EventHandler
    public void onPlayerJoin(@Nonnull PlayerJoinEvent e) {
        addGracePeriod(e.getPlayer());
    }

    private void addGracePeriod(@Nonnull Player entity) {
        RadiationUtils.clearExposure(entity);
        RadiationTask.addGracePeriod(entity);
    }

    @EventHandler
    public void onPlayerJoin(@Nonnull PlayerJoinEvent e) {
        RadiationUtils.clearExposure(e.getPlayer());
        RadiationTask.addGracePeriod(e.getPlayer());
    }
}
