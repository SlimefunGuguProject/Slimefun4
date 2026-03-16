package io.github.thebusybiscuit.slimefun4.core.services;

import io.github.bakedlibs.dough.inventory.InvUtils;
import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.AdmissionResult;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.ComparisonResult;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.ConsumeContext;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.InventoryContext;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.ItemResult;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.MatchContext;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.RemainderContext;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Central access point for a single virtual item hook.
 *
 * <p>This service is intentionally lightweight. If no handler is registered, or if the active
 * handler reports that a stack is not virtual, all helper methods fall back to Slimefun's original
 * behavior.
 */
public final class VirtualItemService {

    private volatile @Nullable VirtualItemHandler handler;

    public void setHandler(@Nullable VirtualItemHandler handler) {
        this.handler = handler;
    }

    public @Nullable VirtualItemHandler getHandler() {
        return handler;
    }

    public boolean hasHandler() {
        return handler != null;
    }

    public boolean isVirtualItem(@Nullable ItemStack item) {
        VirtualItemHandler currentHandler = handler;
        return currentHandler != null && currentHandler.isVirtualItem(item);
    }

    public @Nonnull ComparisonResult matches(
            @Nullable ItemStack left, @Nullable ItemStack right, @Nonnull MatchContext context) {
        VirtualItemHandler currentHandler = handler;
        if (currentHandler == null || !isHookRelevant(currentHandler, left, right)) {
            return ComparisonResult.NOT_HANDLED;
        }

        return currentHandler.matches(left, right, context);
    }

    public boolean isSimilar(
            @Nullable ItemStack left,
            @Nullable ItemStack right,
            @Nonnull MatchContext context,
            boolean checkLore,
            boolean checkAmount) {
        ComparisonResult comparison = matches(left, right, context);
        if (comparison == ComparisonResult.MATCH) {
            return true;
        }

        if (comparison == ComparisonResult.NO_MATCH) {
            return false;
        }

        return SlimefunUtils.isItemSimilarWithoutVirtualItems(left, right, checkLore, checkAmount);
    }

    public boolean matchesPredicate(
            @Nonnull ItemStack item, @Nonnull Predicate<ItemStack> predicate, @Nonnull MatchContext context) {
        if (predicate.test(item)) {
            return true;
        }

        VirtualItemHandler currentHandler = handler;
        if (currentHandler == null || !currentHandler.isVirtualItem(item)) {
            return false;
        }

        ComparisonResult comparison = currentHandler.matchesPredicate(item, predicate, context);
        return comparison == ComparisonResult.MATCH;
    }

    public int getMaxStackSize(@Nonnull ItemStack item, @Nonnull InventoryContext context, int defaultMaxStackSize) {
        Validate.notNull(item, "The item cannot be null");

        VirtualItemHandler currentHandler = handler;
        if (currentHandler == null || !currentHandler.isVirtualItem(item)) {
            return defaultMaxStackSize;
        }

        return Math.max(1, currentHandler.getMaxStackSize(item, context, defaultMaxStackSize));
    }

    public @Nonnull AdmissionResult allows(@Nonnull ItemStack item, @Nonnull InventoryContext context) {
        VirtualItemHandler currentHandler = handler;
        if (currentHandler == null || !currentHandler.isVirtualItem(item)) {
            return AdmissionResult.NOT_HANDLED;
        }

        return currentHandler.allows(item, context);
    }

    public boolean canInsertIntoEmptySlot(@Nonnull ItemStack item, @Nonnull InventoryContext context) {
        return allows(item, context) != AdmissionResult.DENY;
    }

    public @Nonnull ItemResult consume(
            @Nullable ItemStack item, int amount, boolean replaceConsumables, @Nonnull ConsumeContext context) {
        VirtualItemHandler currentHandler = handler;
        if (item == null || currentHandler == null || !currentHandler.isVirtualItem(item)) {
            return ItemResult.notHandled();
        }

        return currentHandler.consume(item, amount, replaceConsumables, context);
    }

    public @Nonnull ItemResult getRemainder(@Nullable ItemStack item, @Nonnull RemainderContext context) {
        VirtualItemHandler currentHandler = handler;
        if (item == null || currentHandler == null || !currentHandler.isVirtualItem(item)) {
            return ItemResult.notHandled();
        }

        return currentHandler.getRemainder(item, context);
    }

    public boolean fits(
            @Nonnull Inventory inventory, @Nonnull ItemStack item, @Nonnull InventoryContext context, int... slots) {
        if (!isVirtualItem(item) && SlimefunItem.getByItem(item) == null) {
            if (slots.length == 0) {
                return InvUtils.fits(inventory, item);
            }

            return InvUtils.fits(inventory, ItemStackWrapper.wrap(item), slots);
        }

        ItemStack[] contents = Arrays.stream(inventory.getContents())
                .map(stack -> stack == null ? null : stack.clone())
                .toArray(ItemStack[]::new);
        return insertIntoSnapshot(contents, inventory.getMaxStackSize(), item, context, slots) <= 0;
    }

    public boolean fitAll(
            @Nonnull Inventory inventory, @Nonnull ItemStack[] items, @Nonnull InventoryContext context, int... slots) {
        boolean hasVirtualItems = false;
        for (ItemStack item : items) {
            if (item != null && isVirtualItem(item)) {
                hasVirtualItems = true;
                break;
            }
        }

        if (!hasVirtualItems) {
            return InvUtils.fitAll(inventory, items, slots);
        }

        ItemStack[] contents = Arrays.stream(inventory.getContents())
                .map(stack -> stack == null ? null : stack.clone())
                .toArray(ItemStack[]::new);

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            if (insertIntoSnapshot(contents, inventory.getMaxStackSize(), item, context, slots) > 0) {
                return false;
            }
        }

        return true;
    }

    public @Nullable ItemStack addItem(
            @Nonnull Inventory inventory, @Nonnull ItemStack item, @Nonnull InventoryContext context, int... slots) {
        Validate.notNull(item, "The item cannot be null");

        if (!isVirtualItem(item) && slots.length == 0) {
            Map<Integer, ItemStack> leftovers = inventory.addItem(item.clone());
            if (leftovers.isEmpty()) {
                return null;
            }

            return leftovers.values().iterator().next();
        }

        if (!isVirtualItem(item) && slots.length > 0) {
            return addItemDirectly(inventory, item, context, slots);
        }

        ItemStack[] contents = Arrays.stream(inventory.getContents())
                .map(stack -> stack == null ? null : stack.clone())
                .toArray(ItemStack[]::new);
        int amountLeft = insertIntoSnapshot(contents, inventory.getMaxStackSize(), item, context, slots);
        inventory.setContents(contents);

        if (amountLeft <= 0) {
            return null;
        }

        ItemStack remainder = item.clone();
        remainder.setAmount(amountLeft);
        return remainder;
    }

    private @Nullable ItemStack addItemDirectly(
            @Nonnull Inventory inventory, @Nonnull ItemStack item, @Nonnull InventoryContext context, int... slots) {
        int amountLeft = item.getAmount();
        int inventoryMaxStackSize = inventory.getMaxStackSize();

        for (int slot : slots) {
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }

            ItemStack itemInSlot = inventory.getItem(slot);

            if (itemInSlot == null || itemInSlot.getType().isAir()) {
                int maxStackSize = Math.min(item.getMaxStackSize(), inventoryMaxStackSize);
                int movedAmount = Math.min(amountLeft, maxStackSize);

                ItemStack inserted = item.clone();
                inserted.setAmount(movedAmount);
                inventory.setItem(slot, inserted);
                amountLeft -= movedAmount;
            } else if (itemInSlot.isSimilar(item)) {
                int maxStackSize = Math.min(itemInSlot.getMaxStackSize(), inventoryMaxStackSize);
                int freeSpace = Math.max(0, maxStackSize - itemInSlot.getAmount());

                if (freeSpace <= 0) {
                    continue;
                }

                int movedAmount = Math.min(amountLeft, freeSpace);
                itemInSlot.setAmount(itemInSlot.getAmount() + movedAmount);
                amountLeft -= movedAmount;
            }

            if (amountLeft <= 0) {
                return null;
            }
        }

        if (amountLeft <= 0) {
            return null;
        }

        ItemStack remainder = item.clone();
        remainder.setAmount(amountLeft);
        return remainder;
    }

    private int insertIntoSnapshot(
            @Nonnull ItemStack[] contents,
            int inventoryMaxStackSize,
            @Nonnull ItemStack item,
            @Nonnull InventoryContext context,
            int... slots) {
        int amountLeft = item.getAmount();
        int[] resolvedSlots = resolveSlots(contents.length, slots);

        for (int slot : resolvedSlots) {
            ItemStack itemInSlot = contents[slot];

            if (itemInSlot == null || itemInSlot.getType().isAir()) {
                if (!canInsertIntoEmptySlot(item, context)) {
                    continue;
                }

                int maxStackSize =
                        Math.min(getMaxStackSize(item, context, item.getMaxStackSize()), inventoryMaxStackSize);
                int movedAmount = Math.min(amountLeft, maxStackSize);

                ItemStack inserted = item.clone();
                inserted.setAmount(movedAmount);
                contents[slot] = inserted;
                amountLeft -= movedAmount;
            } else if (canMerge(itemInSlot, item)) {
                int maxStackSize = Math.min(
                        getMaxStackSize(itemInSlot, context, itemInSlot.getMaxStackSize()), inventoryMaxStackSize);
                int freeSpace = maxStackSize - itemInSlot.getAmount();

                if (freeSpace <= 0) {
                    continue;
                }

                int movedAmount = Math.min(amountLeft, freeSpace);
                itemInSlot.setAmount(itemInSlot.getAmount() + movedAmount);
                amountLeft -= movedAmount;
            }

            if (amountLeft <= 0) {
                return 0;
            }
        }

        return amountLeft;
    }

    private boolean canMerge(@Nonnull ItemStack existing, @Nonnull ItemStack incoming) {
        ComparisonResult comparison = matches(existing, incoming, MatchContext.STACK_MERGE);
        if (comparison == ComparisonResult.MATCH) {
            return true;
        }

        if (comparison == ComparisonResult.NO_MATCH) {
            return false;
        }

        ItemStackWrapper wrapper = ItemStackWrapper.wrap(incoming);
        if (SlimefunItem.getByItem(existing) != null || SlimefunItem.getByItem(incoming) != null) {
            return SlimefunUtils.isItemSimilarWithoutVirtualItems(existing, wrapper, true, false);
        }

        return ItemUtils.canStack(wrapper, existing);
    }

    private boolean isHookRelevant(
            @Nonnull VirtualItemHandler currentHandler, @Nullable ItemStack left, @Nullable ItemStack right) {
        return currentHandler.isVirtualItem(left) || currentHandler.isVirtualItem(right);
    }

    private int[] resolveSlots(int inventorySize, int... slots) {
        if (slots.length > 0) {
            return Arrays.stream(slots)
                    .filter(slot -> slot >= 0 && slot < inventorySize)
                    .toArray();
        }

        int[] resolvedSlots = new int[inventorySize];
        for (int slot = 0; slot < inventorySize; slot++) {
            resolvedSlots[slot] = slot;
        }

        return resolvedSlots;
    }
}
