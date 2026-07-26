package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSpawnReason;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.InventoryContext;
import io.github.thebusybiscuit.slimefun4.core.networks.NetworkManager;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * The {@link CargoNetworkTask} is the actual {@link Runnable} responsible for moving {@link ItemStack ItemStacks}
 * around the {@link CargoNet}.
 *
 * Inbefore this was just a method in the {@link CargoNet} class.
 * However for aesthetic reasons but mainly to prevent the Cargo Task from showing up as
 * "lambda:xyz-123" in timing reports... this was moved.
 *
 * @see CargoNet
 * @see CargoUtils
 * @see AbstractItemNetwork
 *
 */
class CargoNetworkTask implements Runnable {

    private final NetworkManager manager;
    private final CargoNet network;
    private final Map<Location, Inventory> inventories = new HashMap<>();
    private final Map<Location, Optional<Block>> attachedBlocks = new HashMap<>();

    private final Map<Location, Integer> inputs;
    private final Map<Integer, List<Location>> outputs;

    @ParametersAreNonnullByDefault
    CargoNetworkTask(CargoNet network, Map<Location, Integer> inputs, Map<Integer, List<Location>> outputs) {
        this.network = network;
        this.manager = Slimefun.getNetworkManager();

        this.inputs = Map.copyOf(inputs);
        Map<Integer, List<Location>> outputSnapshot = new HashMap<>();
        outputs.forEach((frequency, locations) -> outputSnapshot.put(frequency, List.copyOf(locations)));
        this.outputs = Map.copyOf(outputSnapshot);
    }

    @Override
    public void run() {
        long networkTimestamp = Slimefun.getProfiler().newEntry();

        try {
            SlimefunItem inputNode = SlimefunItems.CARGO_INPUT_NODE.getItem();
            for (Map.Entry<Location, Integer> entry : inputs.entrySet()) {
                long nodeTimestamp = Slimefun.getProfiler().newEntry();
                try {
                    Location input = entry.getKey();
                    getAttachedBlock(input).ifPresent(block -> routeItems(input, block, entry.getValue(), outputs));
                } catch (Exception | LinkageError ex) {
                    Slimefun.logger().log(
                            Level.SEVERE,
                            ex,
                            () -> "An Exception was caught while routing Cargo input node @ "
                                    + new BlockPosition(entry.getKey()));
                } finally {
                    long childTime = Slimefun.getProfiler().closeEntry(entry.getKey(), inputNode, nodeTimestamp);
                    if (networkTimestamp != 0) {
                        networkTimestamp += childTime;
                    }
                }
            }
        } catch (Exception | LinkageError x) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "An Exception was caught while ticking a Cargo network @ "
                                    + new BlockPosition(network.getRegulator()));
        } finally {
            Slimefun.getProfiler().closeEntry(
                    network.getRegulator(), SlimefunItems.CARGO_MANAGER.getItem(), networkTimestamp);
        }
    }

    @ParametersAreNonnullByDefault
    private void routeItems(
            Location inputNode, Block inputTarget, int frequency, Map<Integer, List<Location>> outputNodes) {
        ItemStackAndInteger slot = CargoUtils.withdraw(network, inventories, inputNode.getBlock(), inputTarget);

        if (slot == null) {
            return;
        }

        ItemStack stack = slot.getItem();
        int previousSlot = slot.getInt();
        List<Location> destinations = outputNodes.get(frequency);

        if (destinations != null) {
            stack = distributeItem(stack, inputNode, destinations);
        }

        if (stack != null) {
            insertItem(inputTarget, previousSlot, stack);
        }
    }

    @ParametersAreNonnullByDefault
    private void insertItem(Block inputTarget, int previousSlot, ItemStack item) {
        Inventory inv = inventories.get(inputTarget.getLocation());

        if (inv != null) {
            ItemStack rest;

            // Check if the original slot hasn't been occupied in the meantime
            if (inv.getItem(previousSlot) == null) {
                rest = Slimefun.getItemStackService().addItem(inv, item, InventoryContext.CARGO_INSERT, previousSlot);
                if (rest != null) {
                    rest = Slimefun.getItemStackService().addItem(inv, rest, InventoryContext.CARGO_INSERT);
                }
            } else {
                // Try to add the item into another available slot then
                rest = Slimefun.getItemStackService().addItem(inv, item, InventoryContext.CARGO_INSERT);
            }

            if (rest != null && !manager.isItemDeletionEnabled()) {
                // If the item still couldn't be inserted, simply drop it on the ground
                SlimefunUtils.spawnItem(inputTarget.getLocation().add(0, 1, 0), rest, ItemSpawnReason.CARGO_OVERFLOW);
            }
        } else {
            DirtyChestMenu menu = CargoUtils.getChestMenu(inputTarget);

            if (menu != null) {
                if (menu.getItemInSlot(previousSlot) == null) {
                    menu.replaceExistingItem(previousSlot, item);
                } else if (!manager.isItemDeletionEnabled()) {
                    SlimefunUtils.spawnItem(
                            inputTarget.getLocation().add(0, 1, 0), item, ItemSpawnReason.CARGO_OVERFLOW);
                }
            }
        }
    }

    @Nullable @ParametersAreNonnullByDefault
    private ItemStack distributeItem(ItemStack stack, Location inputNode, List<Location> outputNodes) {
        if (outputNodes.isEmpty()) {
            return stack;
        }

        ItemStack item = stack;
        var blockData = StorageCacheUtils.getBlock(inputNode);
        boolean roundRobin = blockData != null && Objects.equals(blockData.getData("round-robin"), "true");
        boolean smartFill = blockData != null && Objects.equals(blockData.getData("smart-fill"), "true");

        int size = outputNodes.size();
        int startIndex = roundRobin ? Math.floorMod(network.roundRobin.getOrDefault(inputNode, 0), size) : 0;

        for (int offset = 0; offset < size; offset++) {
            int outputIndex = roundRobin ? (startIndex + offset) % size : offset;
            Location output = outputNodes.get(outputIndex);
            Optional<Block> target = getAttachedBlock(output);

            if (target.isEmpty()) {
                continue;
            }

            ItemStackWrapper wrapper = ItemStackWrapper.wrap(item);
            item = CargoUtils.insert(
                    network, inventories, output.getBlock(), target.get(), smartFill, item, wrapper);

            if (item == null) {
                if (roundRobin) {
                    network.roundRobin.put(inputNode, (outputIndex + 1) % size);
                }
                return null;
            }
        }

        return item;
    }

    private Optional<Block> getAttachedBlock(Location node) {
        return attachedBlocks.computeIfAbsent(node, network::getAttachedBlock);
    }

}
