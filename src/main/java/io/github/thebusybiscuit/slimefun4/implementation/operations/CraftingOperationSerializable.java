package io.github.thebusybiscuit.slimefun4.implementation.operations;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessSerializable;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

public interface CraftingOperationSerializable extends MachineProcessSerializable<CraftingOperation> {

    default CraftingOperation deserialize(BlockPosition position, String output) {
        return deserializeOperation(output);
    }

    static CraftingOperation deserializeOperation(String yamlStr) {
        var yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(yamlStr);
        } catch (InvalidConfigurationException e) {
            return null;
        }
        return new CraftingOperation(yaml);
    }

    /**
     * this called when a MachineProcessor trys to save a MachineOperation when startOperation called, it will store the return value in KEY_OPERATION-INFO
     * @param position
     * @param operation
     * @return
     */
    default String serialize(BlockPosition position, CraftingOperation operation) {
        var yaml = new YamlConfiguration();
        operation.serializeOperation(yaml, operation);
        return yaml.saveToString();
    }
}
