package io.github.thebusybiscuit.slimefun4.implementation.items.blocks;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.services.holograms.HologramsService;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.utils.ArmorStandUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The {@link HologramProjector} is a very simple block which allows the {@link Player}
 * to create a floating text that is completely configurable.
 *
 * @author TheBusyBiscuit
 * @author Kry-Vosa
 * @author SoSeDiK
 *
 * @see HologramOwner
 * @see HologramsService
 *
 */
public class HologramProjector extends SlimefunItem implements HologramOwner {

    private static final String OFFSET_PARAMETER = "offset";

    @ParametersAreNonnullByDefault
    public HologramProjector(
            ItemGroup itemGroup,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);

        addItemHandler(onPlace(), onRightClick(), onBreak());
    }

    private @Nonnull BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                Block b = e.getBlockPlaced();
                var blockData = StorageCacheUtils.getBlock(b.getLocation());
                blockData.setData("text", "使用投影仪编辑文本");
                blockData.setData(OFFSET_PARAMETER, "0.5");
                blockData.setData("owner", e.getPlayer().getUniqueId().toString());

                getArmorStand(b, true);
            }
        };
    }

    private @Nonnull BlockBreakHandler onBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                killArmorStand(b);
            }
        };
    }

    public @Nonnull BlockUseHandler onRightClick() {
        return e -> {
            e.cancel();

            var p = e.getPlayer();
            var b = e.getClickedBlock().get();
            var data = StorageCacheUtils.getBlock(b.getLocation());

            if (data != null && !data.isDataLoaded()) {
                StorageCacheUtils.requestLoad(data);
                return;
            }

            if (p.getUniqueId().toString().equals(StorageCacheUtils.getData(b.getLocation(), "owner"))) {
                openEditor(p, b);
            }
        };
    }

    private void openEditor(@Nonnull Player p, @Nonnull Block projector) {
        ChestMenu menu =
                new ChestMenu(Slimefun.getLocalization().getMessage(p, "machines.HOLOGRAM_PROJECTOR.inventory-title"));

        menu.addItem(
                0,
                new CustomItemStack(
                        Material.NAME_TAG,
                        "&7展示文本 &e(点击编辑)",
                        "",
                        "&f" + ChatColors.color(StorageCacheUtils.getData(projector.getLocation(), "text"))));
        menu.addMenuClickHandler(0, (pl, slot, item, action) -> {
            pl.closeInventory();
            Slimefun.getLocalization().sendMessage(pl, "machines.HOLOGRAM_PROJECTOR.enter-text", true);

            ChatUtils.awaitInput(pl, message -> {
                // Fixes #3445 - Make sure the projector is not broken
                if (!StorageCacheUtils.isBlock(projector.getLocation(), getId())) {
                    // Hologram projector no longer exists.
                    // TODO: Add a chat message informing the player that their message was ignored.
                    return;
                }

                ArmorStand hologram = getArmorStand(projector, true);
                hologram.setCustomName(ChatColors.color(message));
                StorageCacheUtils.setData(projector.getLocation(), "text", hologram.getCustomName());
                openEditor(pl, projector);
            });

            return false;
        });

        menu.addItem(
                1,
                new CustomItemStack(
                        Material.CLOCK,
                        "&7高度: &e"
                                + NumberUtils.reparseDouble(Double.parseDouble(
                                                StorageCacheUtils.getData(projector.getLocation(), OFFSET_PARAMETER))
                                        + 1.0D),
                        "",
                        "&f左键单击: &7+0.1",
                        "&f右键单击: &7-0.1"));
        menu.addMenuClickHandler(1, (pl, slot, item, action) -> {
            var blockData = StorageCacheUtils.getBlock(projector.getLocation());
            double offset = NumberUtils.reparseDouble(
                    Double.parseDouble(blockData.getData(OFFSET_PARAMETER)) + (action.isRightClicked() ? -0.1F : 0.1F));
            ArmorStand hologram = getArmorStand(projector, true);
            Location l = new Location(
                    projector.getWorld(), projector.getX() + 0.5, projector.getY() + offset, projector.getZ() + 0.5);
            hologram.teleportAsync(l);

            blockData.setData(OFFSET_PARAMETER, String.valueOf(offset));
            openEditor(pl, projector);
            return false;
        });

        menu.open(p);
    }

    private static ArmorStand getArmorStand(@Nonnull Block projector, boolean createIfNoneExists) {
        var blockData = StorageCacheUtils.getBlock(projector.getLocation());
        String nametag = blockData.getData("text");
        double offset = Double.parseDouble(blockData.getData(OFFSET_PARAMETER));
        Location l = new Location(
                projector.getWorld(), projector.getX() + 0.5, projector.getY() + offset, projector.getZ() + 0.5);

        for (Entity n : l.getChunk().getEntities()) {
            if (n instanceof ArmorStand armorStand && l.distanceSquared(n.getLocation()) < 0.4) {
                String customName = n.getCustomName();

                if (customName != null && customName.equals(nametag)) {
                    return armorStand;
                }
            }
        }

        if (!createIfNoneExists) {
            return null;
        }

        return ArmorStandUtils.spawnArmorStand(l, nametag);
    }

    private static void killArmorStand(@Nonnull Block b) {
        ArmorStand hologram = getArmorStand(b, false);

        if (hologram != null) {
            hologram.remove();
        }
    }
}
