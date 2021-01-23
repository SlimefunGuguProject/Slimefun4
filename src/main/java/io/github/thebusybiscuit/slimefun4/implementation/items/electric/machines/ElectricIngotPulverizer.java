package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import io.github.thebusybiscuit.slimefun4.core.attributes.NotHopperable;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link ElectricIngotPulverizer} is an implementation of {@link AContainer} that allows
 * you to turn various Slimefun Ingots back into their dusts.
 *
 * @author John000708
 * @see ElectricIngotFactory
 */
public class ElectricIngotPulverizer extends AContainer implements RecipeDisplayItem, NotHopperable {

    public ElectricIngotPulverizer(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.IRON_PICKAXE);
    }

    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> displayRecipes = new ArrayList<>(recipes.size() * 2);

        for (MachineRecipe recipe : recipes) {
            displayRecipes.add(recipe.getInput()[0]);
            displayRecipes.add(recipe.getOutput()[0]);
        }

        return displayRecipes;
    }

    @Override
    protected void registerDefaultRecipes() {
        // this is an utils recipe on top of PostSetup.loadSmelteryRecipes() for converting

        // Vanilla Gold Ingot to Slimefun gold dust
        registerRecipe(3, new ItemStack(Material.GOLD_INGOT), SlimefunItems.GOLD_DUST);
    }

    @Override
    public String getMachineIdentifier() {
        return "ELECTRIC_INGOT_PULVERIZER";
    }

}
