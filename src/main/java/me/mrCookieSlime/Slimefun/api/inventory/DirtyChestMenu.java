package me.mrCookieSlime.Slimefun.api.inventory;

import city.norain.slimefun4.utils.InventoryUtil;
import io.github.bakedlibs.dough.inventory.InvUtils;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

// This class will be deprecated, relocated and rewritten in a future version.
public class DirtyChestMenu extends ChestMenu {

    protected final BlockMenuPreset preset;
    protected int changes = 1;

    public DirtyChestMenu(@Nonnull BlockMenuPreset preset) {
        super(preset.getTitle());

        this.preset = preset;
    }

    /**
     * This method checks whether this {@link DirtyChestMenu} is currently viewed by a {@link Player}.
     *
     * @return Whether anyone is currently viewing this {@link Inventory}
     */
    public boolean hasViewer() {
        Inventory inv = toInventory();
        return inv != null && !inv.getViewers().isEmpty();
    }

    public void markDirty() {
        changes++;
    }

    public boolean isDirty() {
        return changes > 0;
    }

    public int getUnsavedChanges() {
        return changes;
    }

    @Nonnull
    public BlockMenuPreset getPreset() {
        return preset;
    }

    public boolean canOpen(Block b, Player p) {
        return preset.canOpen(b, p);
    }

    @Override
    public void open(Player... players) {
        if (locked()) {
            return;
        }

        super.open(players);

        // The Inventory will likely be modified soon
        markDirty();
    }

    public void close() {
        InventoryUtil.closeInventory(toInventory());
    }

    public boolean fits(@Nonnull ItemStack item, int... slots) {
        var isSfItem = SlimefunItem.getByItem(item) != null;
        var wrapper = ItemStackWrapper.wrap(item);
        var remain = item.getAmount();

        for (int slot : slots) {
            // A small optimization for empty slots
            var slotItem = getItemInSlot(slot);
            if (slotItem == null || slotItem.getType().isAir()) {
                return true;
            }

            if (isSfItem) {
                if (!slotItem.hasItemMeta()
                        || item.getType() != slotItem.getType()
                        || !SlimefunUtils.isItemSimilar(slotItem, wrapper, true, false)) {
                    continue;
                }

                var slotRemain = slotItem.getMaxStackSize() - slotItem.getAmount();

                remain -= slotRemain;

                if (remain <= 0) {
                    return true;
                }
            }
        }

        boolean result = false;

        if (!isSfItem) {
            result = InvUtils.fits(toInventory(), wrapper, slots);
        }

        return result;
    }

    /**
     * Adds given {@link ItemStack} to any of the given inventory slots.
     * Items will be added to the inventory slots based on their order in the function argument.
     * Items will be added either to any empty inventory slots or any partially filled slots, in which case
     * as many items as can fit will be added to that specific spot.
     *
     * @param item  {@link ItemStack} to be added to the inventory
     * @param slots Numbers of slots to add the {@link ItemStack} to
     * @return {@link ItemStack} with any items that did not fit into the inventory
     * or null when everything had fit
     */
    @Nullable public ItemStack pushItem(ItemStack item, int... slots) {
        if (item == null || item.getType() == Material.AIR) {
            throw new IllegalArgumentException("Cannot push null or AIR");
        }

        if (locked()) {
            throw new IllegalStateException("Cannot push item when menu is locked");
        }

        ItemStackWrapper wrapper = null;
        int amount = item.getAmount();

        for (int slot : slots) {
            if (amount <= 0) {
                break;
            }

            ItemStack stack = getItemInSlot(slot);

            if (stack == null) {
                replaceExistingItem(slot, item);
                return null;
            } else {
                int maxStackSize =
                        Math.min(stack.getMaxStackSize(), toInventory().getMaxStackSize());
                if (stack.getAmount() < maxStackSize) {
                    if (wrapper == null) {
                        wrapper = ItemStackWrapper.wrap(item);
                    }

                    if (SlimefunItem.getByItem(item) != null) {
                        // Patch: use sf item check
                        if (!SlimefunUtils.isItemSimilar(stack, wrapper, true, false)) {
                            continue;
                        }
                    } else {
                        // Use original check
                        if (!ItemUtils.canStack(wrapper, stack)) {
                            continue;
                        }
                    }

                    amount -= (maxStackSize - stack.getAmount());
                    stack.setAmount(Math.min(stack.getAmount() + item.getAmount(), maxStackSize));
                    item.setAmount(amount);
                }
            }
        }

        if (amount > 0) {
            return new CustomItemStack(item, amount);
        } else {
            return null;
        }
    }

    public void consumeItem(int slot) {
        consumeItem(slot, 1);
    }

    public void consumeItem(int slot, int amount) {
        if (locked()) {
            throw new IllegalStateException("Cannot consume item when menu is locked");
        }

        consumeItem(slot, amount, false);
    }

    public void consumeItem(int slot, int amount, boolean replaceConsumables) {
        if (locked()) {
            throw new IllegalStateException("Cannot consume item when menu is locked");
        }

        ItemUtils.consumeItem(getItemInSlot(slot), amount, replaceConsumables);
        markDirty();
    }

    @Override
    public void replaceExistingItem(int slot, ItemStack item) {
        if (locked()) {
            throw new IllegalStateException("Cannot consume item when menu is locked");
        }

        replaceExistingItem(slot, item, true);
    }

    public void replaceExistingItem(int slot, ItemStack item, boolean event) {
        if (locked()) {
            throw new IllegalStateException("Cannot consume item when menu is locked");
        }

        if (event) {
            ItemStack previous = getItemInSlot(slot);
            item = preset.onItemStackChange(this, slot, previous, item);
        }

        super.replaceExistingItem(slot, item);
        markDirty();
    }
}
