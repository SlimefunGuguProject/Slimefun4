package io.github.thebusybiscuit.slimefun4.integrations;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * An optional runtime bridge for AdvancedEnchantments.
 *
 * <p>This integration deliberately has no compile-time dependency on AdvancedEnchantments. Slimefun continues to load
 * and use its vanilla enchantment machines when AdvancedEnchantments is not installed.
 */
public final class AdvancedEnchantmentsIntegration {

    private static final String PLUGIN_NAME = "AdvancedEnchantments";
    private static final String API_CLASS = "net.advancedplugins.ae.api.AEAPI";
    private static final String NAMESPACE = "advancedenchantments";
    private static final String ENCHANTMENT_KEY_PREFIX = "ae_enchantment-";

    private static final NamespacedKey BOOK_KEY = key("ae_book");
    private static final NamespacedKey BOOK_LEVEL_KEY = key("ae_book_level");
    private static final NamespacedKey BOOK_SUCCESS_KEY = key("ae_book_success");
    private static final NamespacedKey BOOK_FAILURE_KEY = key("ae_book_failure");

    private final Plugin plugin;
    private final @Nullable Method applyEnchant;
    private final @Nullable ApplyEnchantOrder applyEnchantOrder;
    private final @Nullable Method removeEnchantment;
    private final @Nullable RemoveEnchantOrder removeEnchantOrder;
    private final @Nullable Method organizeEnchants;

    private final AtomicBoolean apiFailureLogged = new AtomicBoolean();

    /**
     * Creates the optional bridge using classes from the installed AdvancedEnchantments plugin.
     *
     * @param advancedEnchantments the enabled AdvancedEnchantments plugin
     */
    public AdvancedEnchantmentsIntegration(@Nonnull Plugin advancedEnchantments) {
        plugin = advancedEnchantments;

        Class<?> api = loadApiClass(advancedEnchantments);
        Method currentApply = findStaticMethod(api, "applyEnchant", String.class, int.class, ItemStack.class);
        if (currentApply != null) {
            applyEnchant = currentApply;
            applyEnchantOrder = ApplyEnchantOrder.NAME_LEVEL_ITEM;
        } else {
            applyEnchant = findStaticMethod(api, "applyEnchant", ItemStack.class, String.class, int.class);
            applyEnchantOrder = applyEnchant == null ? null : ApplyEnchantOrder.ITEM_NAME_LEVEL;
        }

        Method itemNameRemoval = findStaticMethod(api, "removeEnchantment", ItemStack.class, String.class);
        if (itemNameRemoval != null) {
            removeEnchantment = itemNameRemoval;
            removeEnchantOrder = RemoveEnchantOrder.ITEM_NAME;
        } else {
            removeEnchantment = findStaticMethod(api, "removeEnchantment", String.class, ItemStack.class);
            removeEnchantOrder = removeEnchantment == null ? null : RemoveEnchantOrder.NAME_ITEM;
        }

        organizeEnchants = findStaticMethod(api, "organizeEnchants", ItemStack.class);
    }

    /**
     * Reads applied custom enchantment identifiers and levels from an item.
     *
     * @param item the item to inspect
     *
     * @return an immutable map of AE enchantment IDs to levels
     */
    public @Nonnull Map<String, Integer> getEnchantments(@Nullable ItemStack item) {
        if (!isAvailable() || item == null || !item.hasItemMeta()) {
            return Collections.emptyMap();
        }

        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        Map<String, Integer> enchantments = new LinkedHashMap<>();

        data.getKeys().stream()
                .filter(AdvancedEnchantmentsIntegration::isEnchantmentKey)
                .sorted((first, second) -> first.getKey().compareTo(second.getKey()))
                .forEach(key -> {
                    Integer level = data.get(key, PersistentDataType.INTEGER);
                    if (level != null && level > 0) {
                        enchantments.put(key.getKey().substring(ENCHANTMENT_KEY_PREFIX.length()), level);
                    }
                });

        return Collections.unmodifiableMap(enchantments);
    }

    /**
     * Reads a standard AdvancedEnchantments book.
     *
     * @param item the possible custom-enchantment book
     *
     * @return the book data, or {@code null} when the item is not an AE book
     */
    public @Nullable EnchantmentBook getEnchantmentBook(@Nullable ItemStack item) {
        if (!isAvailable()
                || item == null
                || item.getType() != Material.ENCHANTED_BOOK
                || !item.hasItemMeta()) {
            return null;
        }

        PersistentDataContainer data = item.getItemMeta().getPersistentDataContainer();
        String enchantment = data.get(BOOK_KEY, PersistentDataType.STRING);
        Integer level = data.get(BOOK_LEVEL_KEY, PersistentDataType.INTEGER);

        if (enchantment == null || enchantment.isBlank() || level == null || level <= 0) {
            return null;
        }

        Integer successChance = data.get(BOOK_SUCCESS_KEY, PersistentDataType.INTEGER);
        Integer destroyChance = data.get(BOOK_FAILURE_KEY, PersistentDataType.INTEGER);
        return new EnchantmentBook(
                normalizeId(enchantment),
                level,
                chanceOrDefault(successChance, 100),
                chanceOrDefault(destroyChance, 0));
    }

    /**
     * Creates a standard AE book with guaranteed application and no destroy chance.
     *
     * @param enchantment the AE enchantment identifier
     * @param level the enchantment level
     *
     * @return a functional AE book, or {@code null} if the integration was disabled
     */
    @ParametersAreNonnullByDefault
    public @Nullable ItemStack createEnchantmentBook(String enchantment, int level) {
        return createEnchantmentBook(new EnchantmentBook(normalizeId(enchantment), level, 100, 0));
    }

    /**
     * Creates a standard AE book using AdvancedEnchantments' persistent item-data format.
     *
     * @param book the book data
     *
     * @return a functional AE book, or {@code null} if the integration was disabled
     */
    public @Nullable ItemStack createEnchantmentBook(@Nonnull EnchantmentBook book) {
        if (!isAvailable()) {
            return null;
        }

        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        data.set(BOOK_KEY, PersistentDataType.STRING, book.enchantment());
        data.set(BOOK_LEVEL_KEY, PersistentDataType.INTEGER, book.level());
        data.set(BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, book.successChance());
        data.set(BOOK_FAILURE_KEY, PersistentDataType.INTEGER, book.destroyChance());

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + formatName(book.enchantment()) + ' '
                + formatLevel(book.level()));
        meta.setLore(List.of(
                ChatColor.GREEN + Integer.toString(book.successChance()) + "% Success Rate",
                ChatColor.RED + Integer.toString(book.destroyChance()) + "% Destroy Rate",
                ChatColor.GRAY + "Advanced Enchantment",
                ChatColor.GRAY + "Drag n' drop onto item to enchant"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Applies all requested custom enchantments to a clone through AEAPI. A failure returns {@code null}, leaving the
     * original item and machine inputs untouched.
     *
     * @param item the item to enchant
     * @param enchantments custom enchantment IDs and levels
     *
     * @return the modified clone, or {@code null} if AE rejected or failed to apply an enchantment
     */
    @ParametersAreNonnullByDefault
    public @Nullable ItemStack applyEnchantments(ItemStack item, Map<String, Integer> enchantments) {
        if (!isAvailable() || enchantments.isEmpty()) {
            return null;
        }

        ItemStack result = item.clone();

        try {
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                result = invokeApplyEnchant(entry.getKey(), entry.getValue(), result);
            }

            Map<String, Integer> applied = getEnchantments(result);
            for (Map.Entry<String, Integer> entry : enchantments.entrySet()) {
                if (applied.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                    throw new IllegalStateException(
                            "AdvancedEnchantments did not apply " + entry.getKey() + " level " + entry.getValue());
                }
            }

            return organize(result);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError x) {
            logApiFailure("apply custom enchantments", x);
            return null;
        }
    }

    /**
     * Removes requested custom enchantments from a clone. AEAPI is preferred; a verified namespaced-data fallback is
     * used only when AE can still reorganize the item's lore.
     *
     * @param item the item to clean
     * @param enchantments custom enchantment IDs to remove
     *
     * @return the cleaned clone, or {@code null} if removal could not be verified safely
     */
    @ParametersAreNonnullByDefault
    public @Nullable ItemStack removeEnchantments(ItemStack item, Map<String, Integer> enchantments) {
        if (!isAvailable() || enchantments.isEmpty()) {
            return null;
        }

        ItemStack result = item.clone();

        try {
            for (String enchantment : enchantments.keySet()) {
                if (removeEnchantment != null) {
                    result = invokeRemoveEnchant(enchantment, result);
                } else {
                    if (organizeEnchants == null) {
                        throw new NoSuchMethodException(
                                "AEAPI.removeEnchantment and AEAPI.organizeEnchants are both unavailable");
                    }
                    removeEnchantmentData(result, enchantment);
                }
            }

            result = organize(result);

            Map<String, Integer> remaining = getEnchantments(result);
            for (String enchantment : enchantments.keySet()) {
                if (remaining.containsKey(enchantment)) {
                    throw new IllegalStateException("AdvancedEnchantments did not remove " + enchantment);
                }
            }

            return result;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError x) {
            logApiFailure("remove custom enchantments", x);
            return null;
        }
    }

    private boolean isAvailable() {
        return plugin.isEnabled();
    }

    private @Nonnull ItemStack invokeApplyEnchant(String enchantment, int level, ItemStack item)
            throws ReflectiveOperationException {
        if (applyEnchant == null || applyEnchantOrder == null) {
            throw new NoSuchMethodException("AEAPI.applyEnchant is unavailable");
        }

        Object result;
        if (applyEnchantOrder == ApplyEnchantOrder.NAME_LEVEL_ITEM) {
            result = invoke(applyEnchant, enchantment, level, item);
        } else {
            result = invoke(applyEnchant, item, enchantment, level);
        }

        if (result instanceof ItemStack itemStack) {
            return itemStack;
        }

        throw new IllegalStateException("AEAPI.applyEnchant did not return an ItemStack");
    }

    private @Nonnull ItemStack invokeRemoveEnchant(String enchantment, ItemStack item)
            throws ReflectiveOperationException {
        if (removeEnchantment == null || removeEnchantOrder == null) {
            throw new NoSuchMethodException("AEAPI.removeEnchantment is unavailable");
        }

        Object result;
        if (removeEnchantOrder == RemoveEnchantOrder.ITEM_NAME) {
            result = invoke(removeEnchantment, item, enchantment);
        } else {
            result = invoke(removeEnchantment, enchantment, item);
        }

        if (result == null) {
            return item;
        }
        if (result instanceof ItemStack itemStack) {
            return itemStack;
        }

        throw new IllegalStateException("AEAPI.removeEnchantment returned an unsupported type");
    }

    private @Nonnull ItemStack organize(ItemStack item) throws ReflectiveOperationException {
        if (organizeEnchants == null) {
            return item;
        }

        Object result = invoke(organizeEnchants, item);
        if (result == null) {
            return item;
        }
        if (result instanceof ItemStack itemStack) {
            return itemStack;
        }

        throw new IllegalStateException("AEAPI.organizeEnchants returned an unsupported type");
    }

    private static void removeEnchantmentData(ItemStack item, String enchantment) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.getKeys().stream()
                .filter(AdvancedEnchantmentsIntegration::isEnchantmentKey)
                .filter(key -> key.getKey().substring(ENCHANTMENT_KEY_PREFIX.length()).equals(enchantment))
                .toList()
                .forEach(data::remove);
        item.setItemMeta(meta);
    }

    private static boolean isEnchantmentKey(NamespacedKey key) {
        return NAMESPACE.equals(key.getNamespace()) && key.getKey().startsWith(ENCHANTMENT_KEY_PREFIX);
    }

    private static @Nullable Class<?> loadApiClass(Plugin advancedEnchantments) {
        try {
            ClassLoader classLoader = advancedEnchantments.getClass().getClassLoader();
            return Class.forName(API_CLASS, true, classLoader);
        } catch (ClassNotFoundException | LinkageError x) {
            Slimefun.logger().log(Level.WARNING, x, () -> "Could not load optional " + API_CLASS);
            return null;
        }
    }

    private static @Nullable Method findStaticMethod(
            @Nullable Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null) {
            return null;
        }

        try {
            Method method = type.getMethod(name, parameterTypes);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static @Nullable Object invoke(Method method, Object... arguments) throws ReflectiveOperationException {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException x) {
            Throwable cause = x.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw new IllegalStateException("AdvancedEnchantments API failed", error);
            }
            throw x;
        }
    }

    private void logApiFailure(String operation, Throwable throwable) {
        if (apiFailureLogged.compareAndSet(false, true)) {
            Slimefun.logger()
                    .log(
                            Level.WARNING,
                            throwable,
                            () -> PLUGIN_NAME + " compatibility could not " + operation
                                    + ". Inputs were left untouched; update AdvancedEnchantments or disable it.");
        }
    }

    private static NamespacedKey key(String value) {
        return Objects.requireNonNull(NamespacedKey.fromString(NAMESPACE + ':' + value));
    }

    private static String normalizeId(String enchantment) {
        return enchantment.trim().toLowerCase(Locale.ROOT);
    }

    private static int chanceOrDefault(@Nullable Integer chance, int defaultValue) {
        return chance == null ? defaultValue : Math.max(0, Math.min(100, chance));
    }

    private static String formatName(String enchantment) {
        StringBuilder result = new StringBuilder();
        for (String part : enchantment.split("[-_]")) {
            if (part.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private static String formatLevel(int level) {
        if (level <= 0 || level > 20) {
            return Integer.toString(level);
        }

        int remaining = level;
        StringBuilder result = new StringBuilder();
        int[] values = {10, 9, 5, 4, 1};
        String[] numerals = {"X", "IX", "V", "IV", "I"};
        for (int index = 0; index < values.length; index++) {
            while (remaining >= values[index]) {
                result.append(numerals[index]);
                remaining -= values[index];
            }
        }
        return result.toString();
    }

    /**
     * Immutable metadata carried by a standard AdvancedEnchantments book.
     *
     * @param enchantment the AE enchantment identifier
     * @param level the enchantment level
     * @param successChance the application success chance
     * @param destroyChance the item destroy chance
     */
    public record EnchantmentBook(String enchantment, int level, int successChance, int destroyChance) {

        public EnchantmentBook {
            Objects.requireNonNull(enchantment, "enchantment");
            if (enchantment.isBlank()) {
                throw new IllegalArgumentException("enchantment cannot be blank");
            }
            enchantment = normalizeId(enchantment);
            if (level <= 0) {
                throw new IllegalArgumentException("level must be positive");
            }
            if (successChance < 0 || successChance > 100) {
                throw new IllegalArgumentException("successChance must be between 0 and 100");
            }
            if (destroyChance < 0 || destroyChance > 100) {
                throw new IllegalArgumentException("destroyChance must be between 0 and 100");
            }
        }
    }

    private enum ApplyEnchantOrder {
        NAME_LEVEL_ITEM,
        ITEM_NAME_LEVEL
    }

    private enum RemoveEnchantOrder {
        ITEM_NAME,
        NAME_ITEM
    }
}
