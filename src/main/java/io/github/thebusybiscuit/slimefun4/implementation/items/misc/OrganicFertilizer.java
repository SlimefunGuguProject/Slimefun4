package io.github.thebusybiscuit.slimefun4.implementation.items.misc;

import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.CropGrowthAccelerator;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.FoodComposter;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.TreeGrowthAccelerator;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.inventory.ItemStack;

/**
 * {@link OrganicFertilizer} is used to fuel a {@link CropGrowthAccelerator}
 * or {@link TreeGrowthAccelerator}. And can be crafted using a {@link FoodComposter}.
 *
 * @author TheBusyBiscuit
 * @see CropGrowthAccelerator
 * @see TreeGrowthAccelerator
 */
public class OrganicFertilizer extends SlimefunItem {

    public static final int OUTPUT = 2;

    public OrganicFertilizer(Category category, SlimefunItemStack item, SlimefunItemStack ingredient) {
        super(category, item, RecipeType.FOOD_COMPOSTER, new ItemStack[]{ingredient, null, null, null, null, null, null, null, null}, new SlimefunItemStack(item, OUTPUT));
    }

}