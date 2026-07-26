package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting;

import io.github.thebusybiscuit.slimefun4.api.events.AutoDisenchantEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.InventoryContext;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.integrations.AdvancedEnchantmentsIntegration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

/**
 * The {@link AutoDisenchanter}, in contrast to the {@link AutoEnchanter}, removes
 * {@link Enchantment Enchantments} from a given {@link ItemStack} and transfers them
 * to a book.
 *
 * @author TheBusyBiscuit
 * @author Poslovitch
 * @author John000708
 * @author Walshy
 * @author poma123
 * @author mrcoffee1026
 * @author VoidAngel
 * @author StarWishSama
 *
 * @see AutoEnchanter
 *
 */
public class AutoDisenchanter extends AbstractEnchantmentMachine {

    @ParametersAreNonnullByDefault
    public AutoDisenchanter(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.DIAMOND_CHESTPLATE);
    }

    @Override
    protected MachineRecipe findNextRecipe(BlockMenu menu) {
        for (int slot : getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);

            if (!isDisenchantable(item)) {
                continue;
            }

            AutoDisenchantEvent event = new AutoDisenchantEvent(item, menu.getBlock());
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                if (Slimefun.getItemStackService()
                        .fitAll(
                                menu.toInventory(),
                                new ItemStack[] {item},
                                InventoryContext.MACHINE_OUTPUT,
                                getOutputSlots())) {
                    menu.replaceExistingItem(slot, null);
                    menu.pushItem(item, getOutputSlots());
                }
                return null;
            }

            ItemStack secondItem =
                    menu.getItemInSlot(slot == getInputSlots()[0] ? getInputSlots()[1] : getInputSlots()[0]);

            if (secondItem != null && secondItem.getType() == Material.BOOK) {
                return disenchant(menu, item, secondItem);
            }
        }

        return null;
    }

    @ParametersAreNonnullByDefault
    protected @Nullable MachineRecipe disenchant(BlockMenu menu, ItemStack item, ItemStack book) {
        AdvancedEnchantmentsIntegration advancedEnchantments =
                Slimefun.getIntegrations().getAdvancedEnchantments();
        Map<String, Integer> customEnchantments = advancedEnchantments == null
                ? Collections.emptyMap()
                : advancedEnchantments.getEnchantments(item);

        if (!isEnchantmentCountAllowed(item.getEnchantments().size() + customEnchantments.size())) {
            showEnchantmentLimitWarning(menu);
            return null;
        }

        for (int level : customEnchantments.values()) {
            if (!isEnchantmentLevelAllowed(level)) {
                if (!menu.toInventory().getViewers().isEmpty()) {
                    showEnchantmentLevelWarning(menu);
                }
                return null;
            }
        }

        if (!customEnchantments.isEmpty()) {
            Map.Entry<String, Integer> enchantment =
                    customEnchantments.entrySet().iterator().next();
            Map<String, Integer> extracted = Collections.singletonMap(enchantment.getKey(), enchantment.getValue());

            ItemStack disenchantedItem = advancedEnchantments.removeEnchantments(item, extracted);
            ItemStack enchantedBook =
                    advancedEnchantments.createEnchantmentBook(enchantment.getKey(), enchantment.getValue());
            if (disenchantedItem == null || enchantedBook == null) {
                return null;
            }
            disenchantedItem.setAmount(1);

            return createRecipe(menu, item, book, disenchantedItem, enchantedBook, 1);
        }

        Map<Enchantment, Integer> enchantments = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            if (isEnchantmentLevelAllowed(entry.getValue())) {
                enchantments.put(entry.getKey(), entry.getValue());
                continue;
            }

            if (!menu.toInventory().getViewers().isEmpty()) {
                showEnchantmentLevelWarning(menu);
            }

            return null;
        }

        if (enchantments.isEmpty()) {
            return null;
        }

        ItemStack disenchantedItem = item.clone();
        disenchantedItem.setAmount(1);
        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
        transferEnchantments(disenchantedItem, enchantedBook, enchantments);

        return createRecipe(menu, item, book, disenchantedItem, enchantedBook, enchantments.size());
    }

    @ParametersAreNonnullByDefault
    private @Nullable MachineRecipe createRecipe(
            BlockMenu menu,
            ItemStack item,
            ItemStack book,
            ItemStack disenchantedItem,
            ItemStack enchantedBook,
            int enchantmentCount) {
        MachineRecipe recipe = new MachineRecipe(
                Math.max(1, 90 * enchantmentCount / getSpeed()),
                new ItemStack[] {book, item},
                new ItemStack[] {disenchantedItem, enchantedBook});

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

    @ParametersAreNonnullByDefault
    protected void transferEnchantments(ItemStack item, ItemStack book, Map<Enchantment, Integer> enchantments) {
        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta bookMeta = book.getItemMeta();

        if (itemMeta instanceof Repairable itemRepairable && bookMeta instanceof Repairable bookRepairable) {
            bookRepairable.setRepairCost(itemRepairable.getRepairCost());
            itemRepairable.setRepairCost(0);
            book.setItemMeta(bookMeta);
        }

        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantmentToTransfer = entry.getKey();
            boolean wasEnchantmentRemoved = itemMeta.removeEnchant(enchantmentToTransfer);
            boolean stillHasEnchantment = itemMeta.getEnchants().containsKey(enchantmentToTransfer);

            if (wasEnchantmentRemoved && !stillHasEnchantment) {
                meta.addStoredEnchant(enchantmentToTransfer, entry.getValue(), true);
            } else {
                Slimefun.logger()
                        .log(
                                Level.SEVERE,
                                "AutoDisenchanter has failed to remove enchantment \"{0}\"",
                                enchantmentToTransfer.getKey().getKey());
            }
        }

        item.setItemMeta(itemMeta);
        book.setItemMeta(meta);
    }

    private boolean isDisenchantable(@Nullable ItemStack item) {
        if (item != null && !item.getType().isAir() && item.getType() != Material.BOOK && !hasIgnoredLore(item)) {
            SlimefunItem sfItem = SlimefunItem.getByItem(item);
            return sfItem == null || sfItem.isDisenchantable();
        } else {
            return false;
        }
    }

    @Override
    public String getMachineIdentifier() {
        return "AUTO_DISENCHANTER";
    }
}
