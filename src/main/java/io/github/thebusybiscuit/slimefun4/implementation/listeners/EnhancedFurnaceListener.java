package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.EnhancedFurnace;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import io.papermc.lib.PaperLib;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link Listener} is responsible for enforcing the "fuel efficiency" and "fortune" policies
 * of an {@link EnhancedFurnace}.
 *
 * @author TheBusyBiscuit
 *
 * @see EnhancedFurnace
 *
 */
public class EnhancedFurnaceListener implements Listener {

    public EnhancedFurnaceListener(@Nonnull Slimefun plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFuelBurn(FurnaceBurnEvent e) {
        if (e.getBlock().getType() != Material.FURNACE) {
            // We don't care about Smokers, Blast Furnaces and all that fancy stuff
            return;
        }

        SlimefunItem furnace = StorageCacheUtils.getSlimefunItem(e.getBlock().getLocation());

        // Fixes #2958
        if (furnace instanceof EnhancedFurnace enhancedFurnace
                && !enhancedFurnace.isDisabledIn(e.getBlock().getWorld())
                && enhancedFurnace.getFuelEfficiency() > 0) {
            int burnTime = e.getBurnTime();
            int newBurnTime = enhancedFurnace.getFuelEfficiency() * burnTime;

            e.setBurnTime(Math.min(newBurnTime, Short.MAX_VALUE - 1));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSmelt(FurnaceSmeltEvent e) {
        if (e.getBlock().getType() != Material.FURNACE) {
            // We don't care about Smokers, Blast Furnaces and all that fancy stuff
            return;
        }

        SlimefunItem sfItem = StorageCacheUtils.getSlimefunItem(e.getBlock().getLocation());

        if (sfItem instanceof EnhancedFurnace enhancedFurnace
                && !enhancedFurnace.isDisabledIn(e.getBlock().getWorld())) {
            BlockState state = PaperLib.getBlockState(e.getBlock(), false).getState();

            if (state instanceof Furnace furnace) {
                FurnaceInventory inventory = furnace.getInventory();

                // This if statement fixes #3741
                if (inventory.getSmelting() == null) {
                    return;
                }

                boolean multiplier = SlimefunTag.ENHANCED_FURNACE_LUCK_MATERIALS.isTagged(
                        inventory.getSmelting().getType());
                if (multiplier) {
                    // fix issue #1013: only multiplier = true should we override the result
                    int amount = enhancedFurnace.getRandomOutputAmount();
                    if (amount > 1) {
                        Optional<ItemStack> result =
                                Slimefun.getMinecraftRecipeService().getFurnaceOutput(inventory.getSmelting());

                        if (result.isPresent()) {
                            ItemStack item = result.get();
                            ItemStack previousResult = e.getResult();
                            // fix issue #1013: we should respect other plugin's modification,
                            // if current result is empty or doesn't match the calculated result, then it may be
                            // modified by datapack or other plugins, do not multiply the result
                            if (previousResult != null
                                    && !previousResult.getType().isAir()
                                    && SlimefunUtils.isItemSimilar(previousResult, item, true, false)) {
                                int previous = inventory.getResult() != null
                                        ? inventory.getResult().getAmount()
                                        : 0;
                                amount = Math.min(item.getMaxStackSize() - previous, amount);
                                e.setResult(new ItemStack(item.getType(), amount));
                            }
                        }
                    }
                }
            }
        }
    }
}
