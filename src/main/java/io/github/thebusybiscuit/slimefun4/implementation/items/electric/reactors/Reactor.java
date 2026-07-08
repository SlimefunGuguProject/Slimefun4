package io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.ReactorExplodeEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.core.attributes.MachineProcessHolder;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.cargo.ReactorAccessPort;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.AbstractEnergyProvider;
import io.github.thebusybiscuit.slimefun4.implementation.operations.FuelOperation;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AGenerator;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineFuel;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * The abstract {@link Reactor} class is very similar to {@link AGenerator} but is
 * exclusively used for Reactors.
 *
 * @author John000708
 * @author AlexLander123
 * @author TheBusyBiscuit
 *
 * @see AGenerator
 * @see NuclearReactor
 * @see NetherStarReactor
 *
 */
public abstract class Reactor extends AbstractEnergyProvider
        implements HologramOwner, MachineProcessHolder<FuelOperation> {

    private static final String MODE = "reactor-mode";
    private static final int INFO_SLOT = 49;
    private static final int COOLANT_DURATION = 50;
    private static final BlockFace[] WATER_BLOCKS = {
        BlockFace.NORTH,
        BlockFace.NORTH_EAST,
        BlockFace.EAST,
        BlockFace.SOUTH_EAST,
        BlockFace.SOUTH,
        BlockFace.SOUTH_WEST,
        BlockFace.WEST,
        BlockFace.NORTH_WEST
    };

    private static final int[] border = {0, 1, 2, 3, 5, 6, 7, 8, 12, 13, 14, 21, 23};
    private static final int[] border_1 = {9, 10, 11, 18, 20, 27, 29, 36, 38, 45, 46, 47};
    private static final int[] border_2 = {15, 16, 17, 24, 26, 33, 35, 42, 44, 51, 52, 53};
    private static final int[] border_3 = {30, 31, 32, 39, 41, 48, 50};

    // No coolant border
    private static final int[] border_4 = {25, 34, 43};

    private final Set<Location> explosionsQueue = new HashSet<>();
    private final MachineProcessor<FuelOperation> processor = new MachineProcessor<>(this);

    @ParametersAreNonnullByDefault
    protected Reactor(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        processor.setProgressBar(getProgressBar());

        new BlockMenuPreset(getId(), getInventoryTitle()) {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                var blockData = StorageCacheUtils.getBlock(b.getLocation());
                if (blockData.getData(MODE) == null) {
                    blockData.setData(MODE, ReactorMode.GENERATOR.toString());
                }

                updateInventory(menu, b);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager()
                                .hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };

        addItemHandler(onBreak());
        registerDefaultFuelTypes();
    }

    @Override
    public MachineProcessor<FuelOperation> getMachineProcessor() {
        return processor;
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

                if (inv != null) {
                    inv.dropItems(b.getLocation(), getFuelSlots());
                    inv.dropItems(b.getLocation(), getCoolantSlots());
                    inv.dropItems(b.getLocation(), getOutputSlots());
                }

                processor.endOperation(b);
                removeHologram(b);
            }
        };
    }

    protected void updateInventory(@Nonnull BlockMenu menu, @Nonnull Block b) {
        ReactorMode mode = getReactorMode(b.getLocation());

        switch (mode) {
            case GENERATOR:
                menu.replaceExistingItem(
                        4,
                        new CustomItemStack(
                                SlimefunItems.NUCLEAR_REACTOR,
                                "&7模式: &e发电",
                                "",
                                "&6反应堆将会专注于发电",
                                "&6如果能源网络中没有机器需要电力",
                                "&6它会停止工作",
                                "",
                                "&7\u21E8 单击修改模式为 &e生产"));
                menu.addMenuClickHandler(4, (p, slot, item, action) -> {
                    StorageCacheUtils.setData(b.getLocation(), MODE, ReactorMode.PRODUCTION.toString());
                    updateInventory(menu, b);
                    return false;
                });
                break;
            case PRODUCTION:
                menu.replaceExistingItem(
                        4,
                        new CustomItemStack(
                                SlimefunItems.PLUTONIUM,
                                "&7模式: &e生产",
                                "",
                                "&6反应堆将会专注于生产副产物",
                                "&6如果能源网络中没有机器需要电力",
                                "&6它会继续工作并且不发电",
                                "",
                                "&7\u21E8 单击修改模式为 &e发电"));
                menu.addMenuClickHandler(4, (p, slot, item, action) -> {
                    StorageCacheUtils.setData(b.getLocation(), MODE, ReactorMode.GENERATOR.toString());
                    updateInventory(menu, b);
                    return false;
                });
                break;
            default:
                break;
        }

        BlockMenu port = getAccessPort(menu, b.getLocation());

        if (port != null) {
            menu.replaceExistingItem(
                    INFO_SLOT, new CustomItemStack(Material.GREEN_WOOL, "&7访问接口", "", "&6已连接", "", "&7> 单击查看访问接口"));
            menu.addMenuClickHandler(INFO_SLOT, (p, slot, item, action) -> {
                port.open(p);
                updateInventory(menu, b);

                return false;
            });
        } else {
            menu.replaceExistingItem(
                    INFO_SLOT,
                    new CustomItemStack(Material.RED_WOOL, "&7访问接口", "", "&c未连接", "", "&7接口必须要放置在", "&7反应堆上面的第三格!"));
            menu.addMenuClickHandler(INFO_SLOT, (p, slot, item, action) -> {
                updateInventory(menu, b);
                menu.open(p);
                return false;
            });
        }
    }

    private void constructMenu(@Nonnull BlockMenuPreset preset) {
        for (int i : border) {
            preset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : border_1) {
            preset.addItem(
                    i,
                    new CustomItemStack(Material.LIME_STAINED_GLASS_PANE, " "),
                    ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : border_3) {
            preset.addItem(
                    i,
                    new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, " "),
                    ChestMenuUtils.getEmptyClickHandler());
        }

        preset.addItem(
                22, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "), ChestMenuUtils.getEmptyClickHandler());

        if (this instanceof NuclearReactor) {
            preset.addItem(
                    1,
                    new CustomItemStack(getFuelIcon(), "&7燃料", "", "&f这里可以放入放射性燃料:", "&2铀 &f或 &a镎"),
                    ChestMenuUtils.getEmptyClickHandler());
        } else if (this instanceof NetherStarReactor) {
            preset.addItem(
                    1,
                    new CustomItemStack(getFuelIcon(), "&7燃料", "", "&f这里可以放入燃料:", "&b下界之星"),
                    ChestMenuUtils.getEmptyClickHandler());
        } else {
            preset.addItem(
                    1,
                    new CustomItemStack(getFuelIcon(), "&7燃料", "", "&f这里可以放入放射性燃料:", "&2铀 &f或 &a镎"),
                    ChestMenuUtils.getEmptyClickHandler());
        }

        for (int i : border_2) {
            preset.addItem(
                    i,
                    new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, " "),
                    ChestMenuUtils.getEmptyClickHandler());
        }

        if (needsCooling()) {
            preset.addItem(
                    7, new CustomItemStack(getCoolant(), "&b冷却剂", "", "&f在此处放入冷却剂", "&4没有了冷却剂, 你的反应堆", "&4将会瞬间爆炸"));
        } else {
            preset.addItem(7, new CustomItemStack(Material.BARRIER, "&b冷却剂", "", "&f在此处放入冷却剂"));

            for (int i : border_4) {
                preset.addItem(
                        i, new CustomItemStack(Material.BARRIER, "&c无需冷却剂"), ChestMenuUtils.getEmptyClickHandler());
            }
        }
    }

    @Nonnull
    protected ReactorMode getReactorMode(@Nonnull Location l) {
        ReactorMode mode = ReactorMode.GENERATOR;

        var blockData = StorageCacheUtils.getDataContainer(l);
        if (blockData != null && ReactorMode.PRODUCTION.toString().equals(blockData.getData(MODE))) {
            mode = ReactorMode.PRODUCTION;
        }

        return mode;
    }

    public abstract void extraTick(@Nonnull Location l);

    /**
     * This method returns the {@link ItemStack} that is required to cool this {@link Reactor}.
     * If it returns null, then no cooling is required.
     *
     * @return The {@link ItemStack} required to cool this {@link Reactor}
     */
    @Nullable public abstract ItemStack getCoolant();

    /**
     * This method returns the displayed icon above the fuel input slot.
     * It should reflect the {@link ItemStack} used to power the reactor.
     * This method does <b>not</b> determine the fuel input, only the icon.
     *
     * @return The {@link ItemStack} used as the fuel icon for this {@link Reactor}.
     */
    @Nonnull
    public abstract ItemStack getFuelIcon();

    /**
     * This method returns whether this {@link Reactor} requires as some form of
     * coolant.
     * It is a not-null check performed on {@link #getCoolant()}
     *
     * @return Whether this {@link Reactor} requires cooling
     */
    protected final boolean needsCooling() {
        return getCoolant() != null;
    }

    @Override
    public int[] getInputSlots() {
        return new int[] {19, 28, 37, 25, 34, 43};
    }

    public int[] getFuelSlots() {
        return new int[] {19, 28, 37};
    }

    @Nonnull
    public int[] getCoolantSlots() {
        return needsCooling() ? new int[] {25, 34, 43} : new int[0];
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] {40};
    }

    @Override
    public int getGeneratedOutput(@Nonnull Location l, @Nonnull ASlimefunDataContainer data) {
        BlockMenu inv = StorageCacheUtils.getMenu(l);
        BlockMenu accessPort = getAccessPort(inv, l);
        FuelOperation operation = processor.getOperation(l);

        if (operation != null) {
            extraTick(l);

            if (!operation.isFinished()) {
                return generateEnergy(l, data, inv, accessPort, operation);
            } else {
                createByproduct(l, inv, accessPort, operation);
                return 0;
            }
        } else {
            burnNextFuel(l, inv, accessPort);
            return 0;
        }
    }

    private int generateEnergy(
            @Nonnull Location l,
            @Nonnull ASlimefunDataContainer data,
            @Nonnull BlockMenu inv,
            @Nullable BlockMenu accessPort,
            @Nonnull FuelOperation operation) {
        int produced = getEnergyProduction();
        String energyData = data.getData("energy-charge");
        int charge = 0;

        if (energyData != null) {
            charge = Integer.parseInt(energyData);
        }

        int space = getCapacity() - charge;

        if (space >= produced || getReactorMode(l) != ReactorMode.GENERATOR) {
            operation.addProgress(1);
            checkForWaterBlocks(l);
            processor.updateProgressBar(inv, 22, operation);

            if (needsCooling() && !hasEnoughCoolant(l, inv, accessPort, operation)) {
                explosionsQueue.add(l);
                return 0;
            }
        }

        if (space >= produced) {
            return getEnergyProduction();
        } else {
            return 0;
        }
    }

    @Override
    public boolean willExplode(Location l, ASlimefunDataContainer data) {
        boolean explosion = explosionsQueue.contains(l);

        if (explosion) {
            Slimefun.runSync(() -> {
                ReactorExplodeEvent event = new ReactorExplodeEvent(l, Reactor.this);
                Bukkit.getPluginManager().callEvent(event);

                ((SlimefunBlockData) data).getBlockMenu().close();
                removeHologram(l.getBlock());
            });

            explosionsQueue.remove(l);
            processor.endOperation(l);
        }

        return explosion;
    }

    private void checkForWaterBlocks(Location l) {
        Slimefun.runSync(() -> {
            /*
             * We will pick a surrounding block at random and see if this is water.
             * If it isn't, then we will make it explode.
             */
            int index = ThreadLocalRandom.current().nextInt(WATER_BLOCKS.length);
            BlockFace randomNeighbour = WATER_BLOCKS[index];

            if (l.getBlock().getRelative(randomNeighbour).getType() != Material.WATER) {
                explosionsQueue.add(l);
            }
        });
    }

    private void createByproduct(
            @Nonnull Location l,
            @Nonnull BlockMenu inv,
            @Nullable BlockMenu accessPort,
            @Nonnull FuelOperation operation) {
        inv.replaceExistingItem(22, new CustomItemStack(Material.BLACK_STAINED_GLASS_PANE, " "));
        ItemStack result = operation.getResult();

        if (result != null) {
            inv.pushItem(result.clone(), getOutputSlots());
        }

        if (accessPort != null) {
            for (int slot : getOutputSlots()) {
                if (inv.getItemInSlot(slot) != null) {
                    inv.replaceExistingItem(
                            slot, accessPort.pushItem(inv.getItemInSlot(slot), ReactorAccessPort.getOutputSlots()));
                }
            }
        }

        processor.endOperation(l);
    }

    private void burnNextFuel(Location l, BlockMenu inv, BlockMenu accessPort) {
        Map<Integer, Integer> found = new HashMap<>();
        MachineFuel fuel = findFuel(inv, found);

        if (accessPort != null) {
            restockFuel(inv, accessPort);
        }

        if (fuel != null) {
            for (Map.Entry<Integer, Integer> entry : found.entrySet()) {
                inv.consumeItem(entry.getKey(), entry.getValue());
            }

            processor.startOperation(l, new FuelOperation(fuel));
        }
    }

    /**
     * This method cools the given {@link Reactor}.
     *
     * @param reactor
     *            The {@link Location} of this {@link Reactor}
     * @param menu
     *            The {@link Inventory} of this {@link Reactor}
     * @param accessPort
     *            The {@link ReactorAccessPort}, if available
     * @param operation
     *            The {@link FuelOperation} of this {@link Reactor}
     *
     * @return Whether the {@link Reactor} was successfully cooled, if not it should explode
     */
    private boolean hasEnoughCoolant(
            @Nonnull Location reactor,
            @Nonnull BlockMenu menu,
            @Nullable BlockMenu accessPort,
            @Nonnull FuelOperation operation) {
        boolean requiresCoolant = operation.getProgress() % COOLANT_DURATION == 0;

        if (requiresCoolant) {
            ItemStack coolant = ItemStackWrapper.wrap(getCoolant());

            if (accessPort != null) {
                for (int slot : getCoolantSlots()) {
                    if (SlimefunUtils.isItemSimilar(accessPort.getItemInSlot(slot), coolant, true, false)) {
                        ItemStack remainingItem = menu.pushItem(accessPort.getItemInSlot(slot), getCoolantSlots());
                        accessPort.replaceExistingItem(slot, remainingItem);
                    }
                }
            }

            for (int slot : getCoolantSlots()) {
                if (SlimefunUtils.isItemSimilar(menu.getItemInSlot(slot), coolant, true, false)) {
                    menu.consumeItem(slot);
                    updateHologram(reactor.getBlock(), "&b\u2744 &7100%");
                    return true;
                }
            }

            return false;
        } else {
            updateHologram(
                    reactor.getBlock(),
                    "&b\u2744 &7" + getPercentage(operation.getRemainingTicks(), operation.getTotalTicks()) + "%");
        }

        return true;
    }

    private float getPercentage(int time, int total) {
        int passed = ((total - time) % COOLANT_DURATION);
        return Math.round(((((COOLANT_DURATION - passed) * 100.0F) / COOLANT_DURATION) * 100.0F) / 100.0F);
    }

    @ParametersAreNonnullByDefault
    private void restockFuel(BlockMenu menu, BlockMenu port) {
        for (int slot : getFuelSlots()) {
            for (MachineFuel fuelType : fuelTypes) {
                if (fuelType.test(port.getItemInSlot(slot))
                        && menu.fits(new CustomItemStack(port.getItemInSlot(slot), 1), getFuelSlots())) {
                    port.replaceExistingItem(slot, menu.pushItem(port.getItemInSlot(slot), getFuelSlots()));
                    return;
                }
            }
        }
    }

    @Nullable @ParametersAreNonnullByDefault
    private MachineFuel findFuel(BlockMenu menu, Map<Integer, Integer> found) {
        for (MachineFuel fuel : fuelTypes) {
            for (int slot : getInputSlots()) {
                if (fuel.test(menu.getItemInSlot(slot))) {
                    found.put(slot, fuel.getInput().getAmount());
                    return fuel;
                }
            }
        }

        return null;
    }

    @Nullable protected BlockMenu getAccessPort(BlockMenu menu, @Nonnull Location l) {
        Location portLoc = new Location(l.getWorld(), l.getX(), l.getY() + 3, l.getZ());
        var controller = Slimefun.getDatabaseManager().getBlockDataController();
        var port = controller.getBlockData(portLoc);

        if (port == null || port.isPendingRemove()) {
            return null;
        }

        if (!port.isDataLoaded()) {
            StorageCacheUtils.executeAfterLoad(port, () -> updateInventory(menu, l.getBlock()), false);
            return null;
        }

        if (port.getSfId().equals(SlimefunItems.REACTOR_ACCESS_PORT.getItemId())) {
            return port.getBlockMenu();
        } else {
            return null;
        }
    }
}
