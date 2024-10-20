package io.github.thebusybiscuit.slimefun4.implementation.items.cargo;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.HologramOwner;
import io.github.thebusybiscuit.slimefun4.core.attributes.rotations.NotRotatable;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.cargo.CargoNet;
import io.github.thebusybiscuit.slimefun4.implementation.handlers.SimpleBlockBreakHandler;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CargoManager extends SlimefunItem implements HologramOwner, NotRotatable {

    @ParametersAreNonnullByDefault
    public CargoManager(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        addItemHandler(onBreak());
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new SimpleBlockBreakHandler() {

            @Override
            public void onBlockBreak(@Nonnull Block b) {
                removeHologram(b);
            }
        };
    }

    @Override
    public void preRegister() {
        addItemHandler(
                new BlockTicker() {

                    @Override
                    public void tick(Block b, SlimefunItem item, SlimefunBlockData data) {
                        CargoNet.getNetworkFromLocationOrCreate(b.getLocation()).tick(b, data);
                    }

                    @Override
                    public boolean isSynchronized() {
                        return false;
                    }
                },
                new BlockUseHandler() {

                    @Override
                    public void onRightClick(PlayerRightClickEvent e) {
                        Optional<Block> block = e.getClickedBlock();

                        if (block.isPresent()) {
                            Player p = e.getPlayer();
                            Block b = block.get();

                            var blockData = StorageCacheUtils.getBlock(b.getLocation());
                            if (blockData.getData("visualizer") == null) {
                                blockData.setData("visualizer", "disabled");
                                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c货运网络可视化: " + "&4\u2718"));
                            } else {
                                blockData.removeData("visualizer");
                                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c货运网络可视化: " + "&2\u2714"));
                            }
                        }
                    }
                });
    }
}
