package io.github.thebusybiscuit.slimefun4.implementation.items.elevator;

import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.papermc.lib.PaperLib;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * The {@link ElevatorPlate} is a quick way of teleportation.
 * You can place multiple {@link ElevatorPlate ElevatorPlates} along the y axis
 * to teleport between them.
 *
 * @author TheBusyBiscuit
 * @author Walshy
 */
public class ElevatorPlate extends SimpleSlimefunItem<BlockUseHandler> {

    /**
     * This is our key for storing the floor name.
     */
    private static final String DATA_KEY = "floor";

    /**
     * This is the size of our {@link Inventory}.
     */
    private static final int GUI_SIZE = 27;

    /**
     * This is our {@link Set} of currently teleporting {@link Player Players}.
     * It is used to prevent them from triggering the {@link ElevatorPlate} they land on.
     */
    private final Set<UUID> users = new HashSet<>();

    @ParametersAreNonnullByDefault
    public ElevatorPlate(
            ItemGroup itemGroup,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);

        addItemHandler(onPlace());
    }

    private @Nonnull BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                var blockData = StorageCacheUtils.getBlock(e.getBlock().getLocation());
                blockData.setData(DATA_KEY, ChatColor.WHITE + "一楼");
                blockData.setData("owner", e.getPlayer().getUniqueId().toString());
            }
        };
    }

    @Override
    public @Nonnull BlockUseHandler getItemHandler() {
        return e -> {
            Block b = e.getClickedBlock().get();

            if (e.getPlayer().getUniqueId().toString().equals(StorageCacheUtils.getData(b.getLocation(), "owner"))) {
                openEditor(e.getPlayer(), b);
            }
        };
    }

    public void getFloors(@Nonnull Block b, @Nonnull Consumer<List<ElevatorFloor>> action) {
        var blockDataList = new ArrayList<SlimefunBlockData>();
        var shouldLoad = false;

        for (int y = b.getWorld().getMinHeight(); y < b.getWorld().getMaxHeight(); y++) {
            var block = b.getWorld().getBlockAt(b.getX(), y, b.getZ());
            var loc = block.getLocation();

            if (block.getType() == getItem().getType() && StorageCacheUtils.isBlock(loc, getId())) {
                var blockData = StorageCacheUtils.getBlock(loc);
                if (blockData.isPendingRemove()) {
                    continue;
                }

                if (!blockData.isDataLoaded() && !shouldLoad) {
                    shouldLoad = true;
                }

                blockDataList.add(blockData);
            }
        }

        if (shouldLoad) {
            Slimefun.getDatabaseManager()
                    .getBlockDataController()
                    .loadBlockDataAsync(blockDataList, new IAsyncReadCallback<>() {
                        @Override
                        public boolean runOnMainThread() {
                            return true;
                        }

                        @Override
                        public void onResult(List<SlimefunBlockData> result) {
                            action.accept(toFloors(blockDataList));
                        }
                    });
        } else {
            action.accept(toFloors(blockDataList));
        }
    }

    private List<ElevatorFloor> toFloors(List<SlimefunBlockData> blockDataList) {
        var floors = new LinkedList<ElevatorFloor>();
        for (var i = 0; i < blockDataList.size(); i++) {
            var blockData = blockDataList.get(i);
            floors.addFirst(new ElevatorFloor(
                    ChatColors.color(blockData.getData(DATA_KEY)),
                    i,
                    blockData.getLocation().getBlock()));
        }
        return floors;
    }

    @ParametersAreNonnullByDefault
    public void openInterface(Player p, Block b) {
        if (users.remove(p.getUniqueId())) {
            return;
        }

        getFloors(b, floors -> {
            if (floors.size() < 2) {
                Slimefun.getLocalization().sendMessage(p, "machines.ELEVATOR.no-destinations", true);
            } else {
                openFloorSelector(b, floors, p, 1);
            }
        });
    }

    @ParametersAreNonnullByDefault
    private void openFloorSelector(Block b, List<ElevatorFloor> floors, Player p, int page) {
        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(p, "machines.ELEVATOR.pick-a-floor"));
        menu.setEmptySlotsClickable(false);

        int index = GUI_SIZE * (page - 1);

        for (int i = 0; i < Math.min(GUI_SIZE, floors.size() - index); i++) {
            ElevatorFloor floor = floors.get(index + i);

            // @formatter:off
            if (floor.getAltitude() == b.getY()) {
                menu.addItem(
                        i,
                        new CustomItemStack(
                                Material.COMPASS,
                                ChatColor.GRAY.toString()
                                        + floor.getNumber()
                                        + ". "
                                        + ChatColor.BLACK
                                        + floor.getName(),
                                Slimefun.getLocalization().getMessage(p, "machines.ELEVATOR.current-floor")
                                        + ' '
                                        + ChatColor.WHITE
                                        + floor.getName()),
                        ChestMenuUtils.getEmptyClickHandler());
            } else {
                menu.addItem(
                        i,
                        new CustomItemStack(
                                Material.PAPER,
                                ChatColor.GRAY.toString()
                                        + floor.getNumber()
                                        + ". "
                                        + ChatColor.BLACK
                                        + floor.getName(),
                                Slimefun.getLocalization().getMessage(p, "machines.ELEVATOR.click-to-teleport")
                                        + ' '
                                        + ChatColor.WHITE
                                        + floor.getName()),
                        (player, slot, itemStack, clickAction) -> {
                            teleport(player, floor);
                            return false;
                        });
            }
            // @formatter:on
        }

        int pages = 1 + (floors.size() / GUI_SIZE);

        // 0 index so size is the first slot of the last row.
        for (int i = GUI_SIZE; i < GUI_SIZE + 9; i++) {
            if (i == GUI_SIZE + 2 && pages > 1 && page != 1) {
                menu.addItem(
                        i, ChestMenuUtils.getPreviousButton(p, page, pages), (player, i1, itemStack, clickAction) -> {
                            openFloorSelector(b, floors, p, page - 1);
                            return false;
                        });
            } else if (i == GUI_SIZE + 6 && pages > 1 && page != pages) {
                menu.addItem(i, ChestMenuUtils.getNextButton(p, page, pages), (player, i1, itemStack, clickAction) -> {
                    openFloorSelector(b, floors, p, page + 1);
                    return false;
                });
            } else {
                menu.addItem(i, ChestMenuUtils.getBackground(), (player, i1, itemStack, clickAction) -> false);
            }
        }

        menu.open(p);
    }

    @ParametersAreNonnullByDefault
    private void teleport(Player player, ElevatorFloor floor) {
        Slimefun.runSync(
                () -> {
                    users.add(player.getUniqueId());

                    float yaw = player.getEyeLocation().getYaw() + 180;

                    if (yaw > 180) {
                        yaw = -180 + (yaw - 180);
                    }

                    Location loc = floor.getLocation();
                    Location destination = new Location(
                            player.getWorld(),
                            loc.getX() + 0.5,
                            loc.getY() + 0.4,
                            loc.getZ() + 0.5,
                            yaw,
                            player.getEyeLocation().getPitch());

                    PaperLib.teleportAsync(player, destination).thenAccept(teleported -> {
                        if (teleported.booleanValue()) {
                            player.sendTitle(ChatColor.WHITE + ChatColors.color(floor.getName()), null, 20, 60, 20);
                        }
                    });
                },
                player);
    }

    @ParametersAreNonnullByDefault
    public void openEditor(Player p, Block b) {
        ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(p, "machines.ELEVATOR.editor-title"));

        menu.addItem(
                4,
                new CustomItemStack(
                        Material.NAME_TAG,
                        "&7楼层名 &e(单击编辑)",
                        "",
                        ChatColor.WHITE + ChatColors.color(StorageCacheUtils.getData(b.getLocation(), DATA_KEY))));
        menu.addMenuClickHandler(4, (pl, slot, item, action) -> {
            pl.closeInventory();
            pl.sendMessage("");
            Slimefun.getLocalization().sendMessage(p, "machines.ELEVATOR.enter-name");
            pl.sendMessage("");

            ChatUtils.awaitInput(pl, message -> {
                StorageCacheUtils.setData(b.getLocation(), DATA_KEY, message.replace(ChatColor.COLOR_CHAR, '&'));

                pl.sendMessage("");
                Slimefun.getLocalization()
                        .sendMessage(p, "machines.ELEVATOR.named", msg -> msg.replace("%floor%", message));
                pl.sendMessage("");

                openEditor(pl, b);
            });

            return false;
        });

        menu.open(p);
    }
}
