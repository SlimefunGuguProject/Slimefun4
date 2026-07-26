package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting;

import io.github.thebusybiscuit.slimefun4.api.events.AsyncAutoEnchanterProcessEvent;
import io.github.thebusybiscuit.slimefun4.api.events.AutoEnchantEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.InventoryContext;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.integrations.AdvancedEnchantmentsIntegration;
import io.github.thebusybiscuit.slimefun4.integrations.AdvancedEnchantmentsIntegration.EnchantmentBook;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

/**
 * The {@link AutoEnchanter}, in contrast to the {@link AutoDisenchanter}, adds
 * {@link Enchantment Enchantments} from a given enchanted book and transfers them onto
 * an {@link ItemStack}.
 *
 * @author TheBusyBiscuit
 * @author Poslovitch
 * @author Mooy1
 * @author StarWishSama
 * @author martinbrom
 *
 * @see AutoDisenchanter
 *
 */
public class AutoEnchanter extends AbstractEnchantmentMachine {

    private final ItemSetting<Boolean> overrideExistingEnchantsLvl =
            new ItemSetting<>(this, "override-existing-enchants-lvl", false);

    @ParametersAreNonnullByDefault
    public AutoEnchanter(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemSetting(overrideExistingEnchantsLvl);
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.GOLDEN_CHESTPLATE);
    }

    @Override
    protected MachineRecipe findNextRecipe(BlockMenu menu) {
        for (int slot : getInputSlots()) {
            int otherSlot = slot == getInputSlots()[0] ? getInputSlots()[1] : getInputSlots()[0];
            ItemStack item = menu.getItemInSlot(otherSlot);

            if (!isEnchantable(item)) {
                continue;
            }

            AutoEnchantEvent event = new AutoEnchantEvent(item, menu.getBlock());
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                if (Slimefun.getItemStackService()
                        .fitAll(
                                menu.toInventory(),
                                new ItemStack[] {item},
                                InventoryContext.MACHINE_OUTPUT,
                                getOutputSlots())) {
                    menu.replaceExistingItem(otherSlot, null);
                    menu.pushItem(item, getOutputSlots());
                }
                return null;
            }

            ItemStack enchantedBook = menu.getItemInSlot(slot);

            if (enchantedBook != null && enchantedBook.getType() == Material.ENCHANTED_BOOK) {
                return enchant(menu, item, enchantedBook);
            }
        }

        return null;
    }

    @Nullable @ParametersAreNonnullByDefault
    protected MachineRecipe enchant(BlockMenu menu, ItemStack target, ItemStack enchantedBook) {
        AsyncAutoEnchanterProcessEvent event = new AsyncAutoEnchanterProcessEvent(target, enchantedBook, menu);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return null;
        }

        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) enchantedBook.getItemMeta();
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        Map<String, Integer> customEnchantments = new LinkedHashMap<>();

        AdvancedEnchantmentsIntegration advancedEnchantments =
                Slimefun.getIntegrations().getAdvancedEnchantments();
        EnchantmentBook customBook =
                advancedEnchantments == null ? null : advancedEnchantments.getEnchantmentBook(enchantedBook);
        if (customBook != null) {
            customEnchantments.put(customBook.enchantment(), customBook.level());
        }

        if (!isEnchantmentCountAllowed(meta.getStoredEnchants().size() + customEnchantments.size())) {
            showEnchantmentLimitWarning(menu);
            return null;
        }

        for (Map.Entry<Enchantment, Integer> entry : meta.getStoredEnchants().entrySet()) {
            if (entry.getKey().canEnchantItem(target)) {
                if (isEnchantmentLevelAllowed(entry.getValue())) {
                    enchantments.put(entry.getKey(), entry.getValue());
                    continue;
                }

                if (!menu.toInventory().getViewers().isEmpty()) {
                    showEnchantmentLevelWarning(menu);
                }

                return null;
            }
        }

        for (int level : customEnchantments.values()) {
            if (!isEnchantmentLevelAllowed(level)) {
                if (!menu.toInventory().getViewers().isEmpty()) {
                    showEnchantmentLevelWarning(menu);
                }
                return null;
            }
        }

        if (!overrideExistingEnchantsLvl.getValue()) {
            enchantments.entrySet().removeIf(entry ->
                    target.getEnchantmentLevel(entry.getKey()) >= entry.getValue());

            if (advancedEnchantments != null && !customEnchantments.isEmpty()) {
                Map<String, Integer> existingCustomEnchantments = advancedEnchantments.getEnchantments(target);
                customEnchantments.entrySet().removeIf(
                        entry -> existingCustomEnchantments.getOrDefault(entry.getKey(), 0) >= entry.getValue());
            }
        }

        int enchantmentCount = enchantments.size() + customEnchantments.size();
        if (enchantmentCount == 0) {
            return null;
        }

        ItemStack enchantedItem = target.clone();
        enchantedItem.setAmount(1);
        enchantedItem.addUnsafeEnchantments(enchantments);

        if (!customEnchantments.isEmpty()) {
            enchantedItem = advancedEnchantments.applyEnchantments(enchantedItem, customEnchantments);
            if (enchantedItem == null) {
                return null;
            }
        }

        MachineRecipe recipe = new MachineRecipe(
                75 * enchantmentCount / getSpeed(),
                new ItemStack[] {target, enchantedBook},
                new ItemStack[] {enchantedItem, new ItemStack(Material.BOOK)});

        if (!Slimefun.getItemStackService()
                .fitAll(
                        menu.toInventory(),
                        recipe.getOutput(),
                        InventoryContext.MACHINE_OUTPUT,
                        getOutputSlots())) {
            return null;
        }

        for (int inputSlot : getInputSlots()) {
            menu.consumeItem(inputSlot);
        }

        return recipe;
    }

    private boolean isEnchantable(@Nullable ItemStack item) {
        if (item != null
                && item.getType() != Material.ENCHANTED_BOOK
                && !item.getType().isAir()
                && !hasIgnoredLore(item)) {
            SlimefunItem sfItem = SlimefunItem.getByItem(item);
            return sfItem == null || sfItem.isEnchantable();
        } else {
            return false;
        }
    }

    @Override
    public String getMachineIdentifier() {
        return "AUTO_ENCHANTER";
    }
}
