package io.github.thebusybiscuit.slimefun4.implementation.items.androids.menu;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.skins.PlayerHead;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.androids.ProgrammableAndroid;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * The {@link AndroidShareMenu} is responsibility to modify trusted users
 * in {@link ProgrammableAndroid}.
 *
 * @author StarWishsama
 * @see ProgrammableAndroid
 */
public final class AndroidShareMenu {
    private static final int DISPLAY_START_SLOT = 9;
    private static final NamespacedKey BLOCK_INFO_KEY = new NamespacedKey(Slimefun.instance(), "share-users");
    private static final int SHARED_USERS_LIMIT = 15;

    private AndroidShareMenu() {}

    /**
     * Open a share menu for player.
     *
     * @param p player
     * @param b android
     */
    @ParametersAreNonnullByDefault
    public static void openShareMenu(Player p, Block b) {
        Validate.notNull(p, "The player cannot be null!");
        Validate.notNull(b, "The android block cannot be null!");

        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage("android.access-manager.title"));

        menu.setEmptySlotsClickable(false);

        List<String> users = getTrustedUsers(b);

        // Draw background start
        for (int i = 0; i < 9; i++) {
            menu.addItem(i, ChestMenuUtils.getBackground());
            menu.addMenuClickHandler(i, (pl, slot, item, action) -> false);
        }

        for (int i = 45; i < 54; i++) {
            menu.addItem(i, ChestMenuUtils.getBackground());
            menu.addMenuClickHandler(i, (pl, slot, item, action) -> false);
        }
        // Draw background end

        // Add trusted player slot
        menu.addItem(
                0,
                new CustomItemStack(
                        HeadTexture.SCRIPT_UP.getAsItemStack(),
                        Slimefun.getLocalization().getMessage("android.access-manager.menu.add-player-title"),
                        Slimefun.getLocalization().getMessage("android.access-manager.menu.add-player")));
        menu.addMenuClickHandler(0, (p1, slot, item, action) -> {
            p1.closeInventory();

            if (users.size() >= SHARED_USERS_LIMIT) {
                Slimefun.getLocalization().sendMessage(p1, "android.access-manager.messages.reach-limit");
                return false;
            }

            Slimefun.getLocalization().sendMessage(p1, "android.access-manager.messages.input");

            ChatUtils.awaitInput(p1, message -> {
                Player target = Bukkit.getPlayerExact(message);

                if (target == null) {
                    Slimefun.getLocalization()
                            .sendMessage(
                                    p1,
                                    "android.access-manager.messages.cannot-find-player",
                                    msg -> msg.replace("%player%", message));
                } else {
                    addPlayer(p1, target, b, users);
                }
            });
            return false;
        });

        // Display added trusted player(s)
        if (!users.isEmpty()) {
            for (int index = 0; index < users.size(); index++) {
                int slot = index + DISPLAY_START_SLOT;
                OfflinePlayer current = Bukkit.getOfflinePlayer(UUID.fromString(users.get(index)));
                menu.addItem(
                        slot,
                        new CustomItemStack(
                                PlayerHead.getItemStack(current),
                                "&b" + current.getName(),
                                Slimefun.getLocalization().getMessage("android.access-manager.menu.delete-player")));
                menu.addMenuClickHandler(slot, (p1, slot1, item, action) -> {
                    if (!action.isRightClicked() && !action.isShiftClicked()) {
                        removePlayer(p1, current, b, users);
                    }
                    return false;
                });
            }
        }

        menu.open(p);
    }

    @ParametersAreNonnullByDefault
    private static void addPlayer(Player owner, OfflinePlayer p, Block android, List<String> users) {
        Validate.notNull(owner, "The android cannot be null!");
        Validate.notNull(p, "The target player cannot be null!");
        Validate.notNull(android, "The android block cannot be null!");
        Validate.notNull(users, "The trusted users list cannot be null!");

        if (users.contains(p.getUniqueId().toString())) {
            Slimefun.getLocalization()
                    .sendMessage(
                            owner,
                            "android.access-manager.messages.is-trusted-player",
                            msg -> msg.replace("%player%", p.getName()));
        } else if (owner.getUniqueId() == p.getUniqueId()) {
            Slimefun.getLocalization().sendMessage(owner, "android.access-manager.messages.cannot-add-yourself");
        } else {
            users.add(p.getUniqueId().toString());
            Slimefun.getLocalization()
                    .sendMessage(
                            owner,
                            "android.access-manager.messages.add-success",
                            msg -> msg.replace("%player%", p.getName()));

            setSharedUserData(android.getState(), users.toString());
        }
    }

    @ParametersAreNonnullByDefault
    private static void removePlayer(Player owner, OfflinePlayer p, Block android, List<String> users) {
        Validate.notNull(owner, "The android cannot be null!");
        Validate.notNull(p, "The target player cannot be null!");
        Validate.notNull(android, "The android block cannot be null!");
        Validate.notNull(users, "The trusted users list cannot be null!");

        if (users.contains(p.getUniqueId().toString())) {
            users.remove(p.getUniqueId().toString());
            Slimefun.getLocalization()
                    .sendMessage(
                            owner,
                            "android.access-manager.messages.delete-success",
                            msg -> msg.replace("%player%", p.getName()));

            setSharedUserData(android.getState(), users.toString());
        } else {
            Slimefun.getLocalization()
                    .sendMessage(
                            owner,
                            "android.access-manager.messages.is-not-trusted-player",
                            msg -> msg.replace("%player%", p.getName()));
        }
    }

    /**
     * Parse trusted player list raw string to List.
     *
     * @param value list raw string
     * @return parse trusted player list
     */
    private @Nonnull static List<String> parseBlockInfoToList(@Nonnull String value) {
        Validate.notNull(value, "The trusted player list cannot be null!");

        String replacedText = value.replace("[", "").replace("]", "");

        if (replacedText.isEmpty()) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(Arrays.asList(replacedText.split(", ")));
        }
    }

    /**
     * Get a trusted users list of specific android.
     *
     * @param b the block of android
     * @return trusted users list
     */
    public @Nonnull static List<String> getTrustedUsers(@Nonnull Block b) {
        Validate.notNull(b, "The android block cannot be null!");

        Optional<String> trustUsers = getSharedUserData(b.getState());

        // Checks for old Android
        if (!trustUsers.isPresent()) {
            List<String> emptyUsers = new ArrayList<>();
            setSharedUserData(b.getState(), String.valueOf(emptyUsers));
            return emptyUsers;
        }

        return parseBlockInfoToList(trustUsers.get());
    }

    /**
     * Checks user is in trusted users list.
     *
     * @param b the block of Android
     * @param uuid user's UUID
     * @return user trusted status
     */
    @ParametersAreNonnullByDefault
    public static boolean isTrustedUser(Block b, UUID uuid) {
        Validate.notNull(b, "The android block cannot be null!");
        Validate.notNull(uuid, "The UUID of player to check cannot be null!");

        Optional<String> trustUsers = getSharedUserData(b.getState());

        return trustUsers.map(s -> s.contains(uuid.toString())).orElse(false);
    }

    private static void setSharedUserData(@Nonnull BlockState state, @Nonnull String value) {
        Validate.notNull(state, "The android block state cannot be null!");
        Validate.notNull(value, "The data value cannot be null!");

        if (!(state instanceof TileState)) {
            return;
        }

        try {
            PersistentDataContainer container = ((TileState) state).getPersistentDataContainer();
            container.set(BLOCK_INFO_KEY, PersistentDataType.STRING, value);
            state.update();
        } catch (Exception x) {
            Slimefun.logger()
                    .log(Level.SEVERE, "An Exception was thrown while trying to set Persistent Data for a Android", x);
        }
    }

    private @Nonnull static Optional<String> getSharedUserData(@Nonnull BlockState state) {
        Validate.notNull(state, "The android block state cannot be null!");

        if (!(state instanceof TileState)) {
            return Optional.empty();
        }

        PersistentDataContainer container = ((TileState) state).getPersistentDataContainer();

        return Optional.ofNullable(container.get(BLOCK_INFO_KEY, PersistentDataType.STRING));
    }
}
