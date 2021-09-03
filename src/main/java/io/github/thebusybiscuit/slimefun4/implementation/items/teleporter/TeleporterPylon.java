package io.github.thebusybiscuit.slimefun4.implementation.items.teleporter;

import io.github.thebusybiscuit.slimefun4.core.handlers.RainbowTickHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.RainbowBlock;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * The {@link TeleporterPylon} is a special kind of {@link RainbowBlock} which is required
 * for the {@link Teleporter}.
 *
 * @author TheBusyBiscuit
 * @see Teleporter
 * @see RainbowBlock
 * @see RainbowTickHandler
 */
public class TeleporterPylon extends RainbowBlock {

    @ParametersAreNonnullByDefault
    public TeleporterPylon(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(category, item, recipeType, recipe, recipeOutput, new RainbowTickHandler(Material.CYAN_STAINED_GLASS, Material.PURPLE_STAINED_GLASS));
    }

}