package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.enchanting;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.operations.CraftingOperationSerializable;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * This is a super class of the {@link AutoEnchanter} and {@link AutoDisenchanter} which is
 * used to streamline some methods and combine common attributes to reduce redundancy.
 *
 * @author TheBusyBiscuit
 * @author Rothes
 *
 * @see AutoEnchanter
 * @see AutoDisenchanter
 *
 */
abstract class AbstractEnchantmentMachine extends AContainer implements CraftingOperationSerializable {

    private final ItemSetting<Boolean> useLevelLimit = new ItemSetting<>(this, "use-enchant-level-limit", false);
    private final IntRangeSetting levelLimit = new IntRangeSetting(this, "enchant-level-limit", 0, 10, Short.MAX_VALUE);
    private final ItemSetting<Boolean> useIgnoredLores = new ItemSetting<>(this, "use-ignored-lores", false);
    private final ItemSetting<List<String>> ignoredLores = new ItemSetting<>(
            this, "ignored-lores", Collections.singletonList("&7- &c无法被使用在 " + this.getItemName() + "上"));
    private final ItemSetting<Integer> enchantLimit =
            new IntRangeSetting(this, "enchant-limit", 0, 10, Short.MAX_VALUE);
    private final ItemSetting<Boolean> useEnchantLimit = new ItemSetting<>(this, "use-enchant-limit", false);

    @ParametersAreNonnullByDefault
    protected AbstractEnchantmentMachine(
            ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemSetting(useLevelLimit);
        addItemSetting(levelLimit);
        addItemSetting(useIgnoredLores);
        addItemSetting(ignoredLores);
        addItemSetting(enchantLimit);
        addItemSetting(useEnchantLimit);
    }

    protected boolean isEnchantmentLevelAllowed(int enchantmentLevel) {
        return !useLevelLimit.getValue() || levelLimit.getValue() >= enchantmentLevel;
    }

    protected boolean isEnchantmentCountAllowed(int count) {
        return !useEnchantLimit.getValue() || enchantLimit.getValue() >= count;
    }

    protected void showEnchantmentLevelWarning(@Nonnull BlockMenu menu) {
        if (!useLevelLimit.getValue()) {
            throw new IllegalStateException("自动附/祛魔机等级限制未被启用, 无法展示警告信息.");
        }

        String notice = ChatColors.color(Slimefun.getLocalization().getMessage("messages.above-limit-level"));
        notice = notice.replace("%level%", String.valueOf(levelLimit.getValue()));
        ItemStack progressBar = new CustomItemStack(Material.BARRIER, " ", notice);
        menu.replaceExistingItem(22, progressBar);
    }

    protected void showEnchantmentLimitWarning(@Nonnull BlockMenu menu) {
        if (!useEnchantLimit.getValue()) {
            throw new IllegalStateException("自动附/祛魔机附魔数量限制未被启用, 无法展示警告信息.");
        }

        String notice = ChatColors.color(Slimefun.getLocalization().getMessage("messages.above-enchant-limit"));
        notice = notice.replace("%max%", String.valueOf(enchantLimit.getValue()));
        ItemStack progressBar = new CustomItemStack(Material.BARRIER, " ", notice);
        menu.replaceExistingItem(22, progressBar);
    }

    protected boolean hasIgnoredLore(@Nonnull ItemStack item) {
        if (useIgnoredLores.getValue() && item.hasItemMeta()) {
            ItemMeta itemMeta = item.getItemMeta();

            if (itemMeta.hasLore()) {
                List<String> itemLore = itemMeta.getLore();
                List<String> ignoredLore = ignoredLores.getValue();

                // Check if any of the lines are found on the item
                for (String lore : ignoredLore) {
                    if (itemLore.contains(ChatColors.color(lore))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
