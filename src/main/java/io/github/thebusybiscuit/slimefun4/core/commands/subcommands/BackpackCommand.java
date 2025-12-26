package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.RestoredBackpack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * This command that allows for backpack retrieval in the event they are lost.
 * The command accepts a name and id, if those match up it spawns a Medium Backpack
 * with the correct lore set in the sender's inventory.
 *
 * @author Sfiguz7
 * @see RestoredBackpack
 */
class BackpackCommand extends SubCommand {
    private static final int DISPLAY_START_SLOT = 9;

    @ParametersAreNonnullByDefault
    BackpackCommand(Slimefun plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "backpack", false);
    }

    @Override
    protected String getDescription() {
        return "commands.backpack.description";
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (sender instanceof Player player) {
            if (sender.hasPermission("slimefun.command.backpack")) {
                if (args.length < 1) {
                    Slimefun.getLocalization()
                            .sendMessage(
                                    sender,
                                    "messages.usage",
                                    true,
                                    msg -> msg.replace("%usage%", "/sf backpack (玩家名)"));
                    return;
                }

                if (args.length == 2) {
                    if (sender.hasPermission("slimefun.command.backpack.other")) {
                        Slimefun.getDatabaseManager()
                                .getProfileDataController()
                                .getPlayerUuidAsync(args[1], new IAsyncReadCallback<>() {
                                    @Override
                                    public void onResult(UUID result) {
                                        if (!player.isOnline()) {
                                            return;
                                        }
                                        openBackpackMenu(Bukkit.getOfflinePlayer(result), player);
                                    }

                                    @Override
                                    public void onResultNotFound() {
                                        Slimefun.getLocalization()
                                                .sendMessage(player, "commands.backpack.backpack-does-not-exist");
                                    }
                                });
                    } else {
                        Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
                        return;
                    }
                } else {
                    openBackpackMenu(player, player);
                }

                Slimefun.getLocalization().sendMessage(player, "commands.backpack.searching");
            } else {
                Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
            }
        } else {
            Slimefun.getLocalization().sendMessage(sender, "messages.only-players", true);
        }
    }

    private void openBackpackMenu(@Nonnull OfflinePlayer owner, @Nonnull Player p) {
        Validate.notNull(p, "The player cannot be null!");

        Slimefun.getDatabaseManager()
                .getProfileDataController()
                .getBackpacksAsync(owner.getUniqueId().toString(), new IAsyncReadCallback<>() {
                    @Override
                    public boolean runOnMainThread() {
                        return true;
                    }

                    @Override
                    public void onResult(Set<PlayerBackpack> result) {
                        if (!p.isOnline()) {
                            return;
                        }
                        showBackpackMenu(owner, p, result, 1);
                    }

                    @Override
                    public void onResultNotFound() {
                        Slimefun.getLocalization().sendMessage(p, "commands.backpack.backpack-does-not-exist");
                    }
                });
    }

    private void showBackpackMenu(OfflinePlayer owner, Player p, Set<PlayerBackpack> result, int page) {
        var menu = new ChestMenu(owner.getName() + " 拥有的背包列表");
        menu.setEmptySlotsClickable(false);

        var pages = result.size() / 36;

        // Draw background start
        for (int i = 0; i < 9; i++) {
            menu.addItem(i, ChestMenuUtils.getBackground());
            menu.addMenuClickHandler(i, (pl, slot, item, action) -> false);
        }

        var bps = new ArrayList<>(result);
        // max display 36 backpacks per page
        for (int i = 0; i <= 36; i++) {
            int slot = DISPLAY_START_SLOT + i;
            var index = i + 36 * (page - 1);
            if (index >= bps.size()) {
                break;
            }
            var bp = bps.get(index);

            var visualBackpack = SlimefunItems.RESTORED_BACKPACK.clone();
            var im = visualBackpack.getItemMeta();
            im.setDisplayName(bp.getName().isEmpty() ? "背包 #" + bp.getId() : bp.getName());
            var lore = new ArrayList<String>();
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&a左键 获取此背包"));
            im.setLore(lore);
            visualBackpack.setItemMeta(im);

            menu.addItem(slot, visualBackpack);
            menu.addMenuClickHandler(slot, (p1, slot1, item, action) -> {
                if (!action.isRightClicked() && !action.isShiftClicked() && p1.getUniqueId() == p.getUniqueId()) {
                    var restoreBp = SlimefunItems.RESTORED_BACKPACK.clone();
                    PlayerBackpack.bindItem(restoreBp, bp);
                    p1.getInventory().addItem(restoreBp);
                    Slimefun.getLocalization().sendMessage(p1, "commands.backpack.restored-backpack-given");
                }

                return false;
            });
        }

        for (int i = 45; i < 54; i++) {
            menu.addItem(i, ChestMenuUtils.getBackground());
            menu.addMenuClickHandler(i, (pl, slot, item, action) -> false);
        }

        // Draw background end

        if (pages > 1) {
            menu.addItem(46, ChestMenuUtils.getPreviousButton(p, page, pages));
            menu.addMenuClickHandler(46, (pl, slot, item, action) -> {
                int next = page - 1;

                if (next > 0) {
                    showBackpackMenu(owner, p, result, next);
                }

                return false;
            });

            if (page < pages) {
                menu.addItem(52, ChestMenuUtils.getNextButton(p, page, pages));
                menu.addMenuClickHandler(52, (pl, slot, item, action) -> {
                    int next = page + 1;

                    if (next <= pages) {
                        showBackpackMenu(owner, p, result, next);
                    }

                    return false;
                });
            }
        }

        menu.open(p);
    }
}
