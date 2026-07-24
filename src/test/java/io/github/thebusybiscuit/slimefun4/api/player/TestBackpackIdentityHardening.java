package io.github.thebusybiscuit.slimefun4.api.player;

import io.github.bakedlibs.dough.common.ChatColors;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class TestBackpackIdentityHardening {

    @BeforeAll
    static void loadServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void unloadServer() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Legacy backpack identities are recognized without fragile lore assumptions")
    void testLegacyIdentityParsing() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(List.of(ChatColors.color("&7ID: 123e4567-e89b-12d3-a456-426614174000#12")));

        Assertions.assertTrue(PlayerBackpack.hasBackpackIdentity(meta));
        Assertions.assertEquals(12, PlayerBackpack.getBackpackID(meta).orElseThrow());

        meta.setLore(null);
        Assertions.assertFalse(PlayerBackpack.getBackpackID(meta).isPresent());
    }

    @Test
    @DisplayName("Malformed legacy backpack lore is ignored instead of throwing")
    void testMalformedLegacyIdentity() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.setLore(List.of(ChatColors.color("&7ID: not-a-uuid#broken")));

        Assertions.assertFalse(PlayerBackpack.hasBackpackIdentity(meta));
        Assertions.assertFalse(PlayerBackpack.getBackpackID(meta).isPresent());
    }
}
