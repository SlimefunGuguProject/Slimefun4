package me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.Array;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * Legacy inventory compatibility contract used by Slimefun and established addons.
 *
 * <p>Slimefun Legacy intentionally keeps this interface supported so existing machines and addons
 * can continue to load without repeated deprecation warnings.
 */
public interface InventoryBlock {

    /**
     * This method returns an {@link Array} of slots that serve as the input
     * for the {@link Inventory} of this block.
     *
     * @return The input slots for the {@link Inventory} of this block
     */
    int[] getInputSlots();

    /**
     * This method returns an {@link Array} of slots that serve as the output
     * for the {@link Inventory} of this block.
     *
     * @return The output slots for the {@link Inventory} of this block
     */
    int[] getOutputSlots();

    default void createPreset(SlimefunItem item, Consumer<BlockMenuPreset> setup) {
        createPreset(item, item.getItemName(), setup);
    }

    default void createPreset(SlimefunItem item, String title, Consumer<BlockMenuPreset> setup) {
        InventoryBlockCompatibility.logOnce();

        new BlockMenuPreset(item.getId(), title) {

            @Override
            public void init() {
                setup.accept(this);
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
            public boolean canOpen(Block b, Player p) {
                if (p.hasPermission("slimefun.inventory.bypass")) {
                    return true;
                } else {
                    return item.canUse(p, false)
                            && Slimefun.getProtectionManager()
                                    .hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
                }
            }
        };
    }
}

final class InventoryBlockCompatibility {

    private static final AtomicBoolean NOTICE_LOGGED = new AtomicBoolean();

    private InventoryBlockCompatibility() {}

    static void logOnce() {
        if (NOTICE_LOGGED.compareAndSet(false, true)) {
            Slimefun.logger().info("Compatibility mode enabled.");
        }
    }
}
