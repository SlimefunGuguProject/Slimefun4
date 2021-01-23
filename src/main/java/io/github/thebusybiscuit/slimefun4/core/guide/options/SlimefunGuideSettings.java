package io.github.thebusybiscuit.slimefun4.core.guide.options;

import io.github.thebusybiscuit.cscorelib2.chat.ChatColors;
import io.github.thebusybiscuit.cscorelib2.item.CustomItem;
import io.github.thebusybiscuit.cscorelib2.skull.SkullItem;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.researching.Research;
import io.github.thebusybiscuit.slimefun4.core.services.github.Contributor;
import io.github.thebusybiscuit.slimefun4.core.services.localization.Language;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.utils.*;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * This static utility class offers various methods that provide access to the
 * Settings menu of our {@link SlimefunGuide}.
 *
 * This menu is used to allow a {@link Player} to change things such as the {@link Language}.
 *
 * @author TheBusyBiscuit
 *
 * @see SlimefunGuide
 *
 */
public final class SlimefunGuideSettings {

    private static final int[] BACKGROUND_SLOTS = {1, 3, 5, 7, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 48, 50, 52, 53};
    private static final List<SlimefunGuideOption<?>> options = new ArrayList<>();

    static {
        options.add(new GuideLayoutOption());
        options.add(new FireworksOption());
        options.add(new PlayerLanguageOption());
    }

    private SlimefunGuideSettings() {
    }

    public static <T> void addOption(@Nonnull SlimefunGuideOption<T> option) {
        options.add(option);
    }

    @ParametersAreNonnullByDefault
    public static void openSettings(Player p, ItemStack guide) {
        ChestMenu menu = new ChestMenu(SlimefunPlugin.getLocalization().getMessage(p, "guide.title.settings"));

        menu.setEmptySlotsClickable(false);
        menu.addMenuOpeningHandler(pl -> pl.playSound(pl.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.7F, 0.7F));

        ChestMenuUtils.drawBackground(menu, BACKGROUND_SLOTS);

        addHeader(p, menu, guide);
        addConfigurableOptions(p, menu, guide);

        menu.open(p);
    }

    @ParametersAreNonnullByDefault
    private static void addHeader(Player p, ChestMenu menu, ItemStack guide) {
        menu.addItem(0, new CustomItem(SlimefunGuide.getItem(SlimefunGuideMode.SURVIVAL_MODE), "&e\u21E6 " + SlimefunPlugin.getLocalization().getMessage(p, "guide.back.title"), "", "&7" + SlimefunPlugin.getLocalization().getMessage(p, "guide.back.guide")), (pl, slot, item, action) -> {
            SlimefunGuide.openGuide(pl, guide);
            return false;
        });

        List<String> contributorsLore = new ArrayList<>();
        contributorsLore.add("");
        contributorsLore.addAll(SlimefunPlugin.getLocalization().getMessages(p, "guide.credits.description", msg -> msg.replace("%contributors%", String.valueOf(SlimefunPlugin.getGitHubService().getContributors().size()))));
        contributorsLore.add("");
        contributorsLore.add("&7\u21E8 &e" + SlimefunPlugin.getLocalization().getMessage(p, "guide.credits.open"));

        menu.addItem(2, new CustomItem(SlimefunUtils.getCustomHead("e952d2b3f351a6b0487cc59db31bf5f2641133e5ba0006b18576e996a0293e52"), "&c" + SlimefunPlugin.getLocalization().getMessage(p, "guide.title.credits"), contributorsLore.toArray(new String[0])), (pl, slot, action, item) -> {
            ContributorsMenu.open(pl, 0);
            return false;
        });

        menu.addItem(4, new CustomItem(Material.WRITABLE_BOOK, ChatColor.GREEN + SlimefunPlugin.getLocalization().getMessage(p, "guide.title.versions"), "&7&o" + SlimefunPlugin.getLocalization().getMessage(p, "guide.tooltips.versions-notice"), "", "&fMinecraft: &a" + Bukkit.getBukkitVersion(), "&fSlimefun: &a" + SlimefunPlugin.getVersion()), ChestMenuUtils.getEmptyClickHandler());

        menu.addItem(6,
                new CustomItem(Material.COMPARATOR, "&e" + SlimefunPlugin.getLocalization().getMessage(p, "guide.title.source"), "", "&7Last Activity: &a" + NumberUtils.getElapsedTime(SlimefunPlugin.getGitHubService().getLastUpdate()) + " ago", "&7Forks: &e" + SlimefunPlugin.getGitHubService().getForks(), "&7Stars: &e" + SlimefunPlugin.getGitHubService().getStars(), "", "&7&oSlimefun 4 is a community project,", "&7&othe source code is available on GitHub", "&7&oand if you want to keep this Plugin alive,", "&7&othen please consider contributing to it", "", "&7\u21E8 &eClick to go to GitHub"));
        menu.addMenuClickHandler(6, (pl, slot, item, action) -> {
            pl.closeInventory();
            ChatUtils.sendURL(pl, "https://github.com/TheBusyBiscuit/Slimefun4");
            return false;
        });

        menu.addItem(8, new CustomItem(Material.KNOWLEDGE_BOOK, "&3" + SlimefunPlugin.getLocalization().getMessage(p, "guide.title.wiki"), "", "&7Do you need help with an Item or machine?", "&7You cannot figure out what to do?", "&7Check out our community-maintained Wiki", "&7and become one of our Editors!", "", "&7\u21E8 &eClick to go to the official Slimefun Wiki"), (pl, slot, item, action) -> {
            pl.closeInventory();
            ChatUtils.sendURL(pl, "https://github.com/TheBusyBiscuit/Slimefun4/wiki");
            return false;
        });

        menu.addItem(47, new CustomItem(Material.BOOKSHELF, "&3" + SlimefunPlugin.getLocalization().getMessage(p, "guide.title.addons"), "", "&7Slimefun is huge. But its addons are what makes", "&7this plugin truly shine. Go check them out, some", "&7of them may be exactly what you were missing out on!", "", "&7Installed on this Server: &b" + SlimefunPlugin.getInstalledAddons().size(), "", "&7\u21E8 &eClick to see all available Addons for Slimefun4"), (pl, slot, item, action) -> {
            pl.closeInventory();
            ChatUtils.sendURL(pl, "https://github.com/TheBusyBiscuit/Slimefun4/wiki/Addons");
            return false;
        });

        menu.addItem(49, new CustomItem(Material.REDSTONE_TORCH, "&4" + SlimefunPlugin.getLocalization().getMessage(p, "guide.title.bugs"), "", "&7&oBug 反馈请附上详细信息, 请不要到官方的 Issue 反馈!", "", "&7开启的问题: &a" + SlimefunPlugin.getGitHubService().getOpenIssues(), "&7待合并的提交请求: &a" + SlimefunPlugin.getGitHubService().getPendingPullRequests(), "", "&7\u21E8 &e单击反馈 Bug"), (pl, slot, item, action) -> {
            pl.closeInventory();
            ChatUtils.sendURL(pl, "https://github.com/StarWishsama/Slimefun4/issues");
            return false;
        });

        menu.addItem(51, new CustomItem(Material.TOTEM_OF_UNDYING, ChatColor.RED + SlimefunPlugin.getLocalization().getMessage(p, "guide.work-in-progress")), (pl, slot, item, action) -> {
            // Add something here
            return false;
        });
    }

    @ParametersAreNonnullByDefault
    private static void addConfigurableOptions(Player p, ChestMenu menu, ItemStack guide) {
        int i = 19;

        for (SlimefunGuideOption<?> option : options) {
            Optional<ItemStack> item = option.getDisplayItem(p, guide);

            if (item.isPresent()) {
                menu.addItem(i, item.get());
                menu.addMenuClickHandler(i, (pl, slot, stack, action) -> {
                    option.onClick(p, guide);
                    return false;
                });

                i++;
            }
        }
    }

    private static ItemStack getContributorHead(Player p, Contributor contributor) {
        ItemStack skull = SkullItem.fromBase64(contributor.getTexture());

        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setDisplayName(contributor.getDisplayName());

        List<String> lore = new LinkedList<>();
        lore.add("");

        for (Map.Entry<String, Integer> entry : contributor.getContributions()) {
            String info = entry.getKey();

            if (!info.startsWith("&")) {
                String[] segments = PatternUtils.COMMA.split(info);
                info = SlimefunPlugin.getLocalization().getMessage(p, "guide.credits.roles." + segments[0]);

                if (segments.length == 2) {
                    info += " &7(" + SlimefunPlugin.getLocalization().getMessage(p, "languages." + segments[1]) + ')';
                }
            }

            if (entry.getValue() > 0) {
                String commits = SlimefunPlugin.getLocalization().getMessage(p, "guide.credits." + (entry.getValue() > 1 ? "commits" : "commit"));

                info += " &7(" + entry.getValue() + ' ' + commits + ')';
            }

            lore.add(ChatColors.color(info));
        }

        if (contributor.getProfile() != null) {
            lore.add("");
            lore.add(ChatColors.color("&7\u21E8 &e") + SlimefunPlugin.getLocalization().getMessage(p, "guide.credits.profile-link"));
        }

        meta.setLore(lore);
        skull.setItemMeta(meta);
        return skull;
    }

    /**
     * This method checks if the given {@link Player} has enabled the {@link FireworksOption}
     * in their {@link SlimefunGuide}.
     * If they enabled this setting, they will see fireworks when they unlock a {@link Research}.
     *
     * @param p The {@link Player}
     * @return Whether this {@link Player} wants to see fireworks when unlocking a {@link Research}
     */
    public static boolean hasFireworksEnabled(Player p) {
        for (SlimefunGuideOption<?> option : options) {
            if (option instanceof FireworksOption) {
                FireworksOption fireworks = (FireworksOption) option;
                return fireworks.getSelectedOption(p, SlimefunGuide.getItem(SlimefunGuideMode.SURVIVAL_MODE)).orElse(true);
            }
        }

        return true;
    }

}