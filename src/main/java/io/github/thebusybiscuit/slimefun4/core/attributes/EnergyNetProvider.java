package io.github.thebusybiscuit.slimefun4.core.attributes;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataConfigWrapper;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.AbstractEnergyProvider;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;
import javax.annotation.Nonnull;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import org.bukkit.Location;

/**
 * An {@link EnergyNetProvider} is an extension of {@link EnergyNetComponent} which provides
 * energy to an {@link EnergyNet}.
 * It must be implemented on any Generator or {@link Reactor}.
 *
 * @author TheBusyBiscuit
 * @see EnergyNet
 * @see EnergyNetComponent
 * @see AbstractEnergyProvider
 * @see AGenerator
 * @see Reactor
 */
public interface EnergyNetProvider extends EnergyNetComponent {

    @Override
    @Nonnull
    default EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.GENERATOR;
    }

    /**
     * @deprecated use {@link EnergyNetProvider#getGeneratedOutput(Location, ASlimefunDataContainer)} instead
     *
     * @param l The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored {@link SlimefunBlockData}
     * @return The generated output energy of this {@link EnergyNetProvider}
     */
    @Deprecated(forRemoval = true)
    default int getGeneratedOutput(@Nonnull Location l, @Nonnull SlimefunBlockData data) {
        return getGeneratedOutput(l, new BlockDataConfigWrapper(data));
    }

    /**
     * This method returns how much energy this {@link EnergyNetProvider} provides to the {@link EnergyNet}.
     * Stored energy does not have to be handled in here.
     * <br/>
     * if your machine outputs energy values higher than Integer.MAX_VALUE,
     * please return Integer.MAX_VALUE here
     * and override {@link EnergyNetProvider#getGeneratedOutputLong(Location, SlimefunBlockData)} instead.
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored {@link SlimefunBlockData}
     * @return The generated output energy of this {@link EnergyNetProvider}.
     */
    default int getGeneratedOutput(@Nonnull Location l, @Nonnull ASlimefunDataContainer data) {
        if (data instanceof SlimefunBlockData blockData) {
            return getGeneratedOutput(l, blockData);
        }

        throw new IllegalStateException(
                "You must implement getGeneratedOutput for " + data.getClass().getName());
    }

    /**
     * @deprecated use {@link EnergyNetProvider#getGeneratedOutputLong(Location, SlimefunBlockData)} instead
     *
     * This method only for backward compatibility.
     *
     * @param l The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored {@link Config}
     * @return The generated output energy of this {@link EnergyNetProvider}
     */
    @Deprecated(forRemoval = true)
    default int getGeneratedOutput(@Nonnull Location l, @Nonnull Config data) {
        return 0;
    }

    /**
     * @deprecated use {@link EnergyNetProvider#getGeneratedOutputLong(Location, ASlimefunDataContainer)} instead
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored block data
     * @return The generated output energy of this {@link EnergyNetProvider}.
     */
    @Deprecated(forRemoval = true)
    default long getGeneratedOutputLong(@Nonnull Location l, @Nonnull SlimefunBlockData data) {
        return getGeneratedOutput(l, (ASlimefunDataContainer) data);
    }

    /**
     * This method returns how much energy this {@link EnergyNetProvider} provides to the {@link EnergyNet}.
     * We call this method every time we tick a energy regulator, so make sure to keep it light and fast.
     * Stored energy does not have to be handled in here.
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored block data
     * @return The generated output energy of this {@link EnergyNetProvider}.
     */
    default long getGeneratedOutputLong(@Nonnull Location l, @Nonnull ASlimefunDataContainer data) {
        if (data instanceof SlimefunBlockData blockData) {
            return getGeneratedOutputLong(l, blockData);
        }

        return getGeneratedOutput(l, data);
    }

    /**
     * This method returns whether the given {@link Location} is going to explode on the
     * next tick.
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored block data
     * @return Whether or not this {@link Location} will explode.
     */
    default boolean willExplode(@Nonnull Location l, @Nonnull ASlimefunDataContainer data) {
        if (data instanceof SlimefunBlockData blockData) {
            return willExplode(l, blockData);
        }

        throw new IllegalStateException(
                "You must implement willExplode for " + data.getClass().getName());
    }

    /**
     * @deprecated Use {@link EnergyNetProvider#willExplode(Location, ASlimefunDataContainer)} instead
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored block data
     * @return Whether or not this {@link Location} will explode.
     */
    @Deprecated(forRemoval = true)
    default boolean willExplode(@Nonnull Location l, @Nonnull SlimefunBlockData data) {
        return willExplode(l, new BlockDataConfigWrapper(data));
    }

    /**
     * @deprecated use {@link EnergyNetProvider#willExplode(Location, SlimefunBlockData)} instead
     *
     * This method only for backward compatibility.
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored block data
     * @return Whether or not this {@link Location} will explode.
     */
    @Deprecated(forRemoval = true)
    default boolean willExplode(@Nonnull Location l, @Nonnull Config data) {
        return false;
    }
}
