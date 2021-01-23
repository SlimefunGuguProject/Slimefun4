package io.github.thebusybiscuit.slimefun4.implementation.items.electric.gadgets;

import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class OverclockModule extends SimpleSlimefunItem<ItemUseHandler> {
    private final String OVERCLOCK_PATH = "overclock-multiply";

    public OverclockModule(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(category, item, recipeType, recipe);
    }

    @Override
    public ItemUseHandler getItemHandler() {
        return e -> {
            Optional<Block> block = e.getClickedBlock();
            e.cancel();

            if (block.isPresent()) {
                Block b = block.get();

                //String json = BlockStorage.getBlockInfoAsJson(b);
                //e.getPlayer().sendMessage(json);

                if (BlockStorage.hasBlockInfo(b)) {
                    Config cfg = BlockStorage.getLocationInfo(b.getLocation());

                    SlimefunItem item = SlimefunItem.getByID(cfg.getString("id"));

                    if (item instanceof AContainer) {
                        AContainer machine = (AContainer) item;
                        int multiply = Integer.parseInt(cfg.getString(OVERCLOCK_PATH));

                        if (multiply >= 10) {
                            e.getPlayer().sendMessage("超频倍率已达上限: 10x");
                        } else {
                            cfg.setValue(OVERCLOCK_PATH, multiply + 1);
                            machine.setProcessingSpeed(machine.getSpeed() * multiply + 1);
                            e.getPlayer().sendMessage("超频机器成功, 目前倍率: " + machine.getSpeed() + "x");
                        }
                    } else {
                        e.getPlayer().sendMessage("抱歉, 该方块无法被超频");
                    }
                } else {
                    e.getPlayer().sendMessage("抱歉, 该方块无法被超频");
                }
            }
        };
    }
}