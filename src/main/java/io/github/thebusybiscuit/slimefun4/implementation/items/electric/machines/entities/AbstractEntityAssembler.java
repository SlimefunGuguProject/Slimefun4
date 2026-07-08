package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.BlockPlacerPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This is an abstract super class for Entity Assemblers.
 *
 * @param <T> the type of {@link Entity} this assembler spawns
 * @author TheBusyBiscuit
 * @see WitherAssembler
 * @see IronGolemAssembler
 */
public abstract class AbstractEntityAssembler<T extends Entity> extends SimpleSlimefunItem<BlockTicker>
        implements EnergyNetComponent {

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_OFFSET = "offset";

    private final int[] border = {0, 2, 3, 4, 5, 6, 8, 12, 14, 21, 23, 30, 32, 39, 40, 41};
    private final int[] inputSlots = {19, 28, 25, 34};

    private final int[] headSlots = {19, 28};
    private final int[] headBorder = {9, 10, 11, 18, 20, 27, 29, 36, 37, 38};

    private final int[] bodySlots = {25, 34};
    private final int[] bodyBorder = {15, 16, 17, 24, 26, 33, 35, 42, 43, 44};

    private int lifetime = 0;

    /**
     * Constructs a new AbstractEntityAssembler.
     *
     * @param itemGroup   The item group this item belongs to
     * @param item        The item stack for this entity assembler
     * @param recipeType  The recipe type used to craft this item
     * @param recipe      The recipe to craft this item
     */
    @ParametersAreNonnullByDefault
    protected AbstractEntityAssembler(
            ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        new BlockMenuPreset(getId(), item.getItemMetaSnapshot().getDisplayName().orElse("Entity Assembler")) {

            @Override
            public void init() {
                drawBackground(border);
                drawBackground(new CustomItemStack(getHeadBorder(), " "), headBorder);
                drawBackground(new CustomItemStack(getBodyBorder(), " "), bodyBorder);

                constructMenu(this);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                updateBlockInventory(menu, b);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager()
                                .hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return inputSlots;
                } else {
                    return new int[0];
                }
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                if (flow == ItemTransportFlow.INSERT && item != null) {
                    if (item.getType() == getBody().getType()) {
                        return bodySlots;
                    }

                    if (item.getType() == getHead().getType()) {
                        return headSlots;
                    }
                }

                return new int[0];
            }
        };

        addItemHandler(onPlace(), onBreak());
    }

    @Nonnull
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(true) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                onPlace(e);
            }

            @Override
            public void onBlockPlacerPlace(BlockPlacerPlaceEvent e) {
                onPlace(e);
            }

            private void onPlace(BlockEvent e) {
                var blockData = StorageCacheUtils.getBlock(e.getBlock().getLocation());
                blockData.setData(KEY_OFFSET, "3.0");
                blockData.setData(KEY_ENABLED, String.valueOf(false));
            }
        };
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(false, false) {

            @Override
            public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                Block b = e.getBlock();
                BlockMenu inv = StorageCacheUtils.getMenu(b.getLocation());

                if (inv != null) {
                    inv.dropItems(b.getLocation(), headSlots);
                    inv.dropItems(b.getLocation(), bodySlots);
                }
            }
        };
    }

    private void updateBlockInventory(BlockMenu menu, Block b) {
        var blockData = StorageCacheUtils.getBlock(b.getLocation());
        String val;
        if (blockData == null || (val = blockData.getData(KEY_ENABLED)) == null || val.equals(String.valueOf(false))) {
            menu.replaceExistingItem(22, new CustomItemStack(Material.GUNPOWDER, "&7是否可用: &4\u2718", "", "&e> 单击开启机器"));
            menu.addMenuClickHandler(22, (p, slot, item, action) -> {
                StorageCacheUtils.setData(b.getLocation(), KEY_ENABLED, String.valueOf(true));
                updateBlockInventory(menu, b);
                return false;
            });
        } else {
            menu.replaceExistingItem(22, new CustomItemStack(Material.REDSTONE, "&7是否可用: &2\u2714", "", "&e> 单击关闭机器"));
            menu.addMenuClickHandler(22, (p, slot, item, action) -> {
                StorageCacheUtils.setData(b.getLocation(), KEY_ENABLED, String.valueOf(false));
                updateBlockInventory(menu, b);
                return false;
            });
        }

        val = null;
        double offset =
                (blockData == null || (val = blockData.getData(KEY_OFFSET)) == null) ? 3.0F : Double.parseDouble(val);

        menu.replaceExistingItem(
                31,
                new CustomItemStack(
                        Material.PISTON, "&7生成高度: &3" + offset + " 格方块高", "", "&f左键: &7+0.1", "&f右键: &7-0.1"));
        menu.addMenuClickHandler(31, (p, slot, item, action) -> {
            double offsetv =
                    NumberUtils.reparseDouble(Double.parseDouble(StorageCacheUtils.getData(b.getLocation(), KEY_OFFSET))
                            + (action.isRightClicked() ? -0.1F : 0.1F));
            StorageCacheUtils.setData(b.getLocation(), KEY_OFFSET, String.valueOf(offsetv));
            updateBlockInventory(menu, b);
            return false;
        });
    }

    @Override
    public BlockTicker getItemHandler() {
        return new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, SlimefunBlockData data) {
                if ("false".equals(data.getData(KEY_ENABLED))) {
                    return;
                }

                if (lifetime % 60 == 0 && getCharge(b.getLocation(), data) >= getEnergyConsumption()) {
                    BlockMenu menu = data.getBlockMenu();

                    boolean hasBody = findResource(menu, getBody(), bodySlots);
                    boolean hasHead = findResource(menu, getHead(), headSlots);

                    if (hasBody && hasHead) {
                        consumeResources(menu);

                        removeCharge(b.getLocation(), getEnergyConsumption());
                        double offset = Double.parseDouble(data.getData(KEY_OFFSET));

                        Slimefun.runSync(() -> {
                            Location loc =
                                    new Location(b.getWorld(), b.getX() + 0.5D, b.getY() + offset, b.getZ() + 0.5D);
                            spawnEntity(loc);

                            b.getWorld()
                                    .playEffect(
                                            b.getLocation(),
                                            Effect.STEP_SOUND,
                                            getHead().getType());
                        });
                    }
                }
            }

            @Override
            public void uniqueTick() {
                lifetime++;
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        };
    }

    private boolean findResource(BlockMenu menu, ItemStack item, int[] slots) {
        int found = 0;

        for (int slot : slots) {
            if (SlimefunUtils.isItemSimilar(menu.getItemInSlot(slot), item, true, false)) {
                found += menu.getItemInSlot(slot).getAmount();

                if (found >= item.getAmount()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void consumeResources(BlockMenu inv) {
        int bodyCount = getBody().getAmount();
        int headCount = getHead().getAmount();

        for (int slot : bodySlots) {
            if (SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), getBody(), true, false)) {
                int amount = inv.getItemInSlot(slot).getAmount();

                if (amount >= bodyCount) {
                    inv.consumeItem(slot, bodyCount);
                    break;
                } else {
                    bodyCount -= amount;
                    inv.replaceExistingItem(slot, null);
                }
            }
        }

        for (int slot : headSlots) {
            if (SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), getHead(), true, false)) {
                int amount = inv.getItemInSlot(slot).getAmount();

                if (amount >= headCount) {
                    inv.consumeItem(slot, headCount);
                    break;
                } else {
                    headCount -= amount;
                    inv.replaceExistingItem(slot, null);
                }
            }
        }
    }

    /**
     * Constructs the menu preset for this entity assembler.
     *
     * @param preset The {@link BlockMenuPreset} to construct
     */
    protected void constructMenu(BlockMenuPreset preset) {
        preset.addItem(
                1,
                new CustomItemStack(getHead(), "&7在此处放入头颅", "", "&f此处可以放入作为生成实体头颅的物品"),
                ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(
                7,
                new CustomItemStack(getBody(), "&7在此处放入组装原料", "", "&f此处可以放入作为生成实体躯干的物品"),
                ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(
                13,
                new CustomItemStack(Material.CLOCK, "&7冷却时间: &b30 秒", "", "&f这个机器需要半分钟的时间装配", "&f所以耐心等等吧!"),
                ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    /**
     * Returns the amount of energy consumed per assembly operation.
     *
     * @return The energy consumption
     */
    public abstract int getEnergyConsumption();

    /**
     * Returns the item used as the head/ingredient for entity assembly.
     *
     * @return The head item
     */
    public abstract ItemStack getHead();

    /**
     * Returns the item used as the body/ingredient for entity assembly.
     *
     * @return The body item
     */
    public abstract ItemStack getBody();

    /**
     * Returns the material used for the head border in the GUI.
     *
     * @return The head border material
     */
    public abstract Material getHeadBorder();

    /**
     * Returns the material used for the body border in the GUI.
     *
     * @return The body border material
     */
    public abstract Material getBodyBorder();

    /**
     * Spawns the entity at the given location.
     *
     * @param l The location to spawn the entity
     * @return The spawned entity
     */
    public abstract T spawnEntity(Location l);
}
