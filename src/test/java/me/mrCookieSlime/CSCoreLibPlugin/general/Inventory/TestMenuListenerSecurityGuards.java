package me.mrCookieSlime.CSCoreLibPlugin.general.Inventory;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

class TestMenuListenerSecurityGuards {

    @BeforeAll
    static void loadServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void unloadServer() {
        MockBukkit.unmock();
    }

    private static ChestMenu createMenu() {
        ChestMenu menu = new ChestMenu("Security Test", 9);
        menu.addItem(0, new ItemStack(Material.GRAY_STAINED_GLASS_PANE), (player, slot, item, action) -> false);
        menu.addItem(1, new ItemStack(Material.IRON_INGOT));
        menu.getContents();
        return menu;
    }

    @Test
    @DisplayName("Double-click collection is blocked when it would touch a protected slot")
    void testCollectGuard() {
        ChestMenu menu = createMenu();

        Assertions.assertTrue(MenuListener.collectWouldTouchProtectedSlot(
                new ItemStack(Material.GRAY_STAINED_GLASS_PANE), menu.toInventory(), menu));
        Assertions.assertFalse(MenuListener.collectWouldTouchProtectedSlot(
                new ItemStack(Material.IRON_INGOT), menu.toInventory(), menu));
        Assertions.assertFalse(MenuListener.collectWouldTouchProtectedSlot(
                new ItemStack(Material.DIAMOND), menu.toInventory(), menu));
    }

    @Test
    @DisplayName("Inventory drags are blocked only when they touch protected top-inventory slots")
    void testDragGuard() {
        ChestMenu menu = createMenu();
        int topSize = menu.toInventory().getSize();

        Assertions.assertTrue(MenuListener.dragTouchesProtectedSlot(List.of(0, topSize + 1), topSize, menu));
        Assertions.assertFalse(MenuListener.dragTouchesProtectedSlot(List.of(1, topSize + 1), topSize, menu));
    }
}
