package io.github.thebusybiscuit.slimefun4.core.services;

import io.github.bakedlibs.dough.inventory.InvUtils;
import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemState;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.AdmissionResult;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.ComparisonResult;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.ConsumeContext;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.InventoryContext;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.ItemResult;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.MatchContext;
import io.github.thebusybiscuit.slimefun4.api.items.virtual.VirtualItemHandler.RemainderContext;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import io.github.thebusybiscuit.slimefun4.utils.itemstack.ItemStackWrapper;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class VirtualItemService {

    private static final String ADDON_OWNER_KEY = "virtual_item_owner_addon";
    private static final String ITEM_OWNER_KEY = "virtual_item_owner_item";

    private final Map<String, VirtualItemHandler> addonHandlers = new ConcurrentHashMap<>();
    private final Map<String, VirtualItemHandler> itemHandlers = new ConcurrentHashMap<>();

    private volatile @Nullable NamespacedKey addonOwnerKey;
    private volatile @Nullable NamespacedKey itemOwnerKey;

    public void registerHandler(@Nonnull SlimefunAddon addon, @Nonnull VirtualItemHandler handler) {
        Validate.notNull(addon, "The addon cannot be null");
        Validate.notNull(handler, "The handler cannot be null");
        addonHandlers.put(getAddonKey(addon), handler);
    }

    public void unregisterHandler(@Nonnull SlimefunAddon addon) {
        Validate.notNull(addon, "The addon cannot be null");
        addonHandlers.remove(getAddonKey(addon));
    }

    public void clearHandlers(@Nonnull SlimefunAddon addon) {
        Validate.notNull(addon, "The addon cannot be null");

        String addonKey = getAddonKey(addon);
        addonHandlers.remove(addonKey);

        for (String itemId : itemHandlers.keySet()) {
            if (addonOwnsItem(addonKey, itemId)) {
                itemHandlers.remove(itemId);
            }
        }
    }

    public @Nullable VirtualItemHandler getHandler(@Nonnull SlimefunAddon addon) {
        Validate.notNull(addon, "The addon cannot be null");
        return addonHandlers.get(getAddonKey(addon));
    }

    public boolean hasHandler(@Nonnull SlimefunAddon addon) {
        return getHandler(addon) != null;
    }

    public void registerHandler(@Nonnull SlimefunItem item, @Nonnull VirtualItemHandler handler) {
        Validate.notNull(item, "The SlimefunItem cannot be null");
        Validate.notNull(handler, "The handler cannot be null");
        Validate.isTrue(
                item.getState() != ItemState.UNREGISTERED,
                "The SlimefunItem must be registered before assigning a virtual item handler");

        itemHandlers.put(item.getId(), handler);
    }

    public void unregisterHandler(@Nonnull SlimefunItem item) {
        Validate.notNull(item, "The SlimefunItem cannot be null");
        itemHandlers.remove(item.getId());
    }

    public @Nullable VirtualItemHandler getHandler(@Nonnull SlimefunItem item) {
        Validate.notNull(item, "The SlimefunItem cannot be null");

        VirtualItemHandler itemHandler = itemHandlers.get(item.getId());
        if (itemHandler != null) {
            return itemHandler;
        }

        if (item.getState() == ItemState.UNREGISTERED) {
            return null;
        }

        return addonHandlers.get(getAddonKey(item.getAddon()));
    }

    public boolean hasHandler(@Nonnull SlimefunItem item) {
        return getHandler(item) != null;
    }

    public boolean hasHandler() {
        return !addonHandlers.isEmpty() || !itemHandlers.isEmpty();
    }

    public void setOwner(@Nonnull ItemStack item, @Nonnull SlimefunAddon addon) {
        Validate.notNull(item, "The item cannot be null");
        Validate.notNull(addon, "The addon cannot be null");

        ItemMeta meta = item.getItemMeta();
        Validate.notNull(meta, "The item meta cannot be null");
        setOwner(meta, addon);
        item.setItemMeta(meta);
    }

    public void setOwner(@Nonnull ItemMeta meta, @Nonnull SlimefunAddon addon) {
        Validate.notNull(meta, "The item meta cannot be null");
        Validate.notNull(addon, "The addon cannot be null");

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(getItemOwnerKey());
        container.set(getAddonOwnerKey(), PersistentDataType.STRING, getAddonKey(addon));
    }

    public void setOwner(@Nonnull ItemStack item, @Nonnull SlimefunItem slimefunItem) {
        Validate.notNull(item, "The item cannot be null");
        Validate.notNull(slimefunItem, "The SlimefunItem cannot be null");

        ItemMeta meta = item.getItemMeta();
        Validate.notNull(meta, "The item meta cannot be null");
        setOwner(meta, slimefunItem);
        item.setItemMeta(meta);
    }

    public void setOwner(@Nonnull ItemMeta meta, @Nonnull SlimefunItem slimefunItem) {
        Validate.notNull(meta, "The item meta cannot be null");
        Validate.notNull(slimefunItem, "The SlimefunItem cannot be null");
        Validate.isTrue(
                slimefunItem.getState() != ItemState.UNREGISTERED,
                "The SlimefunItem must be registered before assigning it as a virtual item owner");

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(getAddonOwnerKey());
        container.set(getItemOwnerKey(), PersistentDataType.STRING, slimefunItem.getId());
    }

    public void clearOwner(@Nonnull ItemStack item) {
        Validate.notNull(item, "The item cannot be null");

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        clearOwner(meta);
        item.setItemMeta(meta);
    }

    public void clearOwner(@Nonnull ItemMeta meta) {
        Validate.notNull(meta, "The item meta cannot be null");
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(getAddonOwnerKey());
        container.remove(getItemOwnerKey());
    }

    public boolean isVirtualItem(@Nullable ItemStack item) {
        return resolveHandler(item) != null;
    }

    public @Nonnull ComparisonResult matches(
            @Nullable ItemStack left, @Nullable ItemStack right, @Nonnull MatchContext context) {
        ResolvedHandler leftHandler = resolveHandler(left);
        ResolvedHandler rightHandler = resolveHandler(right);

        if (leftHandler == null && rightHandler == null) {
            return ComparisonResult.NOT_HANDLED;
        }

        if (leftHandler != null && rightHandler != null) {
            VirtualItemHandler sharedHandler = resolveSharedHandler(leftHandler, rightHandler);
            if (sharedHandler == null) {
                return ComparisonResult.NO_MATCH;
            }

            return sharedHandler.matches(left, right, context);
        }

        return (leftHandler != null ? leftHandler.handler() : rightHandler.handler()).matches(left, right, context);
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
        ResolvedHandler resolvedHandler = resolveHandler(item);
        if (resolvedHandler != null) {
            ComparisonResult comparison = resolvedHandler.handler().matchesPredicate(item, predicate, context);
            if (comparison == ComparisonResult.MATCH) {
                return true;
            }

            if (comparison == ComparisonResult.NO_MATCH) {
                return false;
            }
        }

        return predicate.test(item);
    }

    public int getMaxStackSize(@Nonnull ItemStack item, @Nonnull InventoryContext context, int defaultMaxStackSize) {
        Validate.notNull(item, "The item cannot be null");

        ResolvedHandler resolvedHandler = resolveHandler(item);
        if (resolvedHandler == null) {
            return defaultMaxStackSize;
        }

        return Math.max(1, resolvedHandler.handler().getMaxStackSize(item, context, defaultMaxStackSize));
    }

    public @Nonnull AdmissionResult allows(@Nonnull ItemStack item, @Nonnull InventoryContext context) {
        ResolvedHandler resolvedHandler = resolveHandler(item);
        if (resolvedHandler == null) {
            return AdmissionResult.NOT_HANDLED;
        }

        return resolvedHandler.handler().allows(item, context);
    }

    public boolean canInsertIntoEmptySlot(@Nonnull ItemStack item, @Nonnull InventoryContext context) {
        return allows(item, context) != AdmissionResult.DENY;
    }

    public @Nonnull ItemResult consume(
            @Nullable ItemStack item, int amount, boolean replaceConsumables, @Nonnull ConsumeContext context) {
        ResolvedHandler resolvedHandler = resolveHandler(item);
        if (resolvedHandler == null) {
            return ItemResult.notHandled();
        }

        return resolvedHandler.handler().consume(item, amount, replaceConsumables, context);
    }

    public @Nonnull ItemResult getRemainder(@Nullable ItemStack item, @Nonnull RemainderContext context) {
        ResolvedHandler resolvedHandler = resolveHandler(item);
        if (resolvedHandler == null) {
            return ItemResult.notHandled();
        }

        return resolvedHandler.handler().getRemainder(item, context);
    }

    public boolean fits(
            @Nonnull Inventory inventory, @Nonnull ItemStack item, @Nonnull InventoryContext context, int... slots) {
        if (!hasVirtualItemsInSlots(inventory, slots) && !isVirtualItem(item) && SlimefunItem.getByItem(item) == null) {
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
            hasVirtualItems = hasVirtualItemsInSlots(inventory, slots);
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

        boolean hasVirtualItems = hasVirtualItemsInSlots(inventory, slots);

        if (!hasVirtualItems && !isVirtualItem(item) && SlimefunItem.getByItem(item) == null && slots.length == 0) {
            Map<Integer, ItemStack> leftovers = inventory.addItem(item.clone());
            if (leftovers.isEmpty()) {
                return null;
            }

            return leftovers.values().iterator().next();
        }

        if (!hasVirtualItems && !isVirtualItem(item) && SlimefunItem.getByItem(item) == null && slots.length > 0) {
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

    private boolean hasVirtualItemsInSlots(@Nonnull Inventory inventory, int... slots) {
        for (int slot : resolveSlots(inventory.getSize(), slots)) {
            if (resolveHandler(inventory.getItem(slot)) != null) {
                return true;
            }
        }

        return false;
    }

    private @Nullable ResolvedHandler resolveHandler(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }

        SlimefunItem slimefunItem = SlimefunItem.getByItem(item);
        if (slimefunItem != null) {
            return resolveSlimefunHandler(slimefunItem, item);
        }

        String ownedItem = getStoredItemOwner(item);
        if (ownedItem != null) {
            return resolveOwnedItemHandler(ownedItem, item);
        }

        String ownedAddon = getStoredAddonOwner(item);
        if (ownedAddon != null) {
            return resolveOwnedAddonHandler(ownedAddon, item);
        }

        return null;
    }

    private @Nullable ResolvedHandler resolveSlimefunHandler(@Nonnull SlimefunItem item, @Nonnull ItemStack stack) {
        String addonKey = getAddonKey(item.getAddon());

        VirtualItemHandler itemHandler = itemHandlers.get(item.getId());
        if (itemHandler != null && itemHandler.isVirtualItem(stack)) {
            return new ResolvedHandler(itemHandler, addonKey);
        }

        VirtualItemHandler addonHandler = addonHandlers.get(addonKey);
        if (addonHandler != null && addonHandler.isVirtualItem(stack)) {
            return new ResolvedHandler(addonHandler, addonKey);
        }

        return null;
    }

    private @Nullable ResolvedHandler resolveOwnedItemHandler(@Nonnull String itemId, @Nonnull ItemStack item) {
        VirtualItemHandler itemHandler = itemHandlers.get(itemId);
        SlimefunItem slimefunItem = SlimefunItem.getById(itemId);
        if (slimefunItem == null || slimefunItem.getState() == ItemState.UNREGISTERED) {
            return null;
        }

        String addonKey = getAddonKey(slimefunItem);

        if (itemHandler != null && itemHandler.isVirtualItem(item)) {
            return new ResolvedHandler(itemHandler, addonKey);
        }

        VirtualItemHandler addonHandler = addonHandlers.get(addonKey);
        if (addonHandler != null && addonHandler.isVirtualItem(item)) {
            return new ResolvedHandler(addonHandler, addonKey);
        }

        return null;
    }

    private @Nullable ResolvedHandler resolveOwnedAddonHandler(@Nonnull String owner, @Nonnull ItemStack item) {
        VirtualItemHandler handler = addonHandlers.get(owner);
        if (handler == null || !handler.isVirtualItem(item)) {
            return null;
        }

        return new ResolvedHandler(handler, owner);
    }

    private @Nullable VirtualItemHandler resolveSharedHandler(
            @Nonnull ResolvedHandler left, @Nonnull ResolvedHandler right) {
        if (left.handler() == right.handler()) {
            return left.handler();
        }

        if (left.addonKey().equals(right.addonKey())) {
            return addonHandlers.get(left.addonKey());
        }

        return null;
    }

    private @Nullable String getStoredAddonOwner(@Nonnull ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(getAddonOwnerKey(), PersistentDataType.STRING);
    }

    private @Nullable String getStoredItemOwner(@Nonnull ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        return container.get(getItemOwnerKey(), PersistentDataType.STRING);
    }

    private @Nonnull NamespacedKey getAddonOwnerKey() {
        NamespacedKey currentKey = addonOwnerKey;
        if (currentKey == null) {
            currentKey = new NamespacedKey(Slimefun.instance(), ADDON_OWNER_KEY);
            addonOwnerKey = currentKey;
        }

        return currentKey;
    }

    private @Nonnull NamespacedKey getItemOwnerKey() {
        NamespacedKey currentKey = itemOwnerKey;
        if (currentKey == null) {
            currentKey = new NamespacedKey(Slimefun.instance(), ITEM_OWNER_KEY);
            itemOwnerKey = currentKey;
        }

        return currentKey;
    }

    private boolean addonOwnsItem(@Nonnull String addonKey, @Nonnull String itemId) {
        SlimefunItem slimefunItem = SlimefunItem.getById(itemId);
        return slimefunItem != null
                && slimefunItem.getState() != ItemState.UNREGISTERED
                && addonKey.equals(getAddonKey(slimefunItem));
    }

    private @Nonnull String getAddonKey(@Nonnull SlimefunAddon addon) {
        return addon.getName().toLowerCase(Locale.ROOT);
    }

    private @Nonnull String getAddonKey(@Nonnull SlimefunItem item) {
        return getAddonKey(item.getAddon());
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

    private record ResolvedHandler(@Nonnull VirtualItemHandler handler, @Nonnull String addonKey) {}
}
