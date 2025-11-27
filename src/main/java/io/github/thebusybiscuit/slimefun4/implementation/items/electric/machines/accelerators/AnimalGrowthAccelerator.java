package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.accelerators;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.misc.OrganicFood;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedParticle;
import javax.annotation.Nullable;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.block.Block;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class AnimalGrowthAccelerator extends AbstractGrowthAccelerator {

    private static final int ENERGY_CONSUMPTION = 14;
    private static final double RADIUS = 3.0;

    public AnimalGrowthAccelerator(
            ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public int getCapacity() {
        return 1024;
    }

    @Override
    protected void tick(Block b) {
        BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

        for (Entity n : b.getWorld().getNearbyEntities(b.getLocation(), RADIUS, RADIUS, RADIUS, this::isReadyToGrow)) {
            for (int slot : getInputSlots()) {
                if (isOrganicFood(inv.getItemInSlot(slot))) {
                    if (getCharge(b.getLocation()) < ENERGY_CONSUMPTION) {
                        return;
                    }

                    Ageable ageable = (Ageable) n;
                    removeCharge(b.getLocation(), ENERGY_CONSUMPTION);
                    inv.consumeItem(slot);
                    ageable.setAge(ageable.getAge() + 2000);

                    if (ageable.getAge() > 0) {
                        ageable.setAge(0);
                    }

                    n.getWorld()
                            .spawnParticle(
                                    VersionedParticle.HAPPY_VILLAGER,
                                    ((LivingEntity) n).getEyeLocation(),
                                    8,
                                    0.2F,
                                    0.2F,
                                    0.2F);
                    return;
                }
            }
        }
    }

    protected boolean isOrganicFood(@Nullable ItemStack item) {
        return SlimefunItem.getByItem(item) instanceof OrganicFood;
    }

    private boolean isReadyToGrow(Entity n) {
        return n instanceof Ageable ageable && n.isValid() && !ageable.isAdult();
    }
}
