package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines;

import io.github.thebusybiscuit.cscorelib2.inventory.InvUtils;
import io.github.thebusybiscuit.slimefun4.api.events.AutoDisenchantEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link AutoDisenchanter}, in contrast to the {@link AutoEnchanter}, removes
 * {@link Enchantment Enchantments} from a given {@link ItemStack} and transfers them
 * to a book.
 *
 * @author TheBusyBiscuit
 * @author Walshy
 * @author poma123
 * @see AutoEnchanter
 */
public class AutoDisenchanter extends AContainer {

    private final IntRangeSetting enchantLevelLimit = new IntRangeSetting("enchant-level-limit", 0, 5, 32767);

    @ParametersAreNonnullByDefault
    public AutoDisenchanter(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);

        addItemSetting(enchantLevelLimit);
    }

    @Override
    public ItemStack getProgressBar() {
        return new ItemStack(Material.DIAMOND_CHESTPLATE);
    }

    @Override
    protected MachineRecipe findNextRecipe(BlockMenu menu) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();

        for (int slot : getInputSlots()) {
            ItemStack item = menu.getItemInSlot(slot);

            if (!isDisenchantable(item)) {
                return null;
            }

            AutoDisenchantEvent event = new AutoDisenchantEvent(item);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return null;
            }

            ItemStack target = menu.getItemInSlot(slot == getInputSlots()[0] ? getInputSlots()[1] : getInputSlots()[0]);

            // Disenchanting
            if (target != null && target.getType() == Material.BOOK) {
                int amount = 0;

                for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                    if (enchantLevelLimit.validateInput(entry.getValue())) {
                        enchantments.put(entry.getKey(), entry.getValue());
                        amount++;
                    } else {
                        if (!menu.toInventory().getViewers().isEmpty()) {
                            menu.toInventory().getViewers().forEach(p -> SlimefunPlugin.getLocalization().sendMessage(p, "messages.above-limit-level", true));
                        }
                        return null;
                    }
                }

                if (amount > 0) {
                    ItemStack disenchantedItem = item.clone();
                    disenchantedItem.setAmount(1);

                    ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
                    transferEnchantments(disenchantedItem, book, enchantments);

                    MachineRecipe recipe = new MachineRecipe(90 * amount / this.getSpeed(), new ItemStack[]{target, item}, new ItemStack[]{disenchantedItem, book});

                    if (!InvUtils.fitAll(menu.toInventory(), recipe.getOutput(), getOutputSlots())) {
                        return null;
                    }

                    for (int inputSlot : getInputSlots()) {
                        menu.consumeItem(inputSlot);
                    }

                    return recipe;
                }
            }
        }

        return null;
    }

    private void transferEnchantments(ItemStack item, ItemStack book, Map<Enchantment, Integer> enchantments) {
        ItemMeta itemMeta = item.getItemMeta();
        ItemMeta bookMeta = book.getItemMeta();
        ((Repairable) bookMeta).setRepairCost(((Repairable) itemMeta).getRepairCost());
        ((Repairable) itemMeta).setRepairCost(0);
        item.setItemMeta(itemMeta);
        book.setItemMeta(bookMeta);

        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            item.removeEnchantment(entry.getKey());
            meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
        }

        book.setItemMeta(meta);
    }

    private boolean isDisenchantable(@Nullable ItemStack item) {
        if (item == null) {
            return false;
        } else if (item.getType() != Material.BOOK) {
            // ^ This stops endless checks of getByItem for books
            SlimefunItem sfItem = SlimefunItem.getByItem(item);
            return sfItem == null || sfItem.isDisenchantable();
        } else {
            return true;
        }
    }


    @Override
    public String getMachineIdentifier() {
        return "AUTO_DISENCHANTER";
    }

    /**

     private MachineRecipe findRecipe(BlockMenu menu) {
     Map<Enchantment, Integer> enchantments = new HashMap<>();
     Set<ItemEnchantment> emeraldEnchantments = new HashSet<>();

     for (int slot : getInputSlots()) {
     ItemStack item = menu.getItemInSlot(slot);

     if (!isDisenchantable(item)) {
     return null;
     }

     AutoDisenchantEvent event = new AutoDisenchantEvent(item);
     Bukkit.getPluginManager().callEvent(event);

     if (event.isCancelled()) {
     return null;
     }

     ItemStack target = menu.getItemInSlot(slot == getInputSlots()[0] ? getInputSlots()[1] : getInputSlots()[0]);

     // Disenchanting
     if (item != null && target != null && target.getType() == Material.BOOK) {
     int amount = 0;

     for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
     if (entry.getValue() <= SlimefunPlugin.getCfg().getInt("options.enchanter-level-limit") || SlimefunPlugin.getCfg().getInt("options.enchanter-level-limit") == 0) {
     enchantments.put(entry.getKey(), entry.getValue());
     amount++;
     } else {
     if (!menu.toInventory().getViewers().isEmpty()) {
     HumanEntity p = menu.toInventory().getViewers().get(0);
     if (!noticedPlayer.contains(p.getUniqueId())) {
     SlimefunPlugin.getLocalization().sendMessage(p, "messages.above-limit-level", true);
     noticedPlayer.add(p.getUniqueId());
     }
     }
     return null;
     }
     }

     if (SlimefunPlugin.getThirdPartySupportService().isEmeraldEnchantsInstalled()) {
     for (ItemEnchantment enchantment : EmeraldEnchants.getInstance().getRegistry().getEnchantments(item)) {
     amount++;
     emeraldEnchantments.add(enchantment);
     }
     }

     if (amount > 0) {
     ItemStack disenchantedItem = item.clone();
     disenchantedItem.setAmount(1);

     ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
     transferEnchantments(disenchantedItem, book, enchantments);

     for (ItemEnchantment ench : emeraldEnchantments) {
     EmeraldEnchants.getInstance().getRegistry().applyEnchantment(book, ench.getEnchantment(), ench.getLevel());
     EmeraldEnchants.getInstance().getRegistry().applyEnchantment(disenchantedItem, ench.getEnchantment(), 0);
     }

     return new MachineRecipe(90 * amount, new ItemStack[]{target, item}, new ItemStack[]{disenchantedItem, book});
     }
     }
     }

     return null;
     }

     private void transferEnchantments(ItemStack item, ItemStack book, Map<Enchantment, Integer> enchantments) {
     ItemMeta itemMeta = item.getItemMeta();
     ItemMeta bookMeta = book.getItemMeta();
     ((Repairable) bookMeta).setRepairCost(((Repairable) itemMeta).getRepairCost());
     ((Repairable) itemMeta).setRepairCost(0);
     item.setItemMeta(itemMeta);
     book.setItemMeta(bookMeta);

     EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();

     for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
     item.removeEnchantment(entry.getKey());
     meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
     }

     book.setItemMeta(meta);
     }

     private boolean isDisenchantable(ItemStack item) {
     SlimefunItem sfItem = null;

     // stops endless checks of getByItem for empty book stacks.
     if (item != null && item.getType() != Material.BOOK) {
     sfItem = SlimefunItem.getByItem(item);
     }

     return sfItem == null || sfItem.isDisenchantable();
     }

     @Override public int getSpeed() {
     return 1;
     }

     @Override public String getMachineIdentifier() {
     return "AUTO_DISENCHANTER";
     } */

}