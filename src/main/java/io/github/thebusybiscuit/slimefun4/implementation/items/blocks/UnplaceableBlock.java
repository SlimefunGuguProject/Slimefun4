package io.github.thebusybiscuit.slimefun4.implementation.items.blocks;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This is a simple {@link SlimefunItem} implementation which implements the {@link NotPlaceable}
 * attribute and also cancels any {@link PlayerRightClickEvent}.
 * Therefore making this an {@link UnplaceableBlock}.
 *
 * @author TheBusyBiscuit
 *
 * @see NotPlaceable
 *
 */
public class UnplaceableBlock extends SimpleSlimefunItem<ItemUseHandler> implements NotPlaceable {

    @ParametersAreNonnullByDefault
    public UnplaceableBlock(Category category, SlimefunItemStack item, RecipeType recipeType, @Nullable ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @ParametersAreNonnullByDefault
    public UnplaceableBlock(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
        super(category, item, recipeType, recipe, recipeOutput);
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return PlayerRightClickEvent::cancel;
    }

}