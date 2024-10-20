package me.mrCookieSlime.CSCoreLibPlugin.general.Inventory;

import city.norain.slimefun4.holder.SlimefunInventoryHolder;
import city.norain.slimefun4.utils.InventoryUtil;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * An old remnant of CS-CoreLib.
 * This will be removed once we updated everything.
 * Don't look at the code, it will be gone soon, don't worry.
 */
@Deprecated
public class ChestMenu extends SlimefunInventoryHolder {

    private boolean clickable;
    private boolean emptyClickable;

    @Getter
    private String title;

    private List<ItemStack> items;
    /**
     * Size of chestmenu
     * Warning: it DOES NOT present actual size of its inventory!
     */
    private int size = -1;

    private Map<Integer, MenuClickHandler> handlers;
    private MenuOpeningHandler open;
    private MenuCloseHandler close;
    private MenuClickHandler playerclick;

    private final Set<UUID> viewers = new CopyOnWriteArraySet<>();
    private final AtomicBoolean lock = new AtomicBoolean(false);

    /**
     * Creates a new ChestMenu with the specified
     * Title
     *
     * @param title The title of the Menu
     */
    public ChestMenu(String title) {
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        this.clickable = false;
        this.emptyClickable = true;
        this.items = new ArrayList<>();
        this.handlers = new HashMap<>();

        this.open = p -> {};
        this.close = p -> {};
        this.playerclick = (p, slot, item, action) -> isPlayerInventoryClickable();
    }

    public ChestMenu(String title, int size) {
        this(title);
        setSize(size);
    }

    /**
     * Toggles whether Players can access there
     * Inventory while viewing this Menu
     *
     * @param clickable Whether the Player can access his Inventory
     * @return The ChestMenu Instance
     */
    public ChestMenu setPlayerInventoryClickable(boolean clickable) {
        this.clickable = clickable;
        return this;
    }

    /**
     * Returns whether the Player's Inventory is
     * accessible while viewing this Menu
     *
     * @return Whether the Player Inventory is clickable
     */
    public boolean isPlayerInventoryClickable() {
        return clickable;
    }

    /**
     * Toggles whether Players can click the
     * empty menu slots while viewing this Menu
     *
     * @param emptyClickable Whether the Player can click empty slots
     * @return The ChestMenu Instance
     */
    public ChestMenu setEmptySlotsClickable(boolean emptyClickable) {
        this.emptyClickable = emptyClickable;
        return this;
    }

    /**
     * Returns whether the empty menu slots are
     * clickable while viewing this Menu
     *
     * @return Whether the empty menu slots are clickable
     */
    public boolean isEmptySlotsClickable() {
        return emptyClickable;
    }

    /**
     * Adds a ClickHandler to ALL Slots of the
     * Player's Inventory
     *
     * @param handler The MenuClickHandler
     * @return The ChestMenu Instance
     */
    public ChestMenu addPlayerInventoryClickHandler(MenuClickHandler handler) {
        this.playerclick = handler;
        return this;
    }

    /**
     * Adds an Item to the Inventory in that Slot
     *
     * @param slot The Slot in the Inventory
     * @param item The Item for that Slot
     * @return The ChestMenu Instance
     */
    public ChestMenu addItem(int slot, ItemStack item) {
        // do shallow copy due to Paper ItemStack system change
        // See also: https://github.com/PaperMC/Paper/pull/10852
        ItemStack actual = item;
        if (item instanceof SlimefunItemStack) {
            ItemStack clone = new ItemStack(item.getType(), item.getAmount());

            if (item.hasItemMeta()) {
                clone.setItemMeta(item.getItemMeta());
            }

            actual = clone;
        }

        setSize((int) (Math.max(getSize(), Math.ceil((slot + 1) / 9d) * 9)));

        this.items.set(slot, actual);
        this.inventory.setItem(slot, actual);
        return this;
    }

    /**
     * Adds an Item to the Inventory in that Slot
     * as well as a Click Handler
     *
     * @param slot         The Slot in the Inventory
     * @param item         The Item for that Slot
     * @param clickHandler The MenuClickHandler for that Slot
     * @return The ChestMenu Instance
     */
    public ChestMenu addItem(int slot, ItemStack item, MenuClickHandler clickHandler) {
        addItem(slot, item);
        addMenuClickHandler(slot, clickHandler);
        return this;
    }

    /**
     * Returns the ItemStack in that Slot
     *
     * @param slot The Slot in the Inventory
     * @return The ItemStack in that Slot
     */
    public ItemStack getItemInSlot(int slot) {
        setup();
        if (items.size() - 1 < slot) {
            addItem(slot, null);
        }
        return this.inventory.getItem(slot);
    }

    /**
     * Executes a certain Action upon clicking an
     * Item in the Menu
     *
     * @param slot    The Slot in the Inventory
     * @param handler The MenuClickHandler
     * @return The ChestMenu Instance
     */
    public ChestMenu addMenuClickHandler(int slot, MenuClickHandler handler) {
        this.handlers.put(slot, handler);
        return this;
    }

    /**
     * Executes a certain Action upon opening
     * this Menu
     *
     * @param handler The MenuOpeningHandler
     * @return The ChestMenu Instance
     */
    public ChestMenu addMenuOpeningHandler(MenuOpeningHandler handler) {
        this.open = handler;
        return this;
    }

    /**
     * Executes a certain Action upon closing
     * this Menu
     *
     * @param handler The MenuCloseHandler
     * @return The ChestMenu Instance
     */
    public ChestMenu addMenuCloseHandler(MenuCloseHandler handler) {
        this.close = handler;
        return this;
    }

    /**
     * Finishes the Creation of the Menu
     *
     * @return The ChestMenu Instance
     */
    @Deprecated
    public ChestMenu build() {
        return this;
    }

    /**
     * Returns an Array containing the Contents
     * of this Inventory
     *
     * @return The Contents of this Inventory
     */
    public ItemStack[] getContents() {
        setup();
        return this.inventory.getContents();
    }

    public void addViewer(@Nonnull UUID uuid) {
        viewers.add(uuid);
    }

    public void removeViewer(@Nonnull UUID uuid) {
        viewers.remove(uuid);
    }

    public boolean contains(@Nonnull Player viewer) {
        return viewers.contains(viewer.getUniqueId());
    }

    private void setup() {
        if (this.inventory != null) return;
        this.inventory = Bukkit.createInventory(this, getSize(), title);
        for (int i = 0; i < this.items.size(); i++) {
            this.inventory.setItem(i, this.items.get(i));
        }
    }

    /**
     * Resets this ChestMenu to a Point BEFORE the User interacted with it
     */
    public void reset(boolean update) {
        if (this.inventory == null || this.inventory.getSize() != getSize())
            this.inventory = Bukkit.createInventory(this, getSize(), title);

        if (update) {
            this.inventory.clear();
        } else {
            this.inventory = Bukkit.createInventory(this, getSize(), title);
        }

        for (int i = 0; i < this.items.size(); i++) {
            this.inventory.setItem(i, this.items.get(i));
        }
    }

    /**
     * Modifies an ItemStack in an ALREADY OPENED ChestMenu
     *
     * @param slot The Slot of the Item which will be replaced
     * @param item The new Item
     */
    public void replaceExistingItem(int slot, ItemStack item) {
        setup();
        this.inventory.setItem(slot, item);
    }

    /**
     * Opens this Menu for the specified Player/s
     *
     * @param players The Players who will see this Menu
     */
    public void open(Player... players) {
        setup();
        for (Player p : players) {
            p.openInventory(this.inventory);
            addViewer(p.getUniqueId());
            if (open != null) open.onOpen(p);
        }
    }

    /**
     * Returns the MenuClickHandler which was registered for the specified Slot
     *
     * @param slot The Slot in the Inventory
     * @return The MenuClickHandler registered for the specified Slot
     */
    public MenuClickHandler getMenuClickHandler(int slot) {
        return handlers.get(slot);
    }

    /**
     * Returns the registered MenuCloseHandler
     *
     * @return The registered MenuCloseHandler
     */
    public MenuCloseHandler getMenuCloseHandler() {
        return close;
    }

    /**
     * Returns the registered MenuOpeningHandler
     *
     * @return The registered MenuOpeningHandler
     */
    public MenuOpeningHandler getMenuOpeningHandler() {
        return open;
    }

    /**
     * Returns the registered MenuClickHandler
     * for Player Inventories
     *
     * @return The registered MenuClickHandler
     */
    public MenuClickHandler getPlayerInventoryClickHandler() {
        return playerclick;
    }

    /**
     * Converts this ChestMenu Instance into a
     * normal Inventory
     *
     * @return The converted Inventory
     */
    public Inventory toInventory() {
        return this.inventory;
    }

    public int getSize() {
        return isSizeAutomaticallyInferred() ? Math.max(9, (int) Math.ceil(this.items.size() / 9F) * 9) : size;
    }

    public ChestMenu setSize(int size) {
        if (size % 9 == 0 && size >= 0 && size < 55) {
            // Resize items list to match actual inventory size in order to reset inventory.
            // I'm sure that use size of items as inventory size is somehow strange.
            if (size > items.size()) {
                while (items.size() < size) {
                    this.items.add(null);
                }
            } else if (size < items.size()) {
                while (items.size() > size) {
                    this.items.remove(items.size() - 1);
                }
            } else {
                return this;
            }

            this.size = size;

            reset(false);

            return this;
        } else {
            throw new IllegalArgumentException(
                    "The size of a ChestMenu must be a multiple of 9 and within the bounds 0-54,"
                            + " received: "
                            + size);
        }
    }

    public boolean isSizeAutomaticallyInferred() {
        return size == -1;
    }

    public boolean locked() {
        return lock.get();
    }

    public void lock() {
        lock.getAndSet(true);
        InventoryUtil.closeInventory(this.inventory);
    }

    public void unlock() {
        lock.getAndSet(false);
    }

    @FunctionalInterface
    public interface MenuClickHandler {

        public boolean onClick(Player p, int slot, ItemStack item, ClickAction action);
    }

    public interface AdvancedMenuClickHandler extends MenuClickHandler {

        public boolean onClick(InventoryClickEvent e, Player p, int slot, ItemStack cursor, ClickAction action);
    }

    @FunctionalInterface
    public interface MenuOpeningHandler {

        public void onOpen(Player p);
    }

    @FunctionalInterface
    public interface MenuCloseHandler {

        public void onClose(Player p);
    }
}
