package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;

public interface MachineProcessSerializable<T extends MachineOperation> extends MachineProcessHolder<T> {
    String KEY_PROGRESS_LEFT = "p-tick-left";
    String KEY_OPERATION_INFO = "p-op-info";

    /**
     * this called when a MachineProcessor trys to load a MachineOperation from a blockData by using the value under the key {@link MachineProcessSerializable#KEY_OPERATION_INFO} as the operation information and {@link MachineProcessSerializable#KEY_PROGRESS_LEFT} as the progress left
     * @param position
     * @param output
     * @return
     */
    T deserialize(BlockPosition position, String output);

    /**
     * this called when a MachineProcessor trys to save a MachineOperation when startOperation called, it will store the return value under key {@link  MachineProcessSerializable#KEY_OPERATION_INFO}
     *
     * @param position
     * @param operation
     * @return
     */
    String serialize(BlockPosition position, T operation);
}
