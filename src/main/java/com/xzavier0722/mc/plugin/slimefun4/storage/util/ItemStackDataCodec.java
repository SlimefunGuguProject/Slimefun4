package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;

public final class ItemStackDataCodec {
    private static final byte[] FORMAT_V2 = {'S', 'F', '2', 0};
    private static final Object LEGACY_DESERIALIZATION_LOCK = new Object();

    private ItemStackDataCodec() {}

    public static byte[] serialize(ItemStack itemStack) {
        var serializedItem = itemStack.serializeAsBytes();
        var itemData = Arrays.copyOf(FORMAT_V2, FORMAT_V2.length + serializedItem.length);
        System.arraycopy(serializedItem, 0, itemData, FORMAT_V2.length, serializedItem.length);
        return itemData;
    }

    public static ItemStack deserialize(byte[] itemData) throws Exception {
        if (isCurrent(itemData)) {
            return deserializeCurrent(Arrays.copyOfRange(itemData, FORMAT_V2.length, itemData.length));
        }

        var serializedObject = Base64.getMimeDecoder().decode(new String(itemData, StandardCharsets.US_ASCII));
        return deserializeLegacyWithCompatibility(serializedObject);
    }

    private static ItemStack deserializeCurrent(byte[] serializedItem) {
        try {
            return ItemStack.deserializeBytes(serializedItem);
        } catch (RuntimeException primaryFailure) {
            // Keep an explicit fallback through Paper's compatibility entry point. Paper's
            // implementation currently delegates ItemStack#deserializeBytes to this method,
            // but older/newer Paper implementations may provide different migration paths.
            try {
                return Bukkit.getUnsafe().deserializeItem(serializedItem);
            } catch (RuntimeException fallbackFailure) {
                primaryFailure.addSuppressed(fallbackFailure);
                throw primaryFailure;
            }
        }
    }

    public static boolean isLegacy(byte[] itemData) {
        return itemData != null && itemData.length > 0 && !isCurrent(itemData);
    }

    static boolean isCurrent(byte[] itemData) {
        return itemData != null
                && itemData.length >= FORMAT_V2.length
                && Arrays.equals(FORMAT_V2, Arrays.copyOf(itemData, FORMAT_V2.length));
    }

    private static ItemStack deserializeLegacy(byte[] serializedObject) throws Exception {
        try (var stream = new ByteArrayInputStream(serializedObject);
                var input = new BukkitObjectInputStream(stream)) {
            return (ItemStack) input.readObject();
        }
    }

    private static ItemStack deserializeLegacyWithCompatibility(byte[] serializedObject) throws Exception {
        synchronized (LEGACY_DESERIALIZATION_LOCK) {
            var alias = LegacyItemMetaDeserializer.ITEM_META_ALIAS;
            var original = ConfigurationSerialization.getClassByAlias(alias);
            if (original == null) {
                throw new IllegalStateException("Paper ItemMeta deserializer is not registered");
            }
            try {
                LegacyItemMetaDeserializer.setDelegate(original);
                ConfigurationSerialization.registerClass(LegacyItemMetaDeserializer.class, alias);
                return deserializeLegacy(serializedObject);
            } finally {
                ConfigurationSerialization.unregisterClass(alias);
                ConfigurationSerialization.registerClass(original, alias);
            }
        }
    }
}
