package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import io.github.thebusybiscuit.cscorelib2.chat.ChatColors;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.Collection;

class VersionsCommand extends SubCommand {

    VersionsCommand(SlimefunPlugin plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "versions", false);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (sender.hasPermission("slimefun.command.versions") || sender instanceof ConsoleCommandSender) {
            // After all these years... Spigot still displays as "CraftBukkit"
            // so we will just fix this inconsistency for them :)
            String serverSoftware = PaperLib.isSpigot() && !PaperLib.isPaper() ? "Spigot" : Bukkit.getName();

            sender.sendMessage(ChatColor.GRAY + "服务器环境如下:");
            sender.sendMessage(ChatColors.color("&a" + serverSoftware + " &2" + Bukkit.getVersion()));
            sender.sendMessage(ChatColors.color("&aSlimefun &2v" + SlimefunPlugin.getVersion()));

            if (SlimefunPlugin.getMetricsService().getVersion() != null) {
                sender.sendMessage(ChatColors.color("&aMetrics 构建版本: &2#" + SlimefunPlugin.getMetricsService().getVersion()));
            }

            if (SlimefunPlugin.getRegistry().isBackwardsCompatible()) {
                sender.sendMessage(ChatColor.RED + "向后兼容已启用!");
            }

            sender.sendMessage("");

            Collection<Plugin> addons = SlimefunPlugin.getInstalledAddons();
            sender.sendMessage(ChatColors.color("&7已安装的附属插件: &8(" + addons.size() + ")"));

            for (Plugin plugin : addons) {
                String version = plugin.getDescription().getVersion();

                if (Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                    sender.sendMessage(ChatColor.GREEN + " " + plugin.getName() + ChatColor.DARK_GREEN + " v" + version);
                } else {
                    sender.sendMessage(ChatColor.RED + " " + plugin.getName() + ChatColor.DARK_RED + " v" + version);
                }
            }
        } else {
            SlimefunPlugin.getLocalization().sendMessage(sender, "messages.no-permission", true);
        }
    }

}