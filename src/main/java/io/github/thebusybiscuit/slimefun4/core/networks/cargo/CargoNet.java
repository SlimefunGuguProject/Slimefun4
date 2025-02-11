package io.github.thebusybiscuit.slimefun4.core.networks.cargo;

import com.molean.folia.adapter.Folia;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import com.xzavier0722.mc.plugin.slimefuncomplib.event.cargo.CargoTickEvent;
import io.github.bakedlibs.dough.common.CommonPatterns;
import io.github.thebusybiscuit.slimefun4.api.network.Network;
import io.github.thebusybiscuit.slimefun4.api.network.NetworkComponent;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

/**
 * The {@link CargoNet} is a type of {@link Network} which deals with {@link ItemStack} transportation.
 * It is also an extension of {@link AbstractItemNetwork} which provides methods to deal
 * with the addon ChestTerminal.
 *
 * @author meiamsome
 * @author Poslovitch
 * @author John000708
 * @author BigBadE
 * @author SoSeDiK
 * @author TheBusyBiscuit
 * @author Walshy
 * @author DNx5
 *
 */
public class CargoNet extends AbstractItemNetwork implements HologramOwner {

    private static final int RANGE = 5;

    private final Set<Location> inputNodes = new CopyOnWriteArraySet<>();
    private final Set<Location> outputNodes = new CopyOnWriteArraySet<>();

    protected final Map<Location, Integer> roundRobin = Collections.synchronizedMap(new HashMap<>());
    private int tickDelayThreshold = 0;

    public static @Nullable CargoNet getNetworkFromLocation(@Nonnull Location l) {
        return Slimefun.getNetworkManager()
                .getNetworkFromLocation(l, CargoNet.class)
                .orElse(null);
    }

    public static @Nonnull CargoNet getNetworkFromLocationOrCreate(@Nonnull Location l) {
        Optional<CargoNet> cargoNetwork = Slimefun.getNetworkManager().getNetworkFromLocation(l, CargoNet.class);

        if (cargoNetwork.isPresent()) {
            return cargoNetwork.get();
        } else {
            CargoNet network = new CargoNet(l);
            Slimefun.getNetworkManager().registerNetwork(network);
            return network;
        }
    }

    /**
     * This constructs a new {@link CargoNet} at the given {@link Location}.
     *
     * @param l
     *            The {@link Location} marking the manager of this {@link Network}.
     */
    protected CargoNet(@Nonnull Location l) {
        super(l);
    }

    @Override
    public String getId() {
        return "CARGO_NETWORK";
    }

    @Override
    public int getRange() {
        return RANGE;
    }

    @Override
    public NetworkComponent classifyLocation(@Nonnull Location l) {
        var data = StorageCacheUtils.getBlock(l);

        if (data == null) {
            return null;
        }

        return switch (data.getSfId()) {
            case "CARGO_MANAGER" -> NetworkComponent.REGULATOR;
            case "CARGO_NODE" -> NetworkComponent.CONNECTOR;
            case "CARGO_NODE_INPUT", "CARGO_NODE_OUTPUT", "CARGO_NODE_OUTPUT_ADVANCED" -> NetworkComponent.TERMINUS;
            default -> null;
        };
    }

    @Override
    public void onClassificationChange(Location l, NetworkComponent from, NetworkComponent to) {
        connectorCache.remove(l);

        if (from == NetworkComponent.TERMINUS) {
            inputNodes.remove(l);
            outputNodes.remove(l);
        }

        if (to == NetworkComponent.TERMINUS) {
            var data = StorageCacheUtils.getBlock(l);
            switch (data.getSfId()) {
                case "CARGO_NODE_INPUT" -> inputNodes.add(l);
                case "CARGO_NODE_OUTPUT", "CARGO_NODE_OUTPUT_ADVANCED" -> outputNodes.add(l);
                default -> {}
            }
        }
    }

    public void tick(@Nonnull Block b, SlimefunBlockData blockData) {
        if (!regulator.equals(b.getLocation())) {
            updateHologram(b, "&4发现附近有多个货运网络调节机", blockData::isPendingRemove);
            return;
        }

        super.tick();

        if (connectorNodes.isEmpty() && terminusNodes.isEmpty()) {
            updateHologram(b, "&c找不到附近的货运网络节点", blockData::isPendingRemove);
        } else {
            updateHologram(b, "&7状态: &a&l已连接", blockData::isPendingRemove);

            // Skip ticking if the threshold is not reached. The delay is not same as minecraft tick,
            // but it's based on 'custom-ticker-delay' config.
            if (tickDelayThreshold < Slimefun.getCfg().getInt("networks.cargo-ticker-delay")) {
                tickDelayThreshold++;
                return;
            }

            // Reset the internal threshold, so we can start skipping again
            tickDelayThreshold = 0;

            Map<Location, Integer> inputs = mapInputNodes();
            Map<Integer, List<Location>> outputs = mapOutputNodes();

            if (StorageCacheUtils.getData(b.getLocation(), "visualizer") == null) {
                display();
            }

            Folia.runSync(
                    () -> {
                        if (blockData.isPendingRemove()) {
                            return;
                        }
                        var event = new CargoTickEvent(inputs, outputs);
                        Folia.getPluginManager().ce(event);
                        event.getHologramMsg().ifPresent(msg -> updateHologram(b, msg));
                        if (event.isCancelled()) {
                            return;
                        }

                        Slimefun.getProfiler().scheduleEntries(inputs.size() + 1);
                        new CargoNetworkTask(this, inputs, outputs).run();
                    },
                    b.getLocation());
        }
    }

    private @Nonnull Map<Location, Integer> mapInputNodes() {
        Map<Location, Integer> inputs = Collections.synchronizedMap(new HashMap<>());

        for (Location node : inputNodes) {
            int frequency = getFrequency(node);

            if (frequency >= 0 && frequency < 16) {
                inputs.put(node, frequency);
            }
        }

        return inputs;
    }

    private @Nonnull Map<Integer, List<Location>> mapOutputNodes() {
        Map<Integer, List<Location>> output = Collections.synchronizedMap(new HashMap<>());

        List<Location> list = new LinkedList<>();
        int lastFrequency = -1;

        for (Location node : outputNodes) {
            int frequency = getFrequency(node);
            if (frequency == -1) {
                continue;
            }

            if (frequency != lastFrequency && lastFrequency != -1) {
                output.merge(lastFrequency, list, (prev, next) -> {
                    prev.addAll(next);
                    return prev;
                });

                list = new LinkedList<>();
            }

            list.add(node);
            lastFrequency = frequency;
        }

        if (!list.isEmpty()) {
            output.merge(lastFrequency, list, (prev, next) -> {
                prev.addAll(next);
                return prev;
            });
        }

        return output;
    }

    /**
     * This method returns the frequency a given node is set to.
     * Should there be invalid data this method it will fall back to zero in
     * order to preserve the integrity of the {@link CargoNet}.
     *
     * @param node
     *            The {@link Location} of our cargo node
     *
     * @return The frequency of the given node
     */
    private static int getFrequency(@Nonnull Location node) {
        var data = StorageCacheUtils.getBlock(node);
        if (data == null) {
            return -1;
        }

        if (!data.isDataLoaded()) {
            StorageCacheUtils.requestLoad(data);
            return -1;
        }

        String frequency = data.getData("frequency");

        if (frequency == null) {
            return -1;
        } else if (!CommonPatterns.NUMERIC.matcher(frequency).matches()) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            () -> "Failed to parse a Cargo Node Frequency ("
                                    + node.getWorld().getName()
                                    + " - "
                                    + node.getBlockX()
                                    + ','
                                    + node.getBlockY()
                                    + ','
                                    + node.getBlockZ()
                                    + "): "
                                    + frequency);
            return -1;
        } else {
            return Integer.parseInt(frequency);
        }
    }
}
