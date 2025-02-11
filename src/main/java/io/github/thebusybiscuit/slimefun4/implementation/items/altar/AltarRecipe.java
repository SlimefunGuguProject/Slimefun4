package io.github.thebusybiscuit.slimefun4.implementation.items.altar;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.inventory.ItemStack;

public class AltarRecipe {

    private final ItemStack catalyst;
    private final List<ItemStack> input;
    private final ItemStack output;

    public AltarRecipe(List<ItemStack> input, ItemStack output) {
        this.catalyst = input.get(4);
        this.input = new CopyOnWriteArrayList<>();

        this.input.add(input.get(0));
        this.input.add(input.get(1));
        this.input.add(input.get(2));
        this.input.add(input.get(5));

        this.input.add(input.get(8));
        this.input.add(input.get(7));
        this.input.add(input.get(6));
        this.input.add(input.get(3));

        this.output = output;
    }

    public ItemStack getCatalyst() {
        return this.catalyst;
    }

    public ItemStack getOutput() {
        return this.output;
    }

    public List<ItemStack> getInput() {
        return this.input;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AltarRecipe ar) {
            return ar.getCatalyst().equals(getCatalyst())
                    && ar.getInput().equals(getInput())
                    && ar.getOutput().equals(getOutput());
        } else {
            return false;
        }
    }
}
