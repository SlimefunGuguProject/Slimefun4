package io.github.thebusybiscuit.slimefun4.api.player;

import city.norain.slimefun4.holder.SlimefunInventoryHolder;
import city.norain.slimefun4.utils.InventoryUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.InvSnapshot;
import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.common.CommonPatterns;
import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.backpacks.SlimefunBackpack;
import io.github.thebusybiscuit.slimefun4.implementation.listeners.BackpackListener;
import io.github.thebusybiscuit.slimefun4.utils.ThreadUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * This class represents the instance of a {@link SlimefunBackpack} that is ready to
 * be opened.
 *
 * It holds an actual {@link Inventory} and represents the backpack on the
 * level of an individual {@link ItemStack} as opposed to the class {@link SlimefunBackpack}.
 *
 * @author TheBusyBiscuit
 *
 * @see SlimefunBackpack
 * @see BackpackListener
 */
@SlimefunAPI
public class PlayerBackpack extends SlimefunInventoryHolder {
    public static final String LORE_OWNER = "&7Owner: ";
    private static final String COLORED_LORE_OWNER = ChatColors.color(LORE_OWNER);
    private static final String COLORED_LORE_ID = ChatColors.color("&7ID: ");
    private static NamespacedKey backpackUuidKey;
    private static NamespacedKey ownerUuidKey;
    private final OfflinePlayer owner;
    private final UUID uuid;
    private final int id;
    private String name;
    private int size;
    private boolean isInvalid = false;
    // This snapshot holds the inventory's last save content , it should be recreated after each save by using
    // PlayerBackpack#refreshSnapshot
    @Nonnull
    @Getter
    private InvSnapshot snapshot;

    /**
     * Loads a backpack and executes the callback using the legacy global/main-thread behavior.
     *
     * <p>On Folia, callers that touch a player or world location should use the entity- or location-owned overload
     * instead.
     */
    public static void getAsync(ItemStack item, Consumer<PlayerBackpack> callback, boolean runCbOnMainThread) {
        Executor executor = runCbOnMainThread
                ? ThreadUtils.getMainDelayedExecutor()
                : Slimefun.getDatabaseManager().getProfileDataController().getCallbackExecutor();
        getAsync(item, callback, executor, runCbOnMainThread);
    }

    /**
     * Loads a backpack and invokes the callback on the thread that owns the supplied entity.
     *
     * @param item the backpack item
     * @param callback the callback to invoke
     * @param owner the entity that owns the callback context
     */
    public static void getAsync(ItemStack item, Consumer<PlayerBackpack> callback, Entity owner) {
        getAsync(item, callback, ThreadUtils.getEntityDelayedExecutor(owner), true);
    }

    private static void getAsync(
            ItemStack item, Consumer<PlayerBackpack> callback, Executor executor, boolean migrateItem) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        var bUuid = getBackpackUUID(item.getItemMeta());
        if (bUuid.isPresent()) {
            Slimefun.getDatabaseManager()
                    .getProfileDataController()
                    .getBackpackAsync(bUuid.get())
                    .thenAcceptAsync(
                            (result) -> {
                                if (result != null) {
                                    if (migrateItem) {
                                        migrateLegacyItem(item, result);
                                    }
                                    callback.accept(result);
                                }
                            },
                            executor);
            return;
        }

        // Old backpack item
        Optional<LegacyBackpackReference> legacyReference = getLegacyBackpackReference(item.getItemMeta());
        if (legacyReference.isPresent()) {
            LegacyBackpackReference reference = legacyReference.get();
            Slimefun.getDatabaseManager()
                    .getProfileDataController()
                    .getBackpackAsync(Bukkit.getOfflinePlayer(reference.owner()), reference.id())
                    .thenAcceptAsync(
                            (result) -> {
                                if (result != null) {
                                    if (migrateItem) {
                                        migrateLegacyItem(item, result);
                                    }
                                    callback.accept(result);
                                }
                            },
                            executor);
        }
    }

    public static CompletableFuture<PlayerBackpack> getAsync(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return CompletableFuture.completedFuture(null);
        }

        var bUuid = getBackpackUUID(item.getItemMeta());
        if (bUuid.isPresent()) {
            return Slimefun.getDatabaseManager().getProfileDataController().getBackpackAsync(bUuid.get());
        }

        // Old backpack item
        Optional<LegacyBackpackReference> legacyReference = getLegacyBackpackReference(item.getItemMeta());
        if (legacyReference.isPresent()) {
            LegacyBackpackReference reference = legacyReference.get();
            return Slimefun.getDatabaseManager()
                    .getProfileDataController()
                    .getBackpackAsync(Bukkit.getOfflinePlayer(reference.owner()), reference.id());
        }
        return CompletableFuture.completedFuture(null);
    }

    public static Optional<String> getBackpackUUID(ItemMeta meta) {
        if (meta == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                meta.getPersistentDataContainer().get(getBackpackUuidKey(), PersistentDataType.STRING));
    }

    public static Optional<String> getOwnerUUID(ItemMeta meta) {
        if (meta == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(meta.getPersistentDataContainer().get(getOwnerUuidKey(), PersistentDataType.STRING));
    }

    public static OptionalInt getBackpackID(ItemMeta meta) {
        if (meta == null || !meta.hasLore()) {
            return OptionalInt.empty();
        }

        return getLegacyBackpackReference(meta).map(reference -> OptionalInt.of(reference.id()))
                .orElseGet(OptionalInt::empty);
    }

    /**
     * Returns whether this item metadata belongs to an already-bound backpack.
     * Both the current PDC identity and the legacy visible ID lore are recognized.
     */
    public static boolean hasBackpackIdentity(@Nullable ItemMeta meta) {
        if (getLegacyBackpackReference(meta).isPresent()) {
            return true;
        }

        return meta != null && Slimefun.instance() != null && getBackpackUUID(meta).isPresent();
    }

    public static void setItemPdc(ItemStack item, String bpUuid, String ownerUuid) {
        ItemMeta meta = item.getItemMeta();
        setPdc(meta, bpUuid, ownerUuid);
        item.setItemMeta(meta);
    }

    public static void bindItem(ItemStack item, PlayerBackpack bp) {
        var meta = item.getItemMeta();
        setPdc(meta, bp.uuid.toString(), bp.owner.getUniqueId().toString());
        removeLegacyIdLore(meta);
        setItem(meta, bp);
        item.setItemMeta(meta);
    }

    /**
     * Migrates a resolved legacy backpack item to the current persistent identity
     * format and removes the old visible ID lore only after the PDC values can be
     * read back successfully.
     */
    public static void migrateLegacyItem(ItemStack item, PlayerBackpack bp) {
        if (item == null || bp == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        setPdc(meta, bp.uuid.toString(), bp.owner.getUniqueId().toString());

        var pdc = meta.getPersistentDataContainer();
        String storedBackpackUuid = pdc.get(getBackpackUuidKey(), PersistentDataType.STRING);
        String storedOwnerUuid = pdc.get(getOwnerUuidKey(), PersistentDataType.STRING);
        if (bp.uuid.toString().equals(storedBackpackUuid)
                && bp.owner.getUniqueId().toString().equals(storedOwnerUuid)) {
            removeLegacyIdLore(meta);
        }

        item.setItemMeta(meta);
    }

    public static void setItemDisplayInfo(ItemStack item, PlayerBackpack bp) {
        var meta = item.getItemMeta();
        setItem(meta, bp);
        item.setItemMeta(meta);
    }

    public static boolean isOwnerOnline(ItemMeta meta) {
        if (Slimefun.getCfg().getBoolean("backpack.allow-open-when-owner-offline")) {
            return true;
        }
        var ownerUuid = PlayerBackpack.getOwnerUUID(meta);
        if (ownerUuid.isEmpty()) {
            return true;
        }

        try {
            return Bukkit.getPlayer(UUID.fromString(ownerUuid.get())) != null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void setPdc(ItemMeta meta, String bpUuid, String ownerUuid) {
        var pdc = meta.getPersistentDataContainer();
        pdc.set(getBackpackUuidKey(), PersistentDataType.STRING, bpUuid);
        pdc.set(getOwnerUuidKey(), PersistentDataType.STRING, ownerUuid);
    }

    private static NamespacedKey getBackpackUuidKey() {
        if (backpackUuidKey == null) {
            backpackUuidKey = new NamespacedKey(Slimefun.instance(), "B_UUID");
        }
        return backpackUuidKey;
    }

    private static NamespacedKey getOwnerUuidKey() {
        if (ownerUuidKey == null) {
            ownerUuidKey = new NamespacedKey(Slimefun.instance(), "OWNER_UUID");
        }
        return ownerUuidKey;
    }

    private static void setItem(ItemMeta meta, PlayerBackpack bp) {
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        boolean ownerLineFound = false;
        for (var i = 0; i < lore.size(); i++) {
            var line = lore.get(i);
            if (line != null && line.startsWith(COLORED_LORE_OWNER)) {
                lore.set(i, COLORED_LORE_OWNER + bp.getOwner().getName());
                ownerLineFound = true;
                break;
            }
        }

        if (!ownerLineFound) {
            lore.add(COLORED_LORE_OWNER + bp.getOwner().getName());
        }
        meta.setLore(lore);

        if (bp.name.isEmpty() || bp.name.isBlank()) {
            return;
        }
        meta.setDisplayName(ChatColors.color(bp.name));
    }

    private static Optional<LegacyBackpackReference> getLegacyBackpackReference(@Nullable ItemMeta meta) {
        if (meta == null || !meta.hasLore()) {
            return Optional.empty();
        }

        for (String line : meta.getLore()) {
            if (line == null || !line.startsWith(COLORED_LORE_ID) || line.indexOf('#') == -1) {
                continue;
            }

            String identity = line.substring(COLORED_LORE_ID.length());
            String[] splitLine = CommonPatterns.HASH.split(identity, 2);
            if (splitLine.length != 2 || !CommonPatterns.NUMERIC.matcher(splitLine[1]).matches()) {
                continue;
            }

            try {
                return Optional.of(new LegacyBackpackReference(
                        UUID.fromString(splitLine[0]), Integer.parseInt(splitLine[1])));
            } catch (IllegalArgumentException ignored) {
                // Malformed legacy identity - keep searching in case another valid line exists.
            }
        }

        return Optional.empty();
    }

    private static void removeLegacyIdLore(ItemMeta meta) {
        if (meta == null || !meta.hasLore()) {
            return;
        }

        List<String> lore = new ArrayList<>(meta.getLore());
        lore.removeIf(line -> line != null && line.startsWith(COLORED_LORE_ID));
        meta.setLore(lore.isEmpty() ? null : lore);
    }

    private record LegacyBackpackReference(UUID owner, int id) {}

    @ParametersAreNonnullByDefault
    public PlayerBackpack(
            OfflinePlayer owner, UUID uuid, String name, int id, int size, @Nullable ItemStack[] contents) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Invalid size! Size must be one of: [9, 18, 27, 36, 45, 54]");
        }

        this.owner = owner;
        this.uuid = uuid;
        this.name = name;
        this.id = id;
        this.size = size;
        inventory = newInv();

        if (contents != null) {
            if (size != contents.length) {
                throw new IllegalArgumentException("Invalid contents: size mismatched!");
            }
            inventory.setContents(contents);
        }

        this.snapshot = new InvSnapshot(inventory);
    }

    /**
     * This refreshes the internal snapshot,
     * It should be called after every database writing task
     * It should not be called elsewhere
     */
    public void refreshSnapshot() {
        this.snapshot = new InvSnapshot(inventory);
    }

    /**
     * This returns the id of this {@link PlayerBackpack}
     *
     * @return The id of this {@link PlayerBackpack}
     */
    public int getId() {
        return id;
    }

    /**
     * This method returns the {@link PlayerProfile} this {@link PlayerBackpack} belongs to
     *
     * @return The owning {@link PlayerProfile}
     */
    @Nonnull
    public OfflinePlayer getOwner() {
        return owner;
    }

    /**
     * This returns the size of this {@link PlayerBackpack}.
     *
     * @return The size of this {@link PlayerBackpack}
     */
    public int getSize() {
        return size;
    }

    /**
     * This method returns the {@link Inventory} of this {@link PlayerBackpack}
     *
     * @return The {@link Inventory} of this {@link PlayerBackpack}
     */
    @Nonnull
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * This will open the {@link Inventory} of this backpack to every {@link Player}
     * that was passed onto this method.
     * <p>
     * 二进制兼容
     *
     * @param p The player who this Backpack will be shown to
     */
    public void open(Player p) {
        if (isInvalid) {
            return;
        }

        InventoryUtil.openInventory(p, inventory);
    }

    /**
     * This will change the current size of this Backpack to the specified size.
     *
     * @param size
     *            The new size for this Backpack
     */
    public void setSize(int size) {
        if (size < 9 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException("Invalid size! Size must be one of: [9, 18, 27, 36, 45, 54]");
        }

        this.size = size;
        updateInv();
        Slimefun.getDatabaseManager().getProfileDataController().saveBackpackInfo(this);
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public void setName(String name) {
        this.name = name;
        updateInv();
        Slimefun.getDatabaseManager().getProfileDataController().saveBackpackInfo(this);
    }

    public String getName() {
        return name;
    }

    public void markInvalid() {
        isInvalid = true;
        InventoryUtil.closeInventory(this.inventory);
        Slimefun.getDatabaseManager().getProfileDataController().saveBackpackInventory(this);
    }

    public boolean isInvalid() {
        return isInvalid;
    }

    /**
     * Construct a new backpack inventory.
     * <p>
     * Warning: You should **manually** update inventory contents!
     *
     * @return new {@link Inventory}
     */
    private Inventory newInv() {
        return Bukkit.createInventory(
                this, size, (name.isEmpty() ? "Backpack" : ChatColors.color(name + "&r")) + " [Size " + size + "]");
    }

    private void updateInv() {
        InventoryUtil.closeInventory(this.inventory);
        var inv = newInv();
        inv.setContents(this.inventory.getContents());
        this.inventory.clear();
        this.inventory = inv;
        setInventory(inv);
    }
}
