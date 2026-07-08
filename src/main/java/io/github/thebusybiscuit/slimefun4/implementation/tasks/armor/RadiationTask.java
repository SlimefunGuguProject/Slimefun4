package io.github.thebusybiscuit.slimefun4.implementation.tasks.armor;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.api.events.AsyncPlayerRadiationLevelUpdateEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import io.github.thebusybiscuit.slimefun4.core.attributes.RadiationSymptom;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.RadioactivityListener;
import io.github.thebusybiscuit.slimefun4.utils.RadiationUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The {@link RadiationTask} handles radioactivity for
 * {@link Radioactive} items.
 *
 * @author Semisol
 */
public class RadiationTask extends AbstractArmorTask {

    private static final int GRACE_PERIOD_DURATION = Slimefun.getCfg().getInt("options.radiation-grace-period");
    private static final Map<UUID, Long> ACTIVE_GRACE_PERIODS = new HashMap<>();

    private final RadiationSymptom[] symptoms = RadiationSymptom.values();

    @Override
    @ParametersAreNonnullByDefault
    protected void onPlayerTick(Player p, PlayerProfile profile) {
        if (withinGracePeriod(p)) {
            // Player is within their grace period and shouldn't have radiation effects applied.
            return;
        }

        int exposureTotal = 0;
        if (!profile.hasFullProtectionAgainst(ProtectionType.RADIATION)
                && p.getGameMode() != GameMode.CREATIVE
                && p.getGameMode() != GameMode.SPECTATOR) {
            for (ItemStack item : p.getInventory()) {
                if (item == null || item.getType().isAir()) {
                    continue;
                }
                SlimefunItem sfItem = SlimefunItem.getByItem(item);
                if (sfItem instanceof Radioactive radioactiveItem) {
                    exposureTotal += item.getAmount()
                            * radioactiveItem.getRadioactivity().getExposureModifier();
                }
            }
            int exposureLevelBefore = RadiationUtils.getExposure(p);
            int deltaLevel = exposureTotal > 0 ? exposureTotal : (exposureLevelBefore > 0 ? -1 : 0);
            AsyncPlayerRadiationLevelUpdateEvent updateEvent =
                    new AsyncPlayerRadiationLevelUpdateEvent(p, exposureLevelBefore, deltaLevel, false);
            Bukkit.getPluginManager().callEvent(updateEvent);
            deltaLevel = updateEvent.getDeltaLevel();
            if (deltaLevel > 0) {
                if (exposureLevelBefore == 0) {
                    Slimefun.getLocalization().sendMessage(p, "messages.radiation");
                }

                RadiationUtils.addExposure(p, deltaLevel);
            } else if (exposureLevelBefore > 0 && deltaLevel < 0) {
                RadiationUtils.removeExposure(p, -deltaLevel);
            }

            int exposureLevelAfter = RadiationUtils.getExposure(p);

            Slimefun.runSync(() -> {
                for (RadiationSymptom symptom : symptoms) {
                    if (symptom.shouldApply(exposureLevelAfter)) {
                        symptom.apply(p);
                    }
                }
            });

            if (exposureLevelAfter > 0 || exposureLevelBefore > 0) {
                String msg = Slimefun.getLocalization()
                        .getMessage(p, "actionbar.radiation")
                        .replace("%level%", "" + exposureLevelAfter);
                BaseComponent[] components =
                        new ComponentBuilder().append(ChatColors.color(msg)).create();
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
            }
        } else {
            int exposureLevelBefore = RadiationUtils.getExposure(p);
            int deltaLevel = -1;
            AsyncPlayerRadiationLevelUpdateEvent updateEvent =
                    new AsyncPlayerRadiationLevelUpdateEvent(p, exposureLevelBefore, deltaLevel, true);
            Bukkit.getPluginManager().callEvent(updateEvent);
            deltaLevel = updateEvent.getDeltaLevel();
            if (deltaLevel > 0) {
                RadiationUtils.addExposure(p, deltaLevel);
            } else if (deltaLevel < 0) {
                RadiationUtils.removeExposure(p, -deltaLevel);
            }
        }
    }

    /**
     * Checks if the {@link Player} is within their grace period. A grace period is granted after death
     * to give enough time to remove the radioactive items before being killed once more, which
     * with KeepInventory on would result in a very hard-to-escape loop.
     * @see RadioactivityListener
     *
     * @param player
     *              The {@link Player} to check against.
     *
     * @return Returns true if the {@link Player} is within their grace period.
     */
    private boolean withinGracePeriod(@Nonnull Player player) {
        Long gracePeriodEnd = ACTIVE_GRACE_PERIODS.get(player.getUniqueId());

        if (gracePeriodEnd == null) {
            // No grace period present
            return false;
        } else if (gracePeriodEnd >= System.currentTimeMillis()) {
            // Player is within their grace period
            return true;
        } else {
            // A grace period was present but has since lapsed, remove the entry.
            ACTIVE_GRACE_PERIODS.remove(player.getUniqueId());
            return false;
        }
    }

    /**
     * Adds the given {@link Player}'s grace period to the collection.
     *
     * @param player The player to add the grace period to.
     */
    public static void addGracePeriod(@Nonnull Player player) {
        ACTIVE_GRACE_PERIODS.put(player.getUniqueId(), System.currentTimeMillis() + (GRACE_PERIOD_DURATION * 1000L));
    }
}
