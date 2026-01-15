package io.github.thebusybiscuit.slimefun4.implementation.items.electric;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.attributes.RecipeDisplayItem;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import org.apache.commons.lang.Validate;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * This is an abstract super class for machines that produce energy.
 *
 * @author TheBusyBiscuit
 * @see AGenerator
 * @see Reactor
 */
public abstract class AbstractEnergyProvider extends SlimefunItem
        implements InventoryBlock, RecipeDisplayItem, EnergyNetProvider {

    /**
     * The set of fuel types for this energy provider.
     */
    protected final Set<MachineFuel> fuelTypes = new HashSet<>();

    /**
     * Constructs a new AbstractEnergyProvider.
     *
     * @param itemGroup   The item group this item belongs to
     * @param item        The item stack for this energy provider
     * @param recipeType  The recipe type used to craft this item
     * @param recipe      The recipe to craft this item
     */
    @ParametersAreNonnullByDefault
    protected AbstractEnergyProvider(
            ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    /**
     * This method returns the title that is used for the {@link Inventory} of an
     * {@link AbstractEnergyProvider} that has been opened by a Player.
     *
     * Override this method to set the title.
     *
     * @return The title of the {@link Inventory} of this {@link AbstractEnergyProvider}
     */
    @Nonnull
    public String getInventoryTitle() {
        return getItemName();
    }

    /**
     * This method returns the {@link ItemStack} that this {@link AGenerator} will
     * use as a progress bar.
     *
     * Override this method to set the progress bar.
     *
     * @return The {@link ItemStack} to use as the progress bar
     */
    @Nonnull
    public abstract ItemStack getProgressBar();

    /**
     * This method returns the amount of energy that is produced per tick.
     *
     * @return The rate of energy generation
     */
    public abstract int getEnergyProduction();

    /**
     * This method is used to register the default fuel types.
     */
    protected abstract void registerDefaultFuelTypes();

    @Override
    @Nonnull
    public final EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.GENERATOR;
    }

    /**
     * Registers a fuel type for this energy provider.
     *
     * @param fuel The {@link MachineFuel} to register
     */
    public void registerFuel(@Nonnull MachineFuel fuel) {
        Validate.notNull(fuel, "Machine Fuel cannot be null!");
        fuelTypes.add(fuel);
    }

    /**
     * Gets the set of fuel types for this energy provider.
     *
     * @return The set of {@link MachineFuel} types
     */
    @Nonnull
    public Set<MachineFuel> getFuelTypes() {
        return fuelTypes;
    }

    @Override
    public String getLabelLocalPath() {
        return "guide.tooltips.recipes.generator";
    }

    @Override
    public List<ItemStack> getDisplayRecipes() {
        List<ItemStack> list = new ArrayList<>();

        for (MachineFuel fuel : fuelTypes) {
            ItemStack item = fuel.getInput().clone();
            ItemMeta im = item.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(ChatColors.color("&8\u21E8 &7持续时间 " + NumberUtils.getTimeLeft(fuel.getTicks() / 2)));
            lore.add(ChatColors.color("&8\u21E8 &e\u26A1 &7" + getEnergyProduction() * 2) + " J/s");
            lore.add(ChatColors.color("&8\u21E8 &e\u26A1 &7最大储存量: "
                    + NumberUtils.getCompactDouble((double) fuel.getTicks() * getEnergyProduction())
                    + " J"));
            im.setLore(lore);
            item.setItemMeta(im);
            list.add(item);
        }

        return list;
    }
}
