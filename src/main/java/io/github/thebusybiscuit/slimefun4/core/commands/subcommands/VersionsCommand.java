package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import city.norain.slimefun4.utils.EnvUtil;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import io.papermc.lib.PaperLib;
import java.util.Collection;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;

/**
 * This is our class for the /sf versions subcommand.
 *
 * @author TheBusyBiscuit
 * @author Walshy
 *
 */
class VersionsCommand extends SubCommand {

    /**
     * This is the Java version we recommend to use.
     * Bump as necessary and adjust the warning.
     */
    private static final int RECOMMENDED_JAVA_VERSION = 16;

    /**
     * This is the notice that will be displayed when an
     * older version of Java is detected.
     */
    private static final String JAVA_VERSION_NOTICE = "在 Minecraft 1.17 发布时需要 Java 16+!";

    @ParametersAreNonnullByDefault
    VersionsCommand(Slimefun plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "versions", false);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (sender.hasPermission("slimefun.command.versions") || sender instanceof ConsoleCommandSender) {
            /*
             * After all these years... Spigot still displays as "CraftBukkit".
             * so we will just fix this inconsistency for them :)
             */
            String serverSoftware = PaperLib.isSpigot() && !PaperLib.isPaper() ? "Spigot" : Bukkit.getName();
            ComponentBuilder builder = new ComponentBuilder();

            // @formatter:off
            builder.append("Slimefun 运行的服务器环境:\n")
                    .color(ChatColor.GRAY)
                    .append(serverSoftware)
                    .color(ChatColor.GREEN)
                    .append(" " + Bukkit.getVersion() + '\n')
                    .color(ChatColor.DARK_GREEN)
                    .append("Slimefun ")
                    .color(ChatColor.GREEN)
                    .append(Slimefun.getVersion()
                            + (Slimefun.getVersion().toLowerCase(Locale.ROOT).contains("release")
                                    ? ""
                                    : " @" + EnvUtil.getBranch())
                            + '\n')
                    .color(ChatColor.DARK_GREEN)
                    .append("构建时间 ")
                    .color(ChatColor.GREEN)
                    .append(EnvUtil.getBuildTime())
                    .color(ChatColor.DARK_GREEN)
                    .append("\n");
            // @formatter:on

            if (Slimefun.getMetricsService().getVersion() != null) {
                // @formatter:off
                builder.append("Metrics-组件 ")
                        .color(ChatColor.GREEN)
                        .append("#" + Slimefun.getMetricsService().getVersion() + '\n')
                        .color(ChatColor.DARK_GREEN);
                // @formatter:on
            }

            addJavaVersion(builder);

            // Specify we are NOT OFFICIAL build so no support from upstream
            builder.append("\n由 StarWishsama 汉化")
                    .color(ChatColor.WHITE)
                    .append("\n请不要将此版本信息截图到 Discord/Github 反馈 Bug" + "\n优先到汉化页面反馈" + "\n")
                    .color(ChatColor.RED);

            if (Slimefun.getConfigManager().isBypassEnvironmentCheck()) {
                builder.append("\n").event((HoverEvent) null);
                builder.append("\n已禁用环境兼容性检查").color(ChatColor.RED);
            }

            if (Slimefun.getConfigManager().isBypassItemLengthCheck()) {
                builder.append("\n").event((HoverEvent) null);
                builder.append("\n已禁用物品长度检查").color(ChatColor.RED);
            }

            builder.append("\n").event((HoverEvent) null);
            addPluginVersions(builder);

            sender.spigot().sendMessage(builder.create());
        } else {
            Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
        }
    }

    private void addJavaVersion(@Nonnull ComponentBuilder builder) {
        int version = NumberUtils.getJavaVersion();

        if (version < RECOMMENDED_JAVA_VERSION) {
            // @formatter:off
            builder.append("Java " + version)
                    .color(ChatColor.RED)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {
                        new TextComponent("你使用的 Java 版本已过时!\n!"
                                + "推荐你使用 Java "
                                + RECOMMENDED_JAVA_VERSION
                                + " 或更高版本.\n"
                                + JAVA_VERSION_NOTICE)
                    }))
                    .append("\n")
                    .event((HoverEvent) null);
            // @formatter:on
        } else {
            builder.append("Java ")
                    .color(ChatColor.GREEN)
                    .append(version + "\n")
                    .color(ChatColor.DARK_GREEN);
        }
    }

    private void addPluginVersions(@Nonnull ComponentBuilder builder) {
        Collection<Plugin> addons = Slimefun.getInstalledAddons();

        if (addons.isEmpty()) {
            builder.append("没有安装任何附属插件").color(ChatColor.GRAY).italic(true);
            return;
        }

        builder.append("安装的附属插件: ")
                .color(ChatColor.GRAY)
                .append("(" + addons.size() + ")")
                .color(ChatColor.DARK_GRAY);

        for (Plugin plugin : addons) {
            String version = plugin.getDescription().getVersion();

            HoverEvent hoverEvent = null;
            ClickEvent clickEvent = null;
            ChatColor primaryColor;
            ChatColor secondaryColor;

            if (Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                primaryColor = ChatColor.GREEN;
                secondaryColor = ChatColor.DARK_GREEN;
                String authors = String.join(", ", plugin.getDescription().getAuthors());

                if (plugin instanceof SlimefunAddon addon && addon.getBugTrackerURL() != null) {
                    // @formatter:off
                    hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {
                        new TextComponent(new ComponentBuilder()
                                .append("作者: ")
                                .append(authors)
                                .color(ChatColor.YELLOW)
                                .append("\n> 单击打开反馈页面")
                                .color(ChatColor.GOLD)
                                .create())
                    });
                    // @formatter:on

                    clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, addon.getBugTrackerURL());
                } else {
                    // @formatter:off
                    hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {
                        new TextComponent(new ComponentBuilder()
                                .append("作者: ")
                                .append(authors)
                                .color(ChatColor.YELLOW)
                                .create())
                    });
                    // @formatter:on
                }
            } else {
                primaryColor = ChatColor.RED;
                secondaryColor = ChatColor.DARK_RED;

                if (plugin instanceof SlimefunAddon addon && addon.getBugTrackerURL() != null) {
                    // @formatter:off
                    hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {
                        new TextComponent(new ComponentBuilder()
                                .append("此插件已被禁用.\n检查后台是否有报错.")
                                .color(ChatColor.RED)
                                .append("\n> 单击打开反馈页面")
                                .color(ChatColor.DARK_RED)
                                .create())
                    });
                    // @formatter:on

                    if (addon.getBugTrackerURL() != null) {
                        clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, addon.getBugTrackerURL());
                    }
                } else {
                    hoverEvent = new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponent[] {new TextComponent("插件已被禁用. 可以看看后台是否有报错.")});
                }
            }

            // @formatter:off
            // We need to reset the hover event or it's added to all components
            builder.append("\n  " + plugin.getName())
                    .color(primaryColor)
                    .event(hoverEvent)
                    .event(clickEvent)
                    .append(" v" + version)
                    .color(secondaryColor)
                    .append("")
                    .event((ClickEvent) null)
                    .event((HoverEvent) null);
            // @formatter:on
        }
    }
}
