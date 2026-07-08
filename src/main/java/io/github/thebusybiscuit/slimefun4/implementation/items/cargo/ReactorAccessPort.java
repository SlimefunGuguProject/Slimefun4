package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.reactors.Reactor;
import io.github.thebusybiscuit.slimefun4.implementation.items.misc.CoolantCell;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * The {@link ReactorAccessPort} is a block which acts as an interface
 * between a {@link Reactor} and a {@link CargoNet}.
 * Any item placed into the port will get transferred to the {@link Reactor}.
 *
 * @author TheBusyBiscuit
 * @author AlexLander123
 */
public class ReactorAccessPort extends SlimefunItem {

    private static final int INFO_SLOT = 49;

    private final int[] background = {0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13, 14, 21, 23};
    private final int[] fuelBorder = {9, 10, 11, 18, 20, 27, 29, 36, 38, 45, 46, 47};
    private final int[] inputBorder = {15, 16, 17, 24, 26, 33, 35, 42, 44, 51, 52, 53};
    private final int[] outputBorder = {30, 31, 32, 39, 41, 48, 50};

    @ParametersAreNonnullByDefault
    public ReactorAccessPort(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemHandler(onBreak());

        new BlockMenuPreset(getId(), "&2反应堆交互接口") {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                        || Slimefun.getProtectionManager()
                                .hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                BlockMenu reactor = getReactor(b.getLocation());

                if (reactor != null) {
                    menu.replaceExistingItem(
                            INFO_SLOT,
                            new CustomItemStack(Material.GREEN_WOOL, "&7反应堆", "", "&6已检测到反应堆", "", "&7> 点击打开反应堆界面"));
                    menu.addMenuClickHandler(INFO_SLOT, (p, slot, item, action) -> {
                        if (reactor != null) {
                            reactor.open(p);
                        }

                        newInstance(menu, b);

                        return false;
                    });
                } else {
                    menu.replaceExistingItem(
                            INFO_SLOT,
                            new CustomItemStack(
                                    Material.RED_WOOL,
                                    "&7反应堆",
                                    "",
                                    "&c未检测到反应堆",
                                    "",
                                    "&7反应堆必须放置在",
                                    "&7本交互接口的",
                                    "&73个方块以下的位置"));
                    menu.addMenuClickHandler(INFO_SLOT, (p, slot, item, action) -> {
                        newInstance(menu, b);
                        return false;
                    });
                }
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return getInputSlots();
                } else {
                    return getOutputSlots();
                }
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                if (flow == ItemTransportFlow.INSERT) {
                    if (SlimefunItem.getByItem(item) instanceof CoolantCell) {
                        return getCoolantSlots();
                    } else {
                        return getFuelSlots();
                    }
                } else {
                    return getOutputSlots();
                }
            }
        };
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
            }
        };
    }

    private void constructMenu(@Nonnull BlockMenuPreset preset) {
        preset.drawBackground(ChestMenuUtils.getBackground(), background);

        preset.drawBackground(new CustomItemStack(Material.LIME_STAINED_GLASS_PANE, " "), fuelBorder);
        preset.drawBackground(new CustomItemStack(Material.CYAN_STAINED_GLASS_PANE, " "), inputBorder);
        preset.drawBackground(new CustomItemStack(Material.GREEN_STAINED_GLASS_PANE, " "), outputBorder);

        preset.addItem(
                1,
                new CustomItemStack(SlimefunItems.URANIUM, "&7燃料槽", "", "&r这里可以放入放射性燃料, 例如:", "&2铀 &r或 &a镎"),
                ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(
                22,
                new CustomItemStack(SlimefunItems.PLUTONIUM, "&7副产品槽", "", "&r这里可以获取反应堆在运行中产生的副产物", "&r例如 &a镎 &r或 &7钚"),
                ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(
                7,
                new CustomItemStack(
                        SlimefunItems.REACTOR_COOLANT_CELL, "&b冷却剂", "", "&r这里可以放入冷却剂", "&4如果没有冷却剂, 你的反应堆", "&4将会爆炸"),
                ChestMenuUtils.getEmptyClickHandler());
    }

    @Nonnull
    public int[] getInputSlots() {
        return new int[] {19, 28, 37, 25, 34, 43};
    }

    @Nonnull
    public int[] getFuelSlots() {
        return new int[] {19, 28, 37};
    }

    @Nonnull
    public int[] getCoolantSlots() {
        return new int[] {25, 34, 43};
    }

    @Nonnull
    public static int[] getOutputSlots() {
        return new int[] {40};
    }

    @Nullable private BlockMenu getReactor(@Nonnull Location l) {
        Location location = new Location(l.getWorld(), l.getX(), l.getY() - 3, l.getZ());
        SlimefunItem item = StorageCacheUtils.getSlimefunItem(location);

        if (item instanceof Reactor) {
            return StorageCacheUtils.getMenu(location);
        }

        return null;
    }
}
