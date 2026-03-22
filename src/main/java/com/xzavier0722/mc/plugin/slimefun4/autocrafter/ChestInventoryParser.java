package com.xzavier0722.mc.plugin.slimefun4.autocrafter;

import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.InventoryContext;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.autocrafters.AbstractAutoCrafter;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * This is the implementation of {@link CrafterInteractable} with vanilla inventory.
 *
 * @author Xzavier0722
 *
 */
public class ChestInventoryParser implements CrafterInteractable {
    private final Inventory inv;

    public ChestInventoryParser(Inventory inv) {
        this.inv = inv;
    }

    @Override
    public boolean canOutput(ItemStack item) {
        return isFit(item);
    }

    @Override
    public boolean matchRecipe(
            AbstractAutoCrafter crafter,
            Collection<Predicate<ItemStack>> recipe,
            Map<Integer, Integer> itemQuantities) {
        for (Predicate<ItemStack> predicate : recipe) {
            // Check if any Item matches the Predicate
            if (!crafter.matchesAny(inv, itemQuantities, predicate)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return inv.getItem(slot);
    }

    @Override
    public boolean addItem(ItemStack item) {
        return Slimefun.getItemStackService().addItem(inv, item, InventoryContext.MACHINE_OUTPUT) == null;
    }

    private boolean isFit(ItemStack item) {
        return Slimefun.getItemStackService().fits(inv, item, InventoryContext.MACHINE_OUTPUT);
    }
}
