package io.github.thebusybiscuit.slimefun4.api.gps;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Material;

public class PageHelper {

    /**
     * This method renders the pagination buttons for a paged {@link ChestMenu}.
     * <p>
     * The pagination logic follows a 1-based page index. If the total amount of pages is
     * greater than one, this method will add interactive "previous" and "next" buttons
     * to the provided slot positions. Otherwise, both slots will be filled with a
     * background item and no interaction will be allowed.
     *
     * <p>
     * When {@code page > 1}, a clickable "previous page" button will be rendered in
     * {@code prevSlot}. When {@code page < totalPages}, a clickable "next page" button
     * will be rendered in {@code nextSlot}. Disabled buttons are displayed as
     * non-interactable gray panes.
     *
     * @param menu
     *        The {@link ChestMenu} to render the pagination controls into
     *
     * @param prevSlot
     *        The inventory slot used for the "previous page" button
     *
     * @param nextSlot
     *        The inventory slot used for the "next page" button
     *
     * @param pr
     *        The {@link PageRange} defining the current page index and total page count
     *
     * @param prevHander
     *        The {@link ChestMenu.MenuClickHandler} to be executed when the player clicks
     *        the "previous page" button. This handler is only applied when the button
     *        is active.
     *
     * @param nextHander
     *        The {@link ChestMenu.MenuClickHandler} to be executed when the player clicks
     *        the "next page" button. This handler is only applied when the button
     *        is active.
     */
    @ParametersAreNonnullByDefault
    public static void renderPageButton(
            ChestMenu menu,
            int prevSlot,
            int nextSlot,
            PageRange pr,
            ChestMenu.MenuClickHandler prevHander,
            ChestMenu.MenuClickHandler nextHander) {
        int page = pr.getCurrentPage();
        int totalPages = pr.getTotalPages();

        if (totalPages > 1) {
            if (page > 1) {
                menu.addItem(
                        prevSlot, new CustomItemStack(Material.ARROW, "&a上一页 &7(" + page + "/" + totalPages + ")"));
                menu.addMenuClickHandler(prevSlot, prevHander);
            } else {
                menu.addItem(prevSlot, new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, "&7上一页（无）"));
                menu.addMenuClickHandler(prevSlot, ChestMenuUtils.getEmptyClickHandler());
            }

            if (page < totalPages) {
                menu.addItem(
                        nextSlot, new CustomItemStack(Material.ARROW, "&a下一页 &7(" + page + "/" + totalPages + ")"));
                menu.addMenuClickHandler(nextSlot, nextHander);
            } else {
                menu.addItem(nextSlot, new CustomItemStack(Material.GRAY_STAINED_GLASS_PANE, "&7下一页（无）"));
                menu.addMenuClickHandler(nextSlot, ChestMenuUtils.getEmptyClickHandler());
            }
        } else {
            menu.addItem(prevSlot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
            menu.addItem(nextSlot, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }
    }
}
