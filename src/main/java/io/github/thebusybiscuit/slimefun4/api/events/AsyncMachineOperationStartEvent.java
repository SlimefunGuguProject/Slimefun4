package io.github.thebusybiscuit.slimefun4.api.events;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This {@link Event} is fired whenever an {@link MachineProcessor} wants to start a {@link MachineOperation} when invoking any of the {@link MachineProcessor#startOperation} method
 * The event is cancellable, if the event is cancelled, the operation will not be added to the operation map and {@link MachineProcessor#startOperation} will return false
 *
 * @author m1919810
 *
 */
public class AsyncMachineOperationStartEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    private final BlockPosition position;
    private final MachineProcessor<?> machineProcessor;
    private final MachineOperation machineOperation;
    private boolean cancel;

    public <T extends MachineOperation> AsyncMachineOperationStartEvent(
            BlockPosition pos, MachineProcessor<T> processor, T operation) {
        super(!Bukkit.isPrimaryThread());

        this.position = pos;
        this.machineProcessor = processor;
        this.machineOperation = operation;
    }

    /**
     * This returns the {@link BlockPosition} of the machine.
     *
     * @return The {@link BlockPosition} of the machine
     */
    @Nonnull
    public BlockPosition getPosition() {
        return position;
    }

    /**
     * The {@link MachineProcessor} instance of the machine.
     *
     * @return The {@link MachineProcessor} instance of the machine
     */
    @Nullable public MachineProcessor<?> getProcessor() {
        return machineProcessor;
    }

    /**
     * This returns the used {@link MachineOperation} in the process.
     *
     * @return The {@link MachineOperation} of the process
     */
    @Nullable public MachineOperation getOperation() {
        return machineOperation;
    }

    @Nonnull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Nonnull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    /**
     * This returns whether the event is cancelled
     *
     * @return cancel flag
     */
    @Override
    public boolean isCancelled() {
        return this.cancel;
    }

    /**
     * This sets the cancel flag of the event
     * If the event is cancelled, the operation will not be added to the operation map and {@link MachineProcessor#startOperation} will return false
     *
     * @param b new flag
     */
    @Override
    public void setCancelled(boolean b) {
        this.cancel = b;
    }
}
