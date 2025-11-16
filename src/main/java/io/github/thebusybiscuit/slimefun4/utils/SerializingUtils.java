package io.github.thebusybiscuit.slimefun4.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class SerializingUtils {
    public static ItemStack[] loadItemStackArray(ConfigurationSection yaml, String prefixPath) {
        List<ItemStack> itemStacks = new ArrayList<>(2);
        for (int i = 0; i < 666_666; ++i) {
            String keyI = prefixPath + "_" + String.valueOf(i);
            if (yaml.contains(keyI)) {
                ItemStack stack = yaml.getItemStack(keyI);
                // all Exceptions while loading will not be handled
                if (stack != null && !stack.getType().isAir()) itemStacks.add(stack);
            } else {
                break;
            }
        }
        return itemStacks.toArray(ItemStack[]::new);
    }

    public static void saveItemStackArray(ConfigurationSection yaml, String prefixPath, ItemStack... itemStacks) {
        List<ItemStack> results = Arrays.stream(itemStacks)
                .filter(Objects::nonNull)
                .filter(itemStack -> !itemStack.getType().isAir())
                .toList();
        for (var i = 0; i < results.size(); ++i) {
            yaml.set(prefixPath + "_" + String.valueOf(i), results.get(i));
        }
    }
}
