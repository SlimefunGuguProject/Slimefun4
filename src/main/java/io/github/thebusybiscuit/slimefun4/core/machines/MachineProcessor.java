package io.github.thebusybiscuit.slimefun4.core.machines;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.api.events.AsyncMachineOperationFinishEvent;
import io.github.thebusybiscuit.slimefun4.api.events.AsyncMachineOperationStartEvent;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessSerializable;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

/**
 * A {@link MachineProcessor} manages different {@link MachineOperation}s and handles
 * their progress.
 *
 * @author TheBusyBiscuit
 *
 * @param <T>
 *            The type of {@link MachineOperation} this processor can hold.
 *
 * @see MachineOperation
 * @see MachineProcessHolder
 */
public class MachineProcessor<T extends MachineOperation> {

    private final Map<BlockPosition, T> machines = new ConcurrentHashMap<>();
    private final MachineProcessHolder<T> owner;
    private final MachineProcessSerializable<T> optionalSerializer;

    private ItemStack progressBar;

    /**
     * This creates a new {@link MachineProcessor}.
     *
     * @param owner
     *            The owner of this {@link MachineProcessor}.
     */
    public MachineProcessor(@Nonnull MachineProcessHolder<T> owner) {
        Validate.notNull(owner, "The MachineProcessHolder cannot be null.");

        this.owner = owner;
        this.optionalSerializer =
                this.owner instanceof MachineProcessSerializable<T> serializable ? serializable : null;
    }

    /**
     * This returns the owner of this {@link MachineProcessor}.
     *
     * @return The owner / holder
     */
    @Nonnull
    public MachineProcessHolder<T> getOwner() {
        return owner;
    }

    /**
     * This returns the progress bar icon for this {@link MachineProcessor}
     * or null if no progress bar was set.
     *
     * @return The progress bar icon or null
     */
    @Nullable public ItemStack getProgressBar() {
        return progressBar;
    }

    /**
     * This sets the progress bar icon for this {@link MachineProcessor}.
     * You can also set it to null to clear the progress bar.
     *
     * @param progressBar
     *            An {@link ItemStack} or null
     */
    public void setProgressBar(@Nullable ItemStack progressBar) {
        this.progressBar = progressBar;
    }

    /**
     * This method will start a {@link MachineOperation} at the given {@link Location}.
     *
     * @param loc
     *            The {@link Location} at which our machine is located.
     * @param operation
     *            The {@link MachineOperation} to start
     *
     * @return Whether the {@link MachineOperation} was successfully started. This will return false if another
     *         {@link MachineOperation} has already been started at that {@link Location}.
     */
    public boolean startOperation(@Nonnull Location loc, @Nonnull T operation) {
        Validate.notNull(loc, "The location must not be null");
        Validate.notNull(operation, "The operation cannot be null");

        return startOperation(new BlockPosition(loc), operation);
    }

    /**
     * This method will start a {@link MachineOperation} at the given {@link Block}.
     *
     * @param b
     *            The {@link Block} at which our machine is located.
     * @param operation
     *            The {@link MachineOperation} to start
     *
     * @return Whether the {@link MachineOperation} was successfully started. This will return false if another
     *         {@link MachineOperation} has already been started at that {@link Block}.
     */
    public boolean startOperation(@Nonnull Block b, @Nonnull T operation) {
        Validate.notNull(b, "The Block must not be null");
        Validate.notNull(operation, "The machine operation cannot be null");

        return startOperation(new BlockPosition(b), operation);
    }

    /**
     * This method will actually start the {@link MachineOperation}.
     *
     * @param pos
     *            The {@link BlockPosition} of our machine
     * @param operation
     *            The {@link MachineOperation} to start
     *
     * @return Whether the {@link MachineOperation} was successfully started. This will return false if another
     *         {@link MachineOperation} has already been started at that {@link BlockPosition} or the StartEvent is cancelled.
     */
    public boolean startOperation(@Nonnull BlockPosition pos, @Nonnull T operation) {
        Validate.notNull(pos, "The BlockPosition must not be null");
        Validate.notNull(operation, "The machine operation cannot be null");
        // async Machine Operation Start Event

        var currentOperation = machines.computeIfAbsent(pos, (ps) -> {
            // only if the current operation if absent and Event is not cancelled  can we put the new operation in the
            // map
            // other wise it will keep null
            var event = new AsyncMachineOperationStartEvent(ps, this, operation);
            if (event.callEvent()) {
                return operation;
            } else {
                return null;
            }
        });
        // if the current Operation is successfully put into the map, returns true
        if (currentOperation == operation) {
            // serialize and save to blockData
            if (optionalSerializer != null) {
                SlimefunBlockData blockData = StorageCacheUtils.getBlock(pos.toLocation());
                if (blockData != null) {
                    StorageCacheUtils.executeAfterLoad(
                            blockData,
                            () -> {
                                blockData.setData(
                                        MachineProcessSerializable.KEY_OPERATION_INFO,
                                        optionalSerializer.serialize(pos, operation));
                                blockData.setData(
                                        MachineProcessSerializable.KEY_PROGRESS_LEFT,
                                        String.valueOf(operation.getRemainingTicks()));
                            },
                            false);
                }
            }
            return true;
        }

        return false;
    }

    /**
     * This returns the current {@link MachineOperation} at that given {@link Location}.
     *
     * @param loc
     *            The {@link Location} at which our machine is located.
     *
     * @return The current {@link MachineOperation} or null.
     */
    @Nullable public T getOperation(@Nonnull Location loc) {
        Validate.notNull(loc, "The location cannot be null");

        return getOperation(new BlockPosition(loc));
    }

    /**
     * This returns the current {@link MachineOperation} at that given {@link Block}.
     *
     * @param b
     *            The {@link Block} at which our machine is located.
     *
     * @return The current {@link MachineOperation} or null.
     */
    @Nullable public T getOperation(@Nonnull Block b) {
        Validate.notNull(b, "The Block cannot be null");

        return getOperation(new BlockPosition(b));
    }

    /**
     * This returns the current {@link MachineOperation} at that given {@link BlockPosition}.
     *
     * @param pos
     *            The {@link BlockPosition} at which our machine is located.
     *
     * @return The current {@link MachineOperation} or null.
     */
    @Nullable public T getOperation(@Nonnull BlockPosition pos) {
        Validate.notNull(pos, "The BlockPosition must not be null");

        T value = machines.get(pos);
        if (value != null) {
            if (optionalSerializer != null) {
                // try update the progressLeft field
                SlimefunBlockData sfdata = StorageCacheUtils.getBlock(pos.toLocation());
                if (sfdata != null) {
                    StorageCacheUtils.executeAfterLoad(
                            sfdata,
                            () -> sfdata.setData(
                                    MachineProcessSerializable.KEY_PROGRESS_LEFT,
                                    String.valueOf(value.getRemainingTicks())),
                            false);
                }
            }
        } else {
            if (optionalSerializer != null) {
                // try load if operation is absent
                SlimefunBlockData sfdata = StorageCacheUtils.getBlock(pos.toLocation());
                if (sfdata != null && sfdata.isDataLoaded()) {
                    // this may not be multithread-safe, but who cares?
                    String infoYaml = sfdata.getData(MachineProcessSerializable.KEY_OPERATION_INFO);
                    if (infoYaml != null) {
                        T operationLoaded;
                        try {
                            operationLoaded = Objects.requireNonNull(optionalSerializer.deserialize(pos, infoYaml));
                        } finally {
                            sfdata.removeData(MachineProcessSerializable.KEY_OPERATION_INFO);
                        }
                        String progress = sfdata.getData(MachineProcessSerializable.KEY_PROGRESS_LEFT);
                        int progressTickLeft;
                        try {
                            progressTickLeft =
                                    progress == null ? operationLoaded.getTotalTicks() : Integer.parseInt(progress);
                        } catch (Throwable e) {
                            progressTickLeft = operationLoaded.getTotalTicks();
                        }
                        operationLoaded.addProgress(operationLoaded.getTotalTicks() - progressTickLeft);
                        sfdata.setData(
                                MachineProcessSerializable.KEY_OPERATION_INFO,
                                optionalSerializer.serialize(pos, operationLoaded));
                        sfdata.setData(
                                MachineProcessSerializable.KEY_PROGRESS_LEFT,
                                String.valueOf(operationLoaded.getRemainingTicks()));
                        machines.put(pos, operationLoaded);
                    }
                }
            }
        }
        return value;
    }

    /**
     * This will end the {@link MachineOperation} at the given {@link Location}.
     *
     * @param loc
     *            The {@link Location} at which our machine is located.
     *
     * @return Whether the {@link MachineOperation} was successfully ended. This will return false if there was no
     *         {@link MachineOperation} to begin with.
     */
    public boolean endOperation(@Nonnull Location loc) {
        Validate.notNull(loc, "The location should not be null");

        return endOperation(new BlockPosition(loc));
    }

    /**
     * This will end the {@link MachineOperation} at the given {@link Block}.
     *
     * @param b
     *            The {@link Block} at which our machine is located.
     *
     * @return Whether the {@link MachineOperation} was successfully ended. This will return false if there was no
     *         {@link MachineOperation} to begin with.
     */
    public boolean endOperation(@Nonnull Block b) {
        Validate.notNull(b, "The Block should not be null");

        return endOperation(new BlockPosition(b));
    }

    /**
     * This will end the {@link MachineOperation} at the given {@link BlockPosition}.
     *
     * @param pos
     *            The {@link BlockPosition} at which our machine is located.
     *
     * @return Whether the {@link MachineOperation} was successfully ended. This will return false if there was no
     *         {@link MachineOperation} to begin with.
     */
    public boolean endOperation(@Nonnull BlockPosition pos) {
        Validate.notNull(pos, "The BlockPosition cannot be null");
        // remove the serialized data from the blockData
        if (optionalSerializer != null) {
            SlimefunBlockData sfdata = StorageCacheUtils.getBlock(pos.toLocation());
            if (sfdata != null) {
                StorageCacheUtils.executeAfterLoad(
                        sfdata,
                        () -> {
                            sfdata.removeData(MachineProcessSerializable.KEY_PROGRESS_LEFT);
                            sfdata.removeData(MachineProcessSerializable.KEY_OPERATION_INFO);
                        },
                        false);
            }
        }

        T operation = machines.remove(pos);

        if (operation != null) {
            /*
             * Only call an event if the operation actually finished.
             * If it was ended prematurely (aka aborted), then we don't call any event.
             */
            if (operation.isFinished()) {
                Event event = new AsyncMachineOperationFinishEvent(pos, this, operation);
                Bukkit.getPluginManager().callEvent(event);
            } else {
                operation.onCancel(pos);
            }

            return true;
        } else {
            return false;
        }
    }

    public void updateProgressBar(@Nonnull BlockMenu inv, int slot, @Nonnull T operation) {
        Validate.notNull(inv, "The inventory must not be null.");
        Validate.notNull(operation, "The MachineOperation must not be null.");

        if (getProgressBar() == null) {
            // No progress bar, no need to update anything.
            return;
        }

        // Update the progress bar in our inventory (if anyone is watching)
        int remainingTicks = operation.getRemainingTicks();
        int totalTicks = operation.getTotalTicks();

        // Fixes #3538 - If the operation is finished, we don't need to update the progress bar.
        if (remainingTicks > 0 || totalTicks > 0) {
            ChestMenuUtils.updateProgressbar(inv, slot, remainingTicks, totalTicks, getProgressBar());
        }
    }
}
