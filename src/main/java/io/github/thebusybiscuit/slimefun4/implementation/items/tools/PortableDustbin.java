package io.github.thebusybiscuit.slimefun4.implementation.items.tools;

import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;

/**
 * The {@link PortableDustbin} is one of the oldest items in Slimefun.
 * It simply opens an empty {@link Inventory} in which you can dump any
 * unwanted {@link ItemStack}. When closing the {@link Inventory}, all items
 * will be voided.
 *
 * @author TheBusyBiscuit
 *
 */
public class PortableDustbin extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    public PortableDustbin(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Override
    public @Nonnull ItemUseHandler getItemHandler() {
        return e -> {
            e.cancel();

            Player p = e.getPlayer();
            p.openInventory(Bukkit.createInventory(null, 9 * 3, ChatColor.DARK_RED + "销毁物品"));
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
        };
    }
}
