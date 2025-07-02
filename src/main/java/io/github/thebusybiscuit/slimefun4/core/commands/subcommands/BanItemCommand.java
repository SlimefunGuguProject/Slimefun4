package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * This is our class for the /sf banitem subcommand.
 *
 * @author Ddggdd135
 */
public class BanItemCommand extends SubCommand {

    private static final String PLACEHOLDER_ITEM = "%item%";

    @ParametersAreNonnullByDefault
    public BanItemCommand(Slimefun plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "banitem", false);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (sender.hasPermission("slimefun.command.banitem") || sender instanceof ConsoleCommandSender) {
            if (args.length >= 2) {
                SlimefunItem item = SlimefunItem.getById(args[1]);
                if (item != null) {
                    banItem(item, sender);
                    return;
                }
                Slimefun.getLocalization()
                        .sendMessage(
                                sender, "messages.invalid-item", true, msg -> msg.replace(PLACEHOLDER_ITEM, args[1]));
                return;
            }
            if (sender instanceof Player player) {
                ItemStack itemOnHand = player.getInventory().getItemInMainHand();
                if (!itemOnHand.getType().isAir()) {
                    SlimefunItem item = SlimefunItem.getByItem(itemOnHand);
                    if (item != null) {
                        banItem(item, sender);
                        return;
                    }
                }
                Slimefun.getLocalization().sendMessage(sender, "invalid-item-in-hand", true);
                return;
            }
            Slimefun.getLocalization()
                    .sendMessage(
                            sender,
                            "messages.usage",
                            true,
                            msg -> msg.replace("%usage%", "/sf banitem <Slimefun Item>"));
        } else {
            Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
        }
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "commands.banitem.description";
    }

    private static void banItem(SlimefunItem slimefunItem, CommandSender sender) {
        slimefunItem.disable();
        Slimefun.getItemCfg().setValue(slimefunItem.getId() + ".enabled", false);
        Slimefun.getItemCfg().save();
        Slimefun.getLocalization().sendMessage(sender, "commands.banitem.success", true);
    }
}
