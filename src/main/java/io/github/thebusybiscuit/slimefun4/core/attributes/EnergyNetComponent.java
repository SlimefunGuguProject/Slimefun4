package io.github.thebusybiscuit.slimefun4.core.attributes;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.blocks.BlockPosition;
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
        Slimefun.logger().log(Level.FINE, "正在调用旧 BlockStorage 的方法, 建议使用对应附属的新方块存储适配版.");

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
     * This method sets the charge which is stored at a given {@link Location}
     * If this {@link EnergyNetComponent} is of type {@code EnergyNetComponentType.CAPACITOR}, then
     * this method will automatically update the texture of this {@link Capacitor} as well.
     *
     * @param l
     *            The target {@link Location}
     * @param charge
     *            The new charge
     */
    default void setCharge(@Nonnull Location l, long charge) {
        Validate.notNull(l, "Location was null!");
        Validate.isTrue(charge >= 0, "You can only set a charge of zero or more!");

        try {
            long capacity = getCapacity();

            // This method only makes sense if we can actually store energy
            if (capacity > 0) {
                charge = NumberUtils.clamp(0, charge, capacity);

                // Do we even need to update the value?
                if (charge != getCharge(l)) {
                    var blockData = StorageCacheUtils.getDataContainer(l);

                    if (blockData == null || blockData.isPendingRemove()) {
                        return;
                    }

                    if (!blockData.isDataLoaded()) {
                        StorageCacheUtils.requestLoad(blockData);
                        return;
                    }

                    blockData.setData("energy-charge", String.valueOf(charge));

                    // Update the capacitor texture
                    if (getEnergyComponentType() == EnergyNetComponentType.CAPACITOR) {
                        SlimefunUtils.updateCapacitorTexture(l, (double) charge / capacity);
                    }
                }
            }
        } catch (Exception | LinkageError x) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "Exception while trying to set the energy-charge for \""
                                    + getId()
                                    + "\" at "
                                    + new BlockPosition(l));
        }
    }

    @Deprecated
    default void addCharge(@Nonnull Location l, int charge) {
        addCharge(l, (long) charge);
    }

    default void addCharge(@Nonnull Location l, long charge) {
        Validate.notNull(l, "Location was null!");
        Validate.isTrue(charge > 0, "You can only add a positive charge!");

        try {
            long capacity = getCapacityLong();

            // This method only makes sense if we can actually store energy
            if (capacity > 0) {
                long currentCharge = getChargeLong(l);

                // Check if there is even space for new energy
                if (currentCharge < capacity) {
                    long newCharge = NumberUtils.flowSafeAddition(capacity, currentCharge, charge);
                    StorageCacheUtils.setData(l, "energy-charge", String.valueOf(newCharge));

                    // Update the capacitor texture
                    if (getEnergyComponentType() == EnergyNetComponentType.CAPACITOR) {
                        SlimefunUtils.updateCapacitorTexture(l, (double) newCharge / capacity);
                    }
                }
            }
        } catch (Exception | LinkageError x) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "Exception while trying to add an energy-charge for \""
                                    + getId()
                                    + "\" at "
                                    + new BlockPosition(l));
        }
    }

    @Deprecated
    default void removeCharge(@Nonnull Location l, int charge) {
        removeCharge(l, (long) charge);
    }

    default void removeCharge(@Nonnull Location l, long charge) {
        Validate.notNull(l, "Location was null!");
        Validate.isTrue(charge > 0, "The charge to remove must be greater than zero!");

        try {
            long capacity = getCapacityLong();

            // This method only makes sense if we can actually store energy
            if (capacity > 0) {
                long currentCharge = getChargeLong(l);

                // Check if there is even energy stored
                if (currentCharge > 0) {
                    long newCharge = Math.max(0, currentCharge - charge);
                    StorageCacheUtils.setData(l, "energy-charge", String.valueOf(newCharge));

                    // Update the capacitor texture
                    if (getEnergyComponentType() == EnergyNetComponentType.CAPACITOR) {
                        SlimefunUtils.updateCapacitorTexture(l, (double) newCharge / capacity);
                    }
                }
            }
        } catch (Exception | LinkageError x) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "Exception while trying to remove an energy-charge for \""
                                    + getId()
                                    + "\" at "
                                    + new BlockPosition(l));
        }
    }
}
