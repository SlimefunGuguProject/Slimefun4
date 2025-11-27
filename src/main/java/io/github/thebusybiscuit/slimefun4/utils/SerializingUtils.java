package io.github.thebusybiscuit.slimefun4.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class SerializingUtils {
    /**
     * This method load an array of itemStack from a given {@link ConfigurationSection},
     * The structure of the {@link ConfigurationSection} should meet these rules:
     * The i-th itemStack is stored under the path "{prefixPath}_{i}"
     * The itemStack must be valid , notnull and not empty, or we will skip that itemStack
     * @param yaml
     * @param prefixPath
     * @return
     */
    public static ItemStack[] loadItemStackArray(ConfigurationSection yaml, String prefixPath) {
        List<ItemStack> itemStacks = new ArrayList<>(2);
        for(int i = 0; ; ++i){
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

    /**
     * This method save an array of itemStack to a given {@link ConfigurationSection}
     * Each element of the array must be notnull and not empty, or we will skip that itemStack
     * The saved yaml can be load back to itemStack array using {@link SerializingUtils#loadItemStackArray(ConfigurationSection, String)}
     * @param yaml
     * @param prefixPath
     * @param itemStacks
     */
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
