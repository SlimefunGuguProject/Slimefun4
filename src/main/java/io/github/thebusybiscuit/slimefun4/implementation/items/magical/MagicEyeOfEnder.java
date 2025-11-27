package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * The {@link MagicEyeOfEnder} allows you to launch an {@link EnderPearl}
 * out of thin air as long as you are wearing Ender Armor.
 *
 * @author TheBusyBiscuit
 *
 */
public class MagicEyeOfEnder extends SimpleSlimefunItem<ItemUseHandler> {

    @ParametersAreNonnullByDefault
    public MagicEyeOfEnder(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @Nonnull ItemUseHandler getItemHandler() {
        return e -> {
            e.cancel();

            Player p = e.getPlayer();

            if (hasArmor(p.getInventory())) {
                p.launchProjectile(EnderPearl.class);
                SoundEffect.MAGICAL_EYE_OF_ENDER_USE_SOUND.playFor(p);
            } else {
                // add message to warn player
                Slimefun.getLocalization()
                        .sendMessage(p, "messages.usage", true, msg -> msg.replace("%usage%", "穿戴全套末影护甲以发射珍珠"));
            }
        };
    }

    private boolean hasArmor(@Nonnull PlayerInventory inv) {
        // @formatter:off
        return SlimefunItem.getByItem(inv.getHelmet()) == SlimefunItems.ENDER_HELMET.getItem()
                && SlimefunItem.getByItem(inv.getChestplate()) == SlimefunItems.ENDER_CHESTPLATE.getItem()
                && SlimefunItem.getByItem(inv.getLeggings()) == SlimefunItems.ENDER_LEGGINGS.getItem()
                && SlimefunItem.getByItem(inv.getBoots()) == SlimefunItems.ENDER_BOOTS.getItem();
        // @formatter:on
    }
}
