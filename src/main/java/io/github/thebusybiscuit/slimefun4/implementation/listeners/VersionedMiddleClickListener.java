package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.papermc.paper.event.player.PlayerPickItemEvent;
import java.lang.reflect.Method;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.apache.commons.lang.Validate;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class VersionedMiddleClickListener implements Listener {
    Class<? extends PlayerPickItemEvent> pickBlockEventClass;
    Method getBlockMethod;

    public VersionedMiddleClickListener(@Nonnull Slimefun plugin) {
        try {
            pickBlockEventClass = (Class<? extends PlayerPickItemEvent>)
                    Class.forName("io.papermc.paper.event.player.PlayerPickBlockEvent");
            Validate.isTrue(PlayerPickItemEvent.class.isAssignableFrom(pickBlockEventClass));
            getBlockMethod = pickBlockEventClass.getMethod("getBlock");
            getBlockMethod.setAccessible(true);
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        } catch (Throwable e) {
            Slimefun.logger().log(Level.WARNING, "无法初始化中键监听器版本兼容模块, 部分功能可能无法正常使用", e);
        }
    }

    @EventHandler
    public void onMiddleClick(PlayerPickItemEvent event) {
        Player player = event.getPlayer();
        // there is no api for abilities now, so we just judge by player's gameMode
        if (player.getGameMode() == GameMode.CREATIVE
                && pickBlockEventClass != null
                && pickBlockEventClass.isInstance(event)) {
            try {
                Block block = (Block) getBlockMethod.invoke(event);
                SlimefunItem sfItem = StorageCacheUtils.getSlimefunItem(block.getLocation());
                if (sfItem == null) {
                    return;
                }

                int slotTarget = event.getTargetSlot();
                // check targetSlot for safety, actually it must be in range 0-9
                if (slotTarget >= 0 && slotTarget < 9) {
                    // remove original pickItem logic because it sucks
                    event.setCancelled(true);

                    // try redirect to another hotbar if it contains the slimefunItem
                    for (var i = 0; i < 9; ++i) {
                        ItemStack hotbarItem = player.getInventory().getItem(i);
                        if (hotbarItem != null && !hotbarItem.getType().isAir() && sfItem.isItem(hotbarItem)) {
                            player.getInventory().setHeldItemSlot(i);
                            return;
                        }
                    }

                    // use the event target Slot
                    player.getInventory().setHeldItemSlot(slotTarget);
                    player.getInventory().setItemInMainHand(sfItem.getItem().clone());
                }
            } catch (Throwable e) {
                // Ignored this because it is not worth throwing
            }
        }
    }
}
