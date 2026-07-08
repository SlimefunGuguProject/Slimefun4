package io.github.thebusybiscuit.slimefun4.api.items.virtual;

import io.github.thebusybiscuit.slimefun4.api.items.ItemHandler;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;

/**
 * A hook for virtual or proxy items whose behavior cannot be fully expressed by the
 * {@link ItemStack}'s visible {@link org.bukkit.Material}.
 *
 * <p>Implementations are expected to keep all hot-path methods cheap. They should only inspect
 * compact metadata that was precomputed when the virtual item was created.
 *
 * <p>Register this handler on a {@link io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem}
 * via {@link io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem#addItemHandler(ItemHandler...)}.
 */
public interface VirtualItemHandler extends ItemHandler {

    @Override
    @Nonnull
    default Class<? extends ItemHandler> getIdentifier() {
        return VirtualItemHandler.class;
    }

    /**
     * The context in which two stacks are being compared.
     */
    enum MatchContext {
        GENERIC,
        STACK_MERGE,
        RECIPE_INPUT,
        AUTO_CRAFTER_PREDICATE
    }

    /**
     * The context in which a stack is inserted into an inventory.
     */
    enum InventoryContext {
        MENU_FIT,
        MENU_INSERT,
        CARGO_INSERT,
        VANILLA_FIT,
        MACHINE_OUTPUT,
        OUTPUT_CHEST
    }

    /**
     * The context in which a stack is consumed.
     */
    enum ConsumeContext {
        MENU_CONSUME,
        VIRTUAL_CRAFTING
    }

    /**
     * The context in which a crafting remainder is resolved.
     */
    enum RemainderContext {
        AUTO_CRAFTER,
        VIRTUAL_CRAFTING
    }

    /**
     * A tri-state comparison result.
     */
    enum ComparisonResult {
        NOT_HANDLED,
        MATCH,
        NO_MATCH
    }

    /**
     * A tri-state admission result.
     */
    enum AdmissionResult {
        NOT_HANDLED,
        ALLOW,
        DENY
    }

    /**
     * A handled value result for operations that replace a stack.
     *
     * @param handled
     *            Whether the handler processed this request
     * @param item
     *            The replacement item, may be null to clear the slot
     */
    record ItemResult(boolean handled, @Nullable ItemStack item) {

        private static final ItemResult NOT_HANDLED = new ItemResult(false, null);

        public static @Nonnull ItemResult notHandled() {
            return NOT_HANDLED;
        }

        public static @Nonnull ItemResult handled(@Nullable ItemStack item) {
            return new ItemResult(true, item);
        }
    }

    /**
     * This should be a very cheap check that determines whether the given stack belongs to this
     * virtual item system.
     *
     * @param item
     *            The stack to inspect
     *
     * @return Whether this stack should be handled by this hook
     */
    default boolean isVirtualItem(@Nullable ItemStack item) {
        return false;
    }

    /**
     * Compare two stacks in a given context.
     *
     * @param left
     *            The left stack
     * @param right
     *            The right stack
     * @param context
     *            The comparison context
     *
     * @return The comparison result
     */
    default @Nonnull ComparisonResult matches(
            @Nullable ItemStack left, @Nullable ItemStack right, @Nonnull MatchContext context) {
        return ComparisonResult.NOT_HANDLED;
    }

    /**
     * Compare a stack against a recipe predicate.
     *
     * @param item
     *            The stack to inspect
     * @param predicate
     *            The target predicate
     * @param context
     *            The comparison context
     *
     * @return The comparison result
     */
    default @Nonnull ComparisonResult matchesPredicate(
            @Nonnull ItemStack item, @Nonnull Predicate<ItemStack> predicate, @Nonnull MatchContext context) {
        return ComparisonResult.NOT_HANDLED;
    }

    /**
     * Resolve the effective max stack size for the given stack.
     *
     * @param item
     *            The stack to inspect
     * @param context
     *            The insertion context
     * @param defaultMaxStackSize
     *            The original max stack size
     *
     * @return The effective max stack size
     */
    default int getMaxStackSize(@Nonnull ItemStack item, @Nonnull InventoryContext context, int defaultMaxStackSize) {
        return defaultMaxStackSize;
    }

    /**
     * Decide whether a virtual stack may enter an empty slot in a given context.
     *
     * @param item
     *            The stack to inspect
     * @param context
     *            The insertion context
     *
     * @return The admission result
     */
    default @Nonnull AdmissionResult allows(@Nonnull ItemStack item, @Nonnull InventoryContext context) {
        return AdmissionResult.NOT_HANDLED;
    }

    /**
     * Consume an amount from a stack.
     *
     * @param item
     *            The original stack
     * @param amount
     *            The amount to consume
     * @param replaceConsumables
     *            Whether consumables should be replaced
     * @param context
     *            The consume context
     *
     * @return The replacement result
     */
    default @Nonnull ItemResult consume(
            @Nonnull ItemStack item, int amount, boolean replaceConsumables, @Nonnull ConsumeContext context) {
        return ItemResult.notHandled();
    }

    /**
     * Resolve the remainder created when a stack is consumed for a crafting operation.
     *
     * @param item
     *            The consumed stack
     * @param context
     *            The crafting context
     *
     * @return The remainder result
     */
    default @Nonnull ItemResult getRemainder(@Nonnull ItemStack item, @Nonnull RemainderContext context) {
        return ItemResult.notHandled();
    }
}
