package io.github.thebusybiscuit.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class TestDistinctiveItemComparison {

    @BeforeAll
    static void loadServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void unloadServer() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Distinctive comparisons receive the metadata from the actual two stacks")
    void testActualStackMetadataIsCompared() {
        ItemStack first = namedChest("First identity");
        ItemStack second = namedChest("Second identity");
        DistinctiveItem compareNames = new DistinctiveItem() {
            @Override
            public String getId() {
                return "TEST_DISTINCTIVE";
            }

            @Override
            public boolean canStack(ItemMeta firstMeta, ItemMeta secondMeta) {
                return firstMeta.getDisplayName().equals(secondMeta.getDisplayName());
            }
        };

        Assertions.assertFalse(SlimefunUtils.compareDistinctiveStacks(compareNames, first, second));
        Assertions.assertTrue(SlimefunUtils.compareDistinctiveStacks(compareNames, first, first.clone()));
    }

    private static ItemStack namedChest(String name) {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
