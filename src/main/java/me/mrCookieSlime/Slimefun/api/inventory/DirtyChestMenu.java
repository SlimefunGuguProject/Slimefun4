package me.mrCookieSlime.Slimefun.api.inventory;

import city.norain.slimefun4.utils.InventoryUtil;
import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.ComparisonResult;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.ConsumeContext;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.InventoryContext;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.MatchContext;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
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
        var virtualItems = Slimefun.getVirtualItemService();
        var isSfItem = SlimefunItem.getByItem(item) != null || virtualItems.isVirtualItem(item);

        if (slots.length == 0) {
            return virtualItems.fits(toInventory(), item, InventoryContext.MENU_FIT);
        }

        var wrapper = ItemStackWrapper.wrap(item);
        var remain = item.getAmount();

        for (int slot : slots) {
            // A small optimization for empty slots
            var slotItem = getItemInSlot(slot);
            if (slotItem == null || slotItem.getType().isAir()) {
                if (!virtualItems.canInsertIntoEmptySlot(item, InventoryContext.MENU_FIT)) {
                    continue;
                }

                int maxStackSize = Math.min(
                        virtualItems.getMaxStackSize(item, InventoryContext.MENU_FIT, item.getMaxStackSize()),
                        toInventory().getMaxStackSize());
                remain -= maxStackSize;

                if (remain <= 0) {
                    return true;
                }

                continue;
            }

            if (isSfItem) {
                ComparisonResult comparison = virtualItems.matches(slotItem, item, MatchContext.STACK_MERGE);
                if (comparison == ComparisonResult.NO_MATCH) {
                    continue;
                }

                if (comparison == ComparisonResult.NOT_HANDLED) {
                    if (!slotItem.hasItemMeta()) {
                        continue;
                    }
                    if (!SlimefunUtils.isItemSimilarWithoutVirtualItems(slotItem, wrapper, true, false)) {
                        continue;
                    }
                }

                int maxStackSize = Math.min(
                        virtualItems.getMaxStackSize(slotItem, InventoryContext.MENU_FIT, slotItem.getMaxStackSize()),
                        toInventory().getMaxStackSize());
                var slotRemain = Math.max(0, maxStackSize - slotItem.getAmount());

                remain -= slotRemain;

                if (remain <= 0) {
                    return true;
                }
            }
        }

        boolean result = false;

        if (!isSfItem) {
            result = virtualItems.fits(toInventory(), item, InventoryContext.MENU_FIT, slots);
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
        var virtualItems = Slimefun.getVirtualItemService();

        for (int slot : slots) {
            if (amount <= 0) {
                break;
            }

            ItemStack stack = getItemInSlot(slot);

            if (stack == null || stack.getType().isAir()) {
                if (!virtualItems.canInsertIntoEmptySlot(item, InventoryContext.MENU_INSERT)) {
                    continue;
                }

                int maxStackSize = Math.min(
                        virtualItems.getMaxStackSize(item, InventoryContext.MENU_INSERT, item.getMaxStackSize()),
                        toInventory().getMaxStackSize());
                int movedAmount = Math.min(amount, maxStackSize);

                ItemStack inserted = item.clone();
                inserted.setAmount(movedAmount);
                replaceExistingItem(slot, inserted);
                amount -= movedAmount;
            } else {
                int maxStackSize = Math.min(
                        virtualItems.getMaxStackSize(stack, InventoryContext.MENU_INSERT, stack.getMaxStackSize()),
                        toInventory().getMaxStackSize());
                if (stack.getAmount() < maxStackSize) {
                    if (wrapper == null) {
                        wrapper = ItemStackWrapper.wrap(item);
                    }

                    ComparisonResult comparison = virtualItems.matches(stack, item, MatchContext.STACK_MERGE);
                    if (comparison == ComparisonResult.NO_MATCH) {
                        continue;
                    }

                    if (comparison == ComparisonResult.NOT_HANDLED && SlimefunItem.getByItem(item) != null) {
                        if (!SlimefunUtils.isItemSimilarWithoutVirtualItems(stack, wrapper, true, false)) {
                            continue;
                        }
                    } else if (comparison == ComparisonResult.NOT_HANDLED) {
                        if (!ItemUtils.canStack(wrapper, stack)) {
                            continue;
                        }
                    }

                    int movedAmount = Math.min(amount, maxStackSize - stack.getAmount());
                    amount -= movedAmount;
                    stack.setAmount(stack.getAmount() + movedAmount);
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

        ItemStack item = getItemInSlot(slot);
        var virtualItems = Slimefun.getVirtualItemService();
        var result = virtualItems.consume(item, amount, replaceConsumables, ConsumeContext.MENU_CONSUME);
        if (result.handled()) {
            replaceExistingItem(slot, result.item());
        } else {
            ItemUtils.consumeItem(item, amount, replaceConsumables);
        }

        markDirty();
    }

    @Override
    public void replaceExistingItem(int slot, ItemStack item) {
        replaceExistingItem(slot, item, true);
    }

    public void replaceExistingItem(int slot, ItemStack item, boolean event) {
        if (event) {
            ItemStack previous = getItemInSlot(slot);
            item = preset.onItemStackChange(this, slot, previous, item);
        }

        super.replaceExistingItem(slot, item);
        markDirty();
    }
}
