package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.ColoredMaterial;
import io.github.thebusybiscuit.slimefun4.utils.HeadTexture;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * This abstract class is the super class of all cargo nodes.
 *
 * @author TheBusyBiscuit
 *
 */
abstract class AbstractCargoNode extends SimpleSlimefunItem<BlockPlaceHandler> implements CargoNode {

    protected static final String FREQUENCY = "frequency";

    @ParametersAreNonnullByDefault
    AbstractCargoNode(
            ItemGroup itemGroup,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            @Nullable ItemStack recipeOutput) {
        super(itemGroup, item, recipeType, recipe, recipeOutput);

        new BlockMenuPreset(
                getId(), ChatUtils.removeColorCodes(item.getItemMeta().getDisplayName())) {

            @Override
            public void init() {
                createBorder(this);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                menu.addMenuCloseHandler(p -> markDirty(b.getLocation()));
                updateBlockMenu(menu, b);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.hasPermission("slimefun.cargo.bypass")
                        || Slimefun.getProtectionManager()
                                .hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };
    }

    @Override
    public BlockPlaceHandler getItemHandler() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                // The owner and frequency are required by every node
                var blockData = StorageCacheUtils.getBlock(e.getBlock().getLocation());
                blockData.setData("owner", e.getPlayer().getUniqueId().toString());
                blockData.setData(FREQUENCY, "0");

                onPlace(e);
            }
        };
    }

    @ParametersAreNonnullByDefault
    protected void addChannelSelector(Block b, BlockMenu menu, int slotPrev, int slotCurrent, int slotNext) {
        int channel = getSelectedChannel(b);

        menu.replaceExistingItem(
                slotPrev,
                new CustomItemStack(HeadTexture.CARGO_ARROW_LEFT.getAsItemStack(), "&b上一信道", "", "&e> 单击将信道ID减一"));
        menu.addMenuClickHandler(slotPrev, (p, slot, item, action) -> {
            int newChannel = channel - 1;

            if (newChannel < 0) {
                newChannel = 15;
            }

            StorageCacheUtils.setData(b.getLocation(), FREQUENCY, String.valueOf(newChannel));
            updateBlockMenu(menu, b);
            return false;
        });

        if (channel == 16) {
            menu.replaceExistingItem(
                    slotCurrent,
                    new CustomItemStack(HeadTexture.CHEST_TERMINAL.getAsItemStack(), "&b信道 ID: &3" + (channel + 1)));
            menu.addMenuClickHandler(slotCurrent, ChestMenuUtils.getEmptyClickHandler());
        } else {
            menu.replaceExistingItem(
                    slotCurrent, new CustomItemStack(ColoredMaterial.WOOL.get(channel), "&b信道 ID: &3" + (channel + 1)));
            menu.addMenuClickHandler(slotCurrent, ChestMenuUtils.getEmptyClickHandler());
        }

        menu.replaceExistingItem(
                slotNext,
                new CustomItemStack(HeadTexture.CARGO_ARROW_RIGHT.getAsItemStack(), "&b下一信道", "", "&e> 单击将信道ID加一"));
        menu.addMenuClickHandler(slotNext, (p, slot, item, action) -> {
            int newChannel = channel + 1;

            if (newChannel > 15) {
                newChannel = 0;
            }

            StorageCacheUtils.setData(b.getLocation(), FREQUENCY, String.valueOf(newChannel));
            updateBlockMenu(menu, b);
            return false;
        });
    }

    @Override
    public int getSelectedChannel(@Nonnull Block b) {
        Validate.notNull(b, "Block must not be null");

        String frequency = StorageCacheUtils.getData(b.getLocation(), FREQUENCY);

        if (frequency == null) {
            return 0;
        } else {
            int channel = Integer.parseInt(frequency);
            return NumberUtils.clamp(0, channel, 16);
        }
    }

    abstract void onPlace(@Nonnull BlockPlaceEvent e);

    abstract void createBorder(@Nonnull BlockMenuPreset preset);

    abstract void updateBlockMenu(@Nonnull BlockMenu menu, @Nonnull Block b);

    abstract void markDirty(@Nonnull Location loc);
}
