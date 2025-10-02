package io.github.thebusybiscuit.slimefun4.implementation.operations;

import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.utils.SerializingUtils;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link MachineOperation} represents a crafting process.
 *
 * @author TheBusyBiscuit
 *
 */
public class CraftingOperation implements MachineOperation {
    public static String INPUT = "input";
    public static String OUTPUT = "output";
    private final ItemStack[] ingredients;
    private final ItemStack[] results;

    private final int totalTicks;
    private int currentTicks = 0;

    public CraftingOperation(@Nonnull MachineRecipe recipe) {
        this(recipe.getInput(), recipe.getOutput(), recipe.getTicks());
    }

    public CraftingOperation(@Nonnull ItemStack[] ingredients, @Nonnull ItemStack[] results, int totalTicks) {
        Validate.notEmpty(ingredients, "The Ingredients array cannot be empty or null");
        Validate.notEmpty(results, "The results array cannot be empty or null");
        Validate.isTrue(
                totalTicks >= 0,
                "The amount of total ticks must be a positive integer or zero, received: " + totalTicks);

        this.ingredients = ingredients;
        this.results = results;
        this.totalTicks = totalTicks;
    }

    public CraftingOperation(ConfigurationSection yaml) {
        this.totalTicks = yaml.getInt(TOTAL_TICKS);
        this.results = SerializingUtils.loadItemStackArray(yaml, OUTPUT);
        this.ingredients = SerializingUtils.loadItemStackArray(yaml, INPUT);
    }

    @Override
    public void addProgress(int num) {
        Validate.isTrue(num > 0, "Progress must be positive.");
        currentTicks += num;
    }

    @Nonnull
    public ItemStack[] getIngredients() {
        return ingredients;
    }

    @Nonnull
    public ItemStack[] getResults() {
        return results;
    }

    @Override
    public int getProgress() {
        return currentTicks;
    }

    @Override
    public int getTotalTicks() {
        return totalTicks;
    }

    public void serializeOperation(ConfigurationSection yaml, CraftingOperation operation) {
        MachineOperation.super.serializeOperation(yaml, operation);
        SerializingUtils.saveItemStackArray(yaml, INPUT, getIngredients());
        SerializingUtils.saveItemStackArray(yaml, OUTPUT, getResults());
    }
}
