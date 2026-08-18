package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * @author balugaq
 */
public class TickCommand extends SubCommand {
    @ParametersAreNonnullByDefault
    public TickCommand(Slimefun plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "tick", false);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (sender.hasPermission("slimefun.command.tick") || sender instanceof ConsoleCommandSender) {
            if (args.length == 1) {
                Slimefun.getTickerTask().setTickFreeze(!Slimefun.getTickerTask().isTickFreeze());
                Slimefun.getLocalization()
                        .sendMessage(
                                sender,
                                "messages.tick-mode",
                                true,
                                msg -> msg.replace(
                                        "%mode%", Slimefun.getTickerTask().isTickFreeze() ? "开启" : "关闭"));
                return;
            }

            if (args.length == 2) {
                if (sender instanceof Player player && args[1].equalsIgnoreCase("at")) {
                    Block b = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
                    SlimefunItem sf = null;
                    if (b != null) {
                        sf = StorageCacheUtils.getSlimefunItem(b.getLocation());
                    }

                    if (sf == null) {
                        sf = SlimefunItem.getByItem(player.getInventory().getItemInMainHand());
                    }

                    if (sf == null) {
                        Slimefun.getLocalization().sendMessage(sender, "messages.not-found", true);
                        return;
                    }

                    final SlimefunItem finalSf = sf;
                    Slimefun.getTickerTask()
                            .setTickFreezePredicate(
                                    entry -> entry.getItem().getId().equals(finalSf.getId()));

                    return;
                } else if (args[1].equalsIgnoreCase("show")) {
                    Slimefun.getTickerTask().showWaitingList();
                    return;
                } else if (args[1].equalsIgnoreCase("freeze")) {
                    Slimefun.getTickerTask().setTickFreeze(true);
                    Slimefun.getLocalization()
                            .sendMessage(sender, "messages.tick-mode", true, msg -> msg.replace("%mode%", "开启"));
                    return;
                } else if (args[1].equalsIgnoreCase("unfreeze")) {
                    Slimefun.getTickerTask().setTickFreeze(false);
                    Slimefun.getLocalization()
                            .sendMessage(sender, "messages.tick-mode", true, msg -> msg.replace("%mode%", "关闭"));
                    return;
                } else if (args[1].equalsIgnoreCase("query")) {
                    Slimefun.getLocalization()
                            .sendMessage(
                                    sender,
                                    "messages.tick-query",
                                    true,
                                    msg -> msg.replace(
                                                    "%mode%",
                                                    Slimefun.getTickerTask().isTickFreeze() ? "开启" : "关闭")
                                            .replace(
                                                    "%tick-rate%",
                                                    ""
                                                            + Slimefun.getTickerTask()
                                                                    .getTickRate()));
                    return;
                } else {
                    SlimefunItem item = SlimefunItem.getById(args[1].toUpperCase());
                    if (item != null) {
                        Slimefun.getTickerTask()
                                .setTickFreezePredicate(entry -> entry.getItem().equals(item));
                    } else {
                        Slimefun.getLocalization().sendMessage(sender, "messages.not-found");
                    }
                    return;
                }
            }

            if (args[1].equalsIgnoreCase("rate") && args.length == 3) {
                try {
                    int rate = Integer.parseInt(args[2]);
                    Slimefun.getTickerTask().setTickRate(rate);
                    Slimefun.getLocalization()
                            .sendMessage(
                                    sender,
                                    "messages.tick-rate",
                                    true,
                                    msg -> msg.replace("%tick-rate%", String.valueOf(rate)));
                } catch (NumberFormatException e) {
                    Slimefun.getLocalization().sendMessage(sender, "messages.not-a-number", true);
                }
                return;
            }

            if (args.length >= 4) {
                int offset = 0;
                if (args.length == 5) {
                    offset = 1;
                }
                String worldname = args.length == 5
                        ? args[1]
                        : (sender instanceof Player p ? p.getWorld().getName() : "world");
                try {
                    int x = Integer.parseInt(args[1 + offset]);
                    int y = Integer.parseInt(args[2 + offset]);
                    int z = Integer.parseInt(args[3 + offset]);
                    Slimefun.getTickerTask().setTickFreezePredicate(entry -> {
                        var l = entry.getLocation();
                        return l.getWorld().getName().equals(worldname)
                                && l.getBlockX() == x
                                && l.getBlockY() == y
                                && l.getBlockZ() == z;
                    });
                } catch (NumberFormatException e) {
                    Slimefun.getLocalization().sendMessage(sender, "messages.not-a-number", true);
                }

                Slimefun.getLocalization()
                        .sendMessage(
                                sender,
                                "messages.usage",
                                true,
                                msg -> msg.replace("%usage%", "/sf tick (<x> <y> <z> | <Slimefun Item>)"));
            }
        } else {
            Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
        }
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "commands.tick.description";
    }
}
