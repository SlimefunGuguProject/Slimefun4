package io.github.thebusybiscuit.slimefun4.api.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.events.AsyncProfileLoadEvent;
import io.github.thebusybiscuit.slimefun4.api.gps.Waypoint;
import io.github.thebusybiscuit.slimefun4.api.items.HashedArmorpiece;
import io.github.thebusybiscuit.slimefun4.api.items.ItemState;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectiveArmor;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * A class that can store a Player's {@link Research} progress for caching purposes.
 * It also holds the backpacks of a {@link Player}.
 *
 * @author TheBusyBiscuit
 *
 * @see Research
 * @see Waypoint
 * @see HashedArmorpiece
 *
 */
public class PlayerProfile {

    private final OfflinePlayer owner;
    private int backpackNum;
    private final Config waypointsFile;

    private boolean dirty = false;
    private boolean isInvalid = false;
    private boolean markedForDeletion = false;

    private final Set<Research> researches;
    private final List<Waypoint> waypoints = new ArrayList<>();
    private final GuideHistory guideHistory = new GuideHistory(this);

    private final HashedArmorpiece[] armor = {
        new HashedArmorpiece(), new HashedArmorpiece(), new HashedArmorpiece(), new HashedArmorpiece()
    };

    public PlayerProfile(@Nonnull OfflinePlayer p, int backpackNum) {
        this(p, backpackNum, new HashSet<>());
    }

    public PlayerProfile(@Nonnull OfflinePlayer p, int backpackNum, Set<Research> researches) {
        owner = p;
        this.backpackNum = backpackNum;
        this.researches = researches;

        waypointsFile = new Config("data-storage/Slimefun/waypoints/" + p.getUniqueId() + ".yml");
        loadWaypoint();
    }

    private void loadWaypoint() {
        for (String key : waypointsFile.getKeys()) {
            try {
                if (waypointsFile.contains(key + ".world")
                        && Bukkit.getWorld(waypointsFile.getString(key + ".world")) != null) {
                    String waypointName = waypointsFile.getString(key + ".name");
                    Location loc = waypointsFile.getLocation(key);
                    waypoints.add(new Waypoint(this, key, loc, waypointName));
                }
            } catch (Exception x) {
                Slimefun.logger()
                        .log(
                                Level.WARNING,
                                x,
                                () -> "Could not load Waypoint \"" + key + "\" for Player \"" + owner.getName() + '"');
            }
        }
    }

    /**
     * This method provides a fast way to access the armor of a {@link Player}.
     * It returns a cached version, represented by {@link HashedArmorpiece}.
     *
     * @return The cached armor for this {@link Player}
     */
    public @Nonnull HashedArmorpiece[] getArmor() {
        return armor;
    }

    /**
     * This returns the {@link UUID} this {@link PlayerProfile} is linked to.
     *
     * @return The {@link UUID} of our {@link PlayerProfile}
     */
    public @Nonnull UUID getUUID() {
        return owner.getUniqueId();
    }

    /**
     * This method returns whether the {@link Player} has logged off.
     * If this is true, then the Profile can be removed from RAM.
     *
     * @return Whether the Profile is marked for deletion
     */
    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    /**
     * This method returns whether the Profile has unsaved changes
     *
     * @return Whether there are unsaved changes
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * This method will save the Player's Researches and Backpacks to the hard drive
     */
    public void save() {
        // As waypoints still store in file, just keep this method here for now...
        waypointsFile.save();
        dirty = false;
    }

    /**
     * This method sets the Player's "researched" status for this Research.
     * Use the boolean to unlock or lock the {@link Research}
     *
     * @param research
     *            The {@link Research} that should be unlocked or locked
     * @param unlock
     *            Whether the {@link Research} should be unlocked or locked
     */
    public void setResearched(@Nonnull Research research, boolean unlock) {
        Validate.notNull(research, "Research must not be null!");
        dirty = true;

        if (unlock) {
            researches.add(research);
        } else {
            researches.remove(research);
        }
        Slimefun.getDatabaseManager()
                .getProfileDataController()
                .setResearch(owner.getUniqueId().toString(), research.getKey(), unlock);
    }

    /**
     * This method returns whether the {@link Player} has unlocked the given {@link Research}
     *
     * @param research
     *            The {@link Research} that is being queried
     *
     * @return Whether this {@link Research} has been unlocked
     */
    public boolean hasUnlocked(@Nullable Research research) {
        if (research == null) {
            // No Research, no restriction
            return true;
        }

        return !research.isEnabled() || researches.contains(research);
    }

    /**
     * This method returns whether this {@link Player} has unlocked all {@link Research Researches}.
     *
     * @return Whether they unlocked every {@link Research}
     */
    public boolean hasUnlockedEverything() {
        for (Research research : Slimefun.getRegistry().getResearches()) {
            // If there is a single Research not unlocked: They haven't unlocked everything.
            if (!hasUnlocked(research)) {
                return false;
            }
        }

        // Player has everything unlocked - Hooray!
        return true;
    }

    /**
     * This Method will return all Researches that this {@link Player} has unlocked
     *
     * @return A {@code Hashset<Research>} of all Researches this {@link Player} has unlocked
     */
    public @Nonnull Set<Research> getResearches() {
        return ImmutableSet.copyOf(researches);
    }

    /**
     * This returns a {@link List} of all {@link Waypoint Waypoints} belonging to this
     * {@link PlayerProfile}.
     *
     * @return A {@link List} containing every {@link Waypoint}
     */
    public @Nonnull List<Waypoint> getWaypoints() {
        return ImmutableList.copyOf(waypoints);
    }

    /**
     * This adds the given {@link Waypoint} to the {@link List} of {@link Waypoint Waypoints}
     * of this {@link PlayerProfile}.
     *
     * @param waypoint
     *            The {@link Waypoint} to add
     */
    public void addWaypoint(@Nonnull Waypoint waypoint) {
        Validate.notNull(waypoint, "Cannot add a 'null' waypoint!");

        for (Waypoint wp : waypoints) {
            if (wp.getId().equals(waypoint.getId())) {
                throw new IllegalArgumentException("A Waypoint with that id already exists for this Player");
            }
        }

        if (waypoints.size() < 21) {
            waypoints.add(waypoint);

            waypointsFile.setValue(waypoint.getId(), waypoint.getLocation());
            waypointsFile.setValue(waypoint.getId() + ".name", waypoint.getName());
            markDirty();
        }
    }

    /**
     * This removes the given {@link Waypoint} from the {@link List} of {@link Waypoint Waypoints}
     * of this {@link PlayerProfile}.
     *
     * @param waypoint
     *            The {@link Waypoint} to remove
     */
    public void removeWaypoint(@Nonnull Waypoint waypoint) {
        Validate.notNull(waypoint, "Cannot remove a 'null' waypoint!");

        if (waypoints.remove(waypoint)) {
            waypointsFile.setValue(waypoint.getId(), null);
            markDirty();
        }
    }

    /**
     * Call this method if the Player has left.
     * The profile can then be removed from RAM.
     */
    public final void markForDeletion() {
        markedForDeletion = true;
    }

    /**
     * Call this method if this Profile has unsaved changes.
     */
    public final void markDirty() {
        dirty = true;
    }

    public int nextBackpackNum() {
        backpackNum++;
        Slimefun.getDatabaseManager().getProfileDataController().saveProfileBackpackCount(this);
        return backpackNum;
    }

    public int getBackpackCount() {
        return backpackNum;
    }

    public void setBackpackCount(int count) {
        backpackNum = Math.max(backpackNum, count);
        Slimefun.getDatabaseManager().getProfileDataController().saveProfileBackpackCount(this);
    }

    private int countNonEmptyResearches(@Nonnull Collection<Research> researches) {
        int count = 0;
        for (Research research : researches) {
            if (research.hasEnabledItems()) {
                count++;
            }
        }
        return count;
    }

    /**
     * This method gets the research title, as defined in {@code config.yml},
     * of this {@link PlayerProfile} based on the fraction
     * of unlocked {@link Research}es of this player.
     *
     * @return The research title of this {@link PlayerProfile}
     */
    public @Nonnull String getTitle() {
        List<String> titles = Slimefun.getRegistry().getResearchRanks();

        int allResearches = countNonEmptyResearches(Slimefun.getRegistry().getResearches());
        float fraction = (float) countNonEmptyResearches(researches) / allResearches;
        int index = (int) (fraction * (titles.size() - 1));

        return titles.get(index);
    }

    /**
     * This sends the statistics for the specified {@link CommandSender}
     * to the {@link CommandSender}. This includes research title, research progress
     * and total xp spent.
     *
     * @param sender The {@link CommandSender} for which to get the statistics and send them to.
     */
    public void sendStats(@Nonnull CommandSender sender) {
        int unlockedResearches = countNonEmptyResearches(getResearches());
        int levels = getResearches().stream().mapToInt(Research::getLevelCost).sum();
        int allResearches = countNonEmptyResearches(Slimefun.getRegistry().getResearches());

        float progress = Math.round(((unlockedResearches * 100.0F) / allResearches) * 100.0F) / 100.0F;

        sender.sendMessage("");
        sender.sendMessage(ChatColors.color("&7玩家研究统计: &b" + owner.getName()));
        sender.sendMessage("");
        sender.sendMessage(ChatColors.color("&7研究等级: " + ChatColor.AQUA + getTitle()));
        sender.sendMessage(ChatColors.color("&7研究进度: "
                + NumberUtils.getColorFromPercentage(progress)
                + progress
                + " &r% "
                + ChatColor.YELLOW
                + '('
                + unlockedResearches
                + " / "
                + allResearches
                + ')'));
        sender.sendMessage(ChatColors.color("&7总花费经验等级: " + ChatColor.AQUA + levels));
    }

    /**
     * This returns the {@link Player} who this {@link PlayerProfile} belongs to.
     * If the {@link Player} is offline, null will be returned.
     *
     * @return The {@link Player} of this {@link PlayerProfile} or null
     */
    public @Nullable Player getPlayer() {
        return owner.getPlayer();
    }

    /**
     * This returns the {@link GuideHistory} of this {@link Player}.
     * It is basically that player's browsing history.
     *
     * @return The {@link GuideHistory} of this {@link Player}
     */
    public @Nonnull GuideHistory getGuideHistory() {
        return guideHistory;
    }

    public static boolean fromUUID(@Nonnull UUID uuid, @Nonnull Consumer<PlayerProfile> callback) {
        return get(Bukkit.getOfflinePlayer(uuid), callback);
    }

    /**
     * Get the {@link PlayerProfile} for a {@link OfflinePlayer} asynchronously.
     *
     * @param p
     *            The {@link OfflinePlayer} who's {@link PlayerProfile} to retrieve
     * @param callback
     *            The callback with the {@link PlayerProfile}
     *
     * @return If the {@link OfflinePlayer} was cached or not.
     */
    public static boolean get(@Nonnull OfflinePlayer p, @Nonnull Consumer<PlayerProfile> callback) {
        Validate.notNull(p, "Cannot get a PlayerProfile for: null!");

        UUID uuid = p.getUniqueId();
        PlayerProfile profile = Slimefun.getRegistry().getPlayerProfiles().get(uuid);

        if (profile != null && !profile.isInvalid) {
            callback.accept(profile);
            return true;
        }

        getOrCreate(p, callback);
        return false;
    }

    /**
     * This requests an instance of {@link PlayerProfile} to be loaded for the given {@link OfflinePlayer}.
     * This method will return true if the {@link PlayerProfile} was already found.
     *
     * @param p
     *            The {@link OfflinePlayer} to request the {@link PlayerProfile} for.
     *
     * @return Whether the {@link PlayerProfile} was already loaded
     */
    public static boolean request(@Nonnull OfflinePlayer p) {
        Validate.notNull(p, "Cannot request a Profile for null");

        var profile = Slimefun.getRegistry().getPlayerProfiles().get(p.getUniqueId());
        if (profile == null || profile.isInvalid) {
            // Should probably prevent multiple requests for the same profile in the future
            getOrCreate(p, null);
            return false;
        }

        return true;
    }

    /**
     * This method tries to search for a {@link PlayerProfile} of the given {@link OfflinePlayer}.
     * The result of this method is an {@link Optional}, if no {@link PlayerProfile} was found, an empty
     * {@link Optional} will be returned.
     *
     * @param p
     *            The {@link OfflinePlayer} to get the {@link PlayerProfile} for
     *
     * @return An {@link Optional} describing the result
     */
    public static @Nonnull Optional<PlayerProfile> find(@Nonnull OfflinePlayer p) {
        var re = Slimefun.getRegistry().getPlayerProfiles().get(p.getUniqueId());
        if (re == null || re.isInvalid) {
            return Optional.empty();
        }
        return Optional.of(re);
    }

    public static @Nonnull Iterator<PlayerProfile> iterator() {
        return Slimefun.getRegistry().getPlayerProfiles().values().iterator();
    }

    public boolean hasFullProtectionAgainst(@Nonnull ProtectionType type) {
        Validate.notNull(type, "ProtectionType must not be null.");

        int armorCount = 0;
        NamespacedKey setId = null;

        for (HashedArmorpiece armorpiece : armor) {
            Optional<SlimefunArmorPiece> armorPiece = armorpiece.getItem();
            if (armorPiece.isPresent() && armorPiece.get() instanceof ProtectiveArmor protectiveArmor) {
                for (ProtectionType protectionType : protectiveArmor.getProtectionTypes()) {
                    if (protectionType == type) {
                        if (!protectiveArmor.isFullSetRequired()) {
                            return true;
                        } else if (setId == null || setId.equals(protectiveArmor.getArmorSetId())) {
                            armorCount++;
                            setId = protectiveArmor.getArmorSetId();
                        }
                    }
                }
            }
        }

        return armorCount == 4;
    }

    @Override
    public int hashCode() {
        return owner.getUniqueId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PlayerProfile profile && owner.getUniqueId().equals(profile.owner.getUniqueId());
    }

    @Override
    public String toString() {
        return "PlayerProfile {" + owner.getUniqueId() + "}";
    }

    public OfflinePlayer getOwner() {
        return owner;
    }

    public void markInvalid() {
        isInvalid = true;
    }

    public boolean isInvalid() {
        return isInvalid;
    }

    // returns the amount of researches with at least 1 enabled item
    private int nonEmptyResearches() {
        return (int) Slimefun.getRegistry().getResearches().stream()
                .filter(research ->
                        research.getAffectedItems().stream().anyMatch(item -> item.getState() == ItemState.ENABLED))
                .count();
    }

    private static void getOrCreate(OfflinePlayer p, Consumer<PlayerProfile> cb) {
        var controller = Slimefun.getDatabaseManager().getProfileDataController();
        controller.getProfileAsync(p, new IAsyncReadCallback<>() {
            @Override
            public void onResult(PlayerProfile result) {
                invokeCb(result);
            }

            @Override
            public void onResultNotFound() {
                invokeCb(controller.createProfile(p));
            }

            private void invokeCb(PlayerProfile pf) {
                AsyncProfileLoadEvent event = new AsyncProfileLoadEvent(pf);
                Bukkit.getPluginManager().callEvent(event);

                Slimefun.getRegistry().getPlayerProfiles().put(p.getUniqueId(), event.getProfile());
                if (cb != null) {
                    cb.accept(event.getProfile());
                }
            }
        });
    }
}
