package io.github.thebusybiscuit.slimefun4.core.attributes;

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
     * This method returns how much energy this {@link EnergyNetProvider} provides to the {@link EnergyNet}.
     * We call this method every time we tick a energy regulator, so make sure to keep it light and fast.
     * Stored energy does not have to be handled in here.
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored block data
     * @return The generated output energy of this {@link EnergyNetProvider}.
     */
    default long getGeneratedOutputLong(@Nonnull Location l, @Nonnull SlimefunBlockData data) {
        return getGeneratedOutput(l, data);
    }

    /**
     * This method returns how much energy this {@link EnergyNetProvider} provides to the {@link EnergyNet}.
     * Stored energy does not have to be handled in here.
     * if your machine returns a long value of generated output, please return Integer.MAX_VALUE and override {@link EnergyNetProvider#getGeneratedOutputLong(Location, SlimefunBlockData)}
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored block data
     * @return The generated output energy of this {@link EnergyNetProvider}.
     */
    default int getGeneratedOutput(@Nonnull Location l, @Nonnull SlimefunBlockData data) {
        return getGeneratedOutput(l, new BlockDataConfigWrapper(data));
    }

    /**
     * please do not use this method, override {@link EnergyNetProvider#getGeneratedOutput(Location, SlimefunBlockData)} please
     * @param l
     * @param data
     * @return
     */
    @Deprecated(forRemoval = true)
    default int getGeneratedOutput(@Nonnull Location l, @Nonnull Config data) {
        return 0;
    }

    /**
     * This method returns whether the given {@link Location} is going to explode on the
     * next tick.
     *
     * @param l    The {@link Location} of this {@link EnergyNetProvider}
     * @param data The stored block data
     * @return Whether or not this {@link Location} will explode.
     */
    default boolean willExplode(@Nonnull Location l, @Nonnull SlimefunBlockData data) {
        return willExplode(l, new BlockDataConfigWrapper(data));
    }

    @Deprecated(forRemoval = true)
    default boolean willExplode(@Nonnull Location l, @Nonnull Config data) {
        return false;
    }
}
