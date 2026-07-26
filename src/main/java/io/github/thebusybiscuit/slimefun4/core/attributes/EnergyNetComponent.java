package io.github.thebusybiscuit.slimefun4.core.attributes;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNet;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.Capacitor;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;

/**
 * This Interface, when attached to a class that inherits from {@link SlimefunItem}, marks
 * the Item as an electric Block.
 * This will make this Block interact with an {@link EnergyNet}.
 *
 * You can specify the Type of Block via {@link EnergyNetComponent#getEnergyComponentType()}.
 * You can also specify a capacity for this Block via {@link EnergyNetComponent#getCapacity()}.
 *
 * You can support your machine to store long value of energy via {@link EnergyNetComponent#getCapacityLong()}, and make {@link EnergyNetComponent#getCapacity()} returns Integer.MAX_VALUE
 *
 * @author TheBusyBiscuit
 *
 * @see EnergyNetComponentType
 * @see EnergyNet
 *
 */
@SlimefunAPI
public interface EnergyNetComponent extends ItemAttribute {

    /**
     * This method returns the Type of {@link EnergyNetComponentType} this {@link SlimefunItem} represents.
     * It describes how this Block will interact with an {@link EnergyNet}.
     *
     * @return The {@link EnergyNetComponentType} this {@link SlimefunItem} represents.
     */
    @Nonnull
    EnergyNetComponentType getEnergyComponentType();

    /**
     * This method returns the max amount of electricity this Block can hold.
     * If the capacity is zero, then this Block cannot hold any electricity.
     *
     * @return The max amount of electricity this Block can store.
     */
    default long getCapacityLong() {
        return getCapacity();
    }

    @Deprecated
    int getCapacity();

    /**
     * This returns whether this {@link EnergyNetComponent} can hold energy charges.
     * It returns true if {@link #getCapacity()} returns a number greater than zero.
     *
     * @return Whether this {@link EnergyNetComponent} can store energy.
     */
    default boolean isChargeable() {
        return getCapacityLong() > 0;
    }

    /**
     * This returns the currently stored charge at a given {@link Location}.
     *
     * @param l
     *            The target {@link Location}
     *
     * @return The charge stored at that {@link Location}
     */
    default long getChargeLong(@Nonnull Location l) {
        // Emergency fallback, this cannot hold a charge, so we'll just return zero
        if (!isChargeable()) {
            return 0;
        }

        var blockData = StorageCacheUtils.getDataContainer(l);
        if (blockData == null || blockData.isPendingRemove()) {
            return 0;
        }

        if (!blockData.isDataLoaded()) {
            StorageCacheUtils.requestLoad(blockData);
            return 0;
        }

        return getChargeLong(l, blockData);
    }

    @Deprecated
    default int getCharge(@Nonnull Location l) {
        return (int) NumberUtils.clamp(Integer.MIN_VALUE, getChargeLong(l), Integer.MAX_VALUE);
    }

    @Deprecated
    default int getCharge(@Nonnull Location l, @Nonnull Config config) {
        Slimefun.logger().log(Level.FINE, "Legacy BlockStorage method invoked; please switch to the addon's updated block storage adapter.");

        Validate.notNull(l, "Location was null!");

        // Emergency fallback, this cannot hold a charge, so we'll just return zero
        if (!isChargeable()) {
            return 0;
        }

        var blockData = StorageCacheUtils.getDataContainer(l);
        if (blockData == null || blockData.isPendingRemove()) {
            return 0;
        }

        if (!blockData.isDataLoaded()) {
            StorageCacheUtils.requestLoad(blockData);
            return 0;
        }

        return getCharge(l, blockData);
    }

    default int getCharge(@Nonnull Location l, @Nonnull ASlimefunDataContainer data) {
        return (int) NumberUtils.longToInt(getChargeLong(l, data));
    }

    @Deprecated
    default int getCharge(@Nonnull Location l, @Nonnull SlimefunBlockData data) {
        return (int) NumberUtils.longToInt(getChargeLong(l, data));
    }

    @Deprecated(forRemoval = true)
    default long getChargeLong(@Nonnull Location l, @Nonnull SlimefunBlockData data) {
        return getChargeLong(l, (ASlimefunDataContainer) data);
    }

    /**
     * This returns the currently stored charge at a given {@link Location}.
     * object for this {@link Location}.
     *
     * @param l
     *            The target {@link Location}
     * @param data
     *            The data at this {@link Location}
     *
     * @return The charge stored at that {@link Location}
     */
    default long getChargeLong(@Nonnull Location l, @Nonnull ASlimefunDataContainer data) {
        Validate.notNull(l, "Location was null!");
        Validate.notNull(data, "data was null!");

        // Emergency fallback, this cannot hold a charge, so we'll just return zero
        if (!isChargeable()) {
            return 0;
        }

        String charge = data.getData("energy-charge");

        if (charge != null) {
            // parseLong compatible with old int values
            return Long.parseLong(charge);
        } else {
            return 0;
        }
    }

    @Deprecated
    default void setCharge(@Nonnull Location l, int charge) {
        setCharge(l, (long) charge);
    }

    /**
     * Sets the charge stored at a location using the modern long-capacity API.
     *
     * @param l
     *            The target location
     * @param charge
     *            The new charge
     */
    default void setCharge(@Nonnull Location l, long charge) {
        Validate.notNull(l, "Location was null!");
        Validate.isTrue(charge >= 0, "You can only set a charge of zero or more!");

        ASlimefunDataContainer data = getLoadedDataContainer(l);
        if (data != null) {
            setCharge(l, charge, data);
        }
    }

    /**
     * Sets the charge using an already resolved storage container.
     *
     * <p>This overload avoids repeated storage lookups in energy networks and machine tickers.
     *
     * @param l
     *            The target location
     * @param charge
     *            The new charge
     * @param data
     *            The already resolved storage container
     */
    default void setCharge(
            @Nonnull Location l, long charge, @Nonnull ASlimefunDataContainer data) {
        Validate.notNull(l, "Location was null!");
        Validate.notNull(data, "data was null!");
        Validate.isTrue(charge >= 0, "You can only set a charge of zero or more!");

        try {
            long capacity = getCapacityLong();

            if (capacity > 0 && data.isDataLoaded() && !data.isPendingRemove()) {
                long newCharge = NumberUtils.clamp(0, charge, capacity);

                if (newCharge != getChargeLong(l, data)) {
                    data.setData("energy-charge", String.valueOf(newCharge));
                    updateCapacitorTexture(l, newCharge, capacity);
                }
            }
        } catch (Exception | LinkageError x) {
            logEnergyFailure("set", l, x);
        }
    }

    @Deprecated
    default void addCharge(@Nonnull Location l, int charge) {
        addCharge(l, (long) charge);
    }

    /**
     * Adds charge using the modern long-capacity API.
     *
     * @param l
     *            The target location
     * @param charge
     *            The positive charge to add
     */
    default void addCharge(@Nonnull Location l, long charge) {
        Validate.notNull(l, "Location was null!");
        Validate.isTrue(charge > 0, "You can only add a positive charge!");

        ASlimefunDataContainer data = getLoadedDataContainer(l);
        if (data != null) {
            addCharge(l, charge, data);
        }
    }

    /**
     * Adds charge using an already resolved storage container.
     *
     * @param l
     *            The target location
     * @param charge
     *            The positive charge to add
     * @param data
     *            The already resolved storage container
     */
    default void addCharge(
            @Nonnull Location l, long charge, @Nonnull ASlimefunDataContainer data) {
        Validate.notNull(l, "Location was null!");
        Validate.notNull(data, "data was null!");
        Validate.isTrue(charge > 0, "You can only add a positive charge!");

        try {
            long capacity = getCapacityLong();

            if (capacity > 0 && data.isDataLoaded() && !data.isPendingRemove()) {
                long currentCharge = getChargeLong(l, data);

                if (currentCharge < capacity) {
                    long newCharge = NumberUtils.flowSafeAddition(capacity, currentCharge, charge);
                    data.setData("energy-charge", String.valueOf(newCharge));
                    updateCapacitorTexture(l, newCharge, capacity);
                }
            }
        } catch (Exception | LinkageError x) {
            logEnergyFailure("add", l, x);
        }
    }

    @Deprecated
    default void removeCharge(@Nonnull Location l, int charge) {
        removeCharge(l, (long) charge);
    }

    /**
     * Removes charge using the modern long-capacity API.
     *
     * @param l
     *            The target location
     * @param charge
     *            The positive charge to remove
     */
    default void removeCharge(@Nonnull Location l, long charge) {
        Validate.notNull(l, "Location was null!");
        Validate.isTrue(charge > 0, "The charge to remove must be greater than zero!");

        ASlimefunDataContainer data = getLoadedDataContainer(l);
        if (data != null) {
            removeCharge(l, charge, data);
        }
    }

    /**
     * Removes charge using an already resolved storage container.
     *
     * @param l
     *            The target location
     * @param charge
     *            The positive charge to remove
     * @param data
     *            The already resolved storage container
     */
    default void removeCharge(
            @Nonnull Location l, long charge, @Nonnull ASlimefunDataContainer data) {
        Validate.notNull(l, "Location was null!");
        Validate.notNull(data, "data was null!");
        Validate.isTrue(charge > 0, "The charge to remove must be greater than zero!");

        try {
            long capacity = getCapacityLong();

            if (capacity > 0 && data.isDataLoaded() && !data.isPendingRemove()) {
                long currentCharge = getChargeLong(l, data);

                if (currentCharge > 0) {
                    long newCharge = Math.max(0, currentCharge - charge);
                    data.setData("energy-charge", String.valueOf(newCharge));
                    updateCapacitorTexture(l, newCharge, capacity);
                }
            }
        } catch (Exception | LinkageError x) {
            logEnergyFailure("remove", l, x);
        }
    }

    private ASlimefunDataContainer getLoadedDataContainer(Location l) {
        ASlimefunDataContainer data = StorageCacheUtils.getDataContainer(l);

        if (data == null || data.isPendingRemove()) {
            return null;
        }

        if (!data.isDataLoaded()) {
            StorageCacheUtils.requestLoad(data);
            return null;
        }

        return data;
    }

    private void updateCapacitorTexture(Location l, long charge, long capacity) {
        if (getEnergyComponentType() == EnergyNetComponentType.CAPACITOR) {
            SlimefunUtils.updateCapacitorTexture(l, (double) charge / capacity);
        }
    }

    private void logEnergyFailure(String operation, Location l, Throwable throwable) {
        Slimefun.logger()
                .log(
                        Level.SEVERE,
                        throwable,
                        () -> "Exception while trying to "
                                + operation
                                + " the energy-charge for \""
                                + getId()
                                + "\" at "
                                + new BlockPosition(l));
    }
}
