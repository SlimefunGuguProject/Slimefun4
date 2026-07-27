package me.mrCookieSlime.Slimefun.api;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.entity.Player;

/**
 * Provides a few static convenience methods.
 *
 * @deprecated This class is slowly getting stripped away in favour of a more object-oriented approach.
 *
 * @author TheBusyBiscuit
 * @author Walshy
 * @author Poslovitch
 */
@Deprecated
@SlimefunAPI
public final class Slimefun {

    private Slimefun() {}

    /**
     * Checks if this player has the permission to use this item.
     *
     * @param p
     *            the player to check, not null
     * @param item
     *            the item to check, null returns <code>true</code>
     * @param message
     *            whether a message should be sent to the player or not
     *
     * @deprecated This method will soon be removed.
     *
     * @return <code>true</code> if the item is not null and if the player has the permission to use it,
     *         <code>false</code> otherwise.
     */
    @Deprecated
    public static boolean hasPermission(Player p, SlimefunItem item, boolean message) {
        if (item == null) {
            return true;
        } else if (io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getPermissionsService()
                .hasPermission(p, item)) {
            return true;
        } else {
            if (message) {
                io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getLocalization()
                        .sendMessage(p, "messages.no-permission", true);
            }

            return false;
        }
    }

    /**
     * Checks if this item is enabled in the world this player is in.
     *
     * @param p
     *            the player to get the world he is in, not null
     * @param sfItem
     *            the item to check, not null
     * @param message
     *            whether a message should be sent to the player or not
     *
     * @deprecated Please use {@link SlimefunItem#isDisabledIn(org.bukkit.World)} instead.
     *
     * @return <code>true</code> if the item is enabled in the world the player is in,
     *         <code>false</code> otherwise.
     */
    @Deprecated
    public static boolean isEnabled(Player p, SlimefunItem sfItem, boolean message) {
        if (sfItem.isDisabled()) {
            if (message) {
                io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getLocalization()
                        .sendMessage(p, "messages.disabled-item", true);
            }

            return false;
        } else if (!io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getWorldSettingsService()
                .isEnabled(p.getWorld(), sfItem)) {
            if (message) {
                io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getLocalization()
                        .sendMessage(p, "messages.disabled-in-world", true);
            }

            return false;
        }
        return true;
    }
}
