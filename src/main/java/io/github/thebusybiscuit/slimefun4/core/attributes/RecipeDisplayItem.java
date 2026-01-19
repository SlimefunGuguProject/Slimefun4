package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.geo.GEOMiner;
import io.github.thebusybiscuit.slimefun4.implementation.items.tools.GoldPan;
import java.util.List;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * This interface, when attache to a {@link SlimefunItem} class will make additional items
 * appear in the {@link SlimefunGuide}.
 * These additional items can be used represent recipes or resources that are associated
 * with this {@link SlimefunItem}.
 *
 * You can find a few examples below.
 *
 * @author TheBusyBiscuit
 *
 * @see GoldPan
 * @see GEOMiner
 * @see AGenerator
 *
 */
public interface RecipeDisplayItem extends ItemAttribute {

    /**
     * This is the list of items to display alongside this {@link SlimefunItem}.
     * Note that these items will be filled in from top to bottom first.
     * So if you want it to express a recipe, add your input {@link ItemStack}
     * and then your output {@link ItemStack}.
     *
     * @return The recipes to display in the {@link SlimefunGuide}
     */
    @Nonnull
    List<ItemStack> getDisplayRecipes();

    /**
     * Gets the local path for the label in the guide.
     *
     * @return The local path for the label
     */
    @Nonnull
    default String getLabelLocalPath() {
        return "guide.tooltips.recipes.machine";
    }

    @Nonnull
    default String getRecipeSectionLabel(@Nonnull Player p) {
        return "&7\u21E9 " + Slimefun.getLocalization().getMessage(p, getLabelLocalPath()) + " \u21E9";
    }
}
