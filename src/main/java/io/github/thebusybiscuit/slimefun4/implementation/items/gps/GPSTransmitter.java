package io.github.thebusybiscuit.slimefun4.implementation.items.gps;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.rotations.NotDiagonallyRotatable;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public abstract class GPSTransmitter extends SimpleSlimefunItem<BlockTicker>
        implements EnergyNetComponent, NotDiagonallyRotatable {

    private final int capacity;

    @ParametersAreNonnullByDefault
    protected GPSTransmitter(
            ItemGroup itemGroup, int tier, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
        this.capacity = 4 << (2 * tier);

        addItemHandler(onPlace(), onBreak());
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Nonnull
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                StorageCacheUtils.setData(
                        e.getBlock().getLocation(),
                        "owner",
                        e.getPlayer().getUniqueId().toString());
            }
        };
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(false, false) {

            @Override
            public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                Location l = e.getBlock().getLocation();
                UUID owner = UUID.fromString(StorageCacheUtils.getData(l, "owner"));
                Slimefun.getGPSNetwork().updateTransmitter(l, owner, false);
            }
        };
    }

    public abstract int getMultiplier(int y);

    public abstract int getEnergyConsumption();

    @Override
    public BlockTicker getItemHandler() {
        return new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem item, SlimefunBlockData data) {
                int charge = getCharge(b.getLocation(), data);
                UUID owner = UUID.fromString(data.getData("owner"));

                if (charge >= getEnergyConsumption()) {
                    Slimefun.getGPSNetwork().updateTransmitter(b.getLocation(), owner, true);
                    removeCharge(b.getLocation(), getEnergyConsumption());
                } else {
                    Slimefun.getGPSNetwork().updateTransmitter(b.getLocation(), owner, false);
                }
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        };
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }
}
