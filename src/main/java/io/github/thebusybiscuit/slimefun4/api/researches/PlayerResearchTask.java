package io.github.thebusybiscuit.slimefun4.api.researches;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;
import io.github.thebusybiscuit.slimefun4.api.events.ResearchUnlockEvent;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.options.SlimefunGuideSettings;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.FireworkUtils;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * A {@link PlayerResearchTask} is run when a {@link Player} unlocks a {@link Research}.
 *
 * @author TheBusyBiscuit
 *
 * @see Research
 * @see ResearchUnlockEvent
 * @see PlayerProfile
 *
 */
@SlimefunAPI
public class PlayerResearchTask implements Consumer<PlayerProfile> {

    private static final int[] RESEARCH_PROGRESS = {23, 44, 57, 92};
    private static final String PLACEHOLDER = "%research%";

    private final Research research;
    private final boolean isInstant;
    private final Consumer<Player> callback;

    /**
     * This constructs a new {@link PlayerResearchTask}.
     *
     * @param research
     *            The {@link Research} to unlock
     * @param isInstant
     *            Whether to unlock this {@link Research} instantaneously
     * @param callback
     *            The callback to run when the task has completed
     */
    PlayerResearchTask(@Nonnull Research research, boolean isInstant, @Nullable Consumer<Player> callback) {
        Validate.notNull(research, "The Research must not be null");

        this.research = research;
        this.isInstant = isInstant;
        this.callback = callback;
    }

    @Override
    public void accept(PlayerProfile profile) {
        if (profile.hasUnlocked(research)) {
            return;
        }

        Player player = profile.getPlayer();
        if (player != null) {
            Slimefun.runSyncFor(player, () -> beginResearch(profile, player));
        }
    }

    private void beginResearch(@Nonnull PlayerProfile profile, @Nonnull Player p) {
        // Re-check on the entity-owned thread in case another request completed first.
        if (profile.hasUnlocked(research) || !p.isValid()) {
            return;
        }

        if (!isInstant) {
            Slimefun.runSyncFor(
                    p,
                    () -> {
                        SoundEffect.PLAYER_RESEARCHING_SOUND.playFor(p);
                        Slimefun.getLocalization()
                                .sendMessage(
                                        p,
                                        "messages.research.progress",
                                        true,
                                        msg -> msg.replace(PLACEHOLDER, research.getName(p))
                                                .replace("%progress%", "0%"));
                    },
                    5L);
        }

        ResearchUnlockEvent event = new ResearchUnlockEvent(p, research);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        if (isInstant) {
            unlockResearch(p, profile);
        } else if (Slimefun.getRegistry()
                .getCurrentlyResearchingPlayers()
                .add(p.getUniqueId())) {
            Slimefun.getLocalization()
                    .sendMessage(
                            p,
                            "messages.research.start",
                            true,
                            msg -> msg.replace(PLACEHOLDER, research.getName(p)));
            sendUpdateMessage(p);

            Runnable clearResearchState = () -> Slimefun.getRegistry()
                    .getCurrentlyResearchingPlayers()
                    .remove(p.getUniqueId());
            Slimefun.runSyncFor(
                    p,
                    () -> {
                        try {
                            if (p.isValid()) {
                                unlockResearch(p, profile);
                            }
                        } finally {
                            clearResearchState.run();
                        }
                    },
                    clearResearchState,
                    (RESEARCH_PROGRESS.length + 1) * 20L);
        }
    }

    private void sendUpdateMessage(@Nonnull Player p) {
        for (int i = 1; i < RESEARCH_PROGRESS.length + 1; i++) {
            int index = i;

            Slimefun.runSyncFor(
                    p,
                    () -> {
                        SoundEffect.PLAYER_RESEARCHING_SOUND.playFor(p);

                        Slimefun.getLocalization().sendMessage(p, "messages.research.progress", true, msg -> {
                            String progress = RESEARCH_PROGRESS[index - 1] + "%";
                            return msg.replace(PLACEHOLDER, research.getName(p)).replace("%progress%", progress);
                        });
                    },
                    i * 20L);
        }
    }

    private void unlockResearch(@Nonnull Player p, @Nonnull PlayerProfile profile) {
        profile.setResearched(research, true);
        Slimefun.getLocalization()
                .sendMessage(p, "messages.unlocked", true, msg -> msg.replace(PLACEHOLDER, research.getName(p)));
        onFinish(p);

        // Check if the Server and the Player have enabled fireworks for researches
        if (Slimefun.getConfigManager().isResearchFireworkEnabled() && SlimefunGuideSettings.hasFireworksEnabled(p)) {
            FireworkUtils.launchRandom(p, 1);
        }
    }

    /**
     * This method is called when the {@link Research} successfully finished to unlock.
     *
     * @param p
     *            The {@link Player} who has unlocked this {@link Research}
     */
    private void onFinish(@Nonnull Player p) {
        if (callback != null) {
            callback.accept(p);
        }
    }
}
