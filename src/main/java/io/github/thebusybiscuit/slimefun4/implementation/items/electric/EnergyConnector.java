package io.github.thebusybiscuit.slimefun4.implementation.items.electric;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.rotations.NotRotatable;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * This {@link EnergyNetComponent} is a connector for the {@link EnergyNet} networks.
 * They work similar to {@link Capacitor capacitors}.
 *
 * @author Linox
 *
 * @see EnergyNet
 * @see EnergyNetComponent
 *
 */
public class EnergyConnector extends SimpleSlimefunItem<BlockUseHandler> implements EnergyNetComponent, NotRotatable {

    @ParametersAreNonnullByDefault
    public EnergyConnector(
            ItemGroup itemGroup,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);
    }

    @Override
    public @Nonnull BlockUseHandler getItemHandler() {
        return e -> {
            if (!e.getClickedBlock().isPresent()) {
                return;
            }

            Player p = e.getPlayer();
            Block b = e.getClickedBlock().get();

            if (EnergyNet.getNetworkFromLocation(b.getLocation()) != null) {
                Slimefun.getLocalization().sendMessage(p, "machines.CARGO_NODES.connection-status-connected", false);
            } else {
                Slimefun.getLocalization().sendMessage(p, "machines.CARGO_NODES.connection-status-disconnected", false);
            }
        };
    }

    @Override
    public final @Nonnull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONNECTOR;
    }

    @Override
    public int getCapacity() {
        return 0;
    }
}
