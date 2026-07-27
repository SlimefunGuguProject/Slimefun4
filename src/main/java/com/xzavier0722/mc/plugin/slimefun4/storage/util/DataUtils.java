package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import city.norain.slimefun4.utils.StringUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.StorageType;
import io.github.thebusybiscuit.slimefun4.core.debug.Debug;
import io.github.thebusybiscuit.slimefun4.core.debug.TestCase;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;

public class DataUtils {
    private static final int MYSQL_MEDIUMBLOB_MAX_BYTES = 16_777_215;


    /**
     * Serializes an item using the legacy String-facing API.
     *
     * <p>This method remains for addon source and binary compatibility. New database code should
     * use {@link #serializeItemStackBytes(ItemStack)} so the native binary payload is not encoded a
     * second time.
     *
     * @param itemStack item to serialize
     * @return Base64 representation of the versioned binary payload
     * @deprecated use {@link #serializeItemStackBytes(ItemStack)} for storage
     */
    @Deprecated
    public static String serializeItemStack(ItemStack itemStack) {
        var data = serializeItemStackBytes(itemStack);
        return data.length == 0 ? "" : Base64.getEncoder().encodeToString(data);
    }

    /**
     * Serializes an item into Slimefun's versioned binary database format.
     *
     * @param itemStack item to serialize
     * @return versioned binary data, or an empty array for null/failed items
     */
    public static byte[] serializeItemStackBytes(ItemStack itemStack) {
        Debug.log(TestCase.BACKPACK, "Serializing itemstack: " + itemStack);

        if (itemStack == null) {
            return new byte[0];
        }

        try {
            var itemData = ItemStackDataCodec.serialize(itemStack);

            if (!Slimefun.getConfigManager().isBypassItemLengthCheck()
                    && Slimefun.getDatabaseManager().getBlockDataStorageType() == StorageType.MYSQL
                    && itemData.length > MYSQL_MEDIUMBLOB_MAX_BYTES) {
                throw new IllegalArgumentException(
                        "Detected an oversized item. Please contact the plugin developer responsible for that item: "
                                + StringUtil.itemStackToString(itemStack)
                                + ", size = "
                                + itemData.length);
            }

            return itemData;
        } catch (Throwable e) {
            Slimefun.logger()
                    .log(
                            Level.SEVERE,
                            "An error occurred while serializing an item; an empty value will be stored.",
                            e);
            return new byte[0];
        }
    }

    /**
     * Deserializes either the current binary format or a legacy ASCII Base64 payload.
     *
     * @param itemData stored item bytes
     * @return item, or null for an empty payload
     */
    @Nullable public static ItemStack deserializeItemStack(byte[] itemData) {
        if (itemData == null || itemData.length == 0) {
            return null;
        }

        try {
            var result = ItemStackDataCodec.deserialize(itemData);
            Debug.log(TestCase.BACKPACK, "Deserialized itemstack: " + result);

            if (result != null && result.getType().isAir()) {
                Slimefun.logger()
                        .log(
                                Level.SEVERE,
                                "Failed to deserialize an item from the database; the corresponding item cannot be displayed.");
            }

            return result;
        } catch (Exception ex) {
            throw new RuntimeException(
                    "An error occurred while deserializing an item; the corresponding item cannot be displayed.",
                    ex);
        }
    }

    /**
     * Deserializes data supplied through the legacy String-facing API.
     *
     * <p>Schema 1/2 values are raw Base64-encoded Bukkit object streams. Values created through the
     * retained String serializer are Base64-encoded versioned binary data. Both forms are accepted.
     *
     * @param base64Str encoded item data
     * @return item, or null for an empty value
     */
    @Deprecated
    @Nullable public static ItemStack deserializeItemStack(String base64Str) {
        if (base64Str == null || base64Str.isBlank()) {
            return null;
        }

        Debug.log(TestCase.BACKPACK, "Deserializing legacy string item data");
        var decoded = Base64.getMimeDecoder().decode(base64Str);
        return ItemStackDataCodec.isCurrent(decoded)
                ? deserializeItemStack(decoded)
                : deserializeItemStack(base64Str.getBytes(StandardCharsets.US_ASCII));
    }

    public static boolean isLegacyItemStack(byte[] data) {
        return ItemStackDataCodec.isLegacy(data);
    }

    public static String blockDataBase64(String text) {
        return Slimefun.getDatabaseManager().isBlockDataBase64Enabled() ? base64Encode(text) : text;
    }

    public static String blockDataDebase64(String base64Str) {
        return Slimefun.getDatabaseManager().isBlockDataBase64Enabled() ? base64Decode(base64Str) : base64Str;
    }

    public static String profileDataBase64(String text) {
        return Slimefun.getDatabaseManager().isProfileDataBase64Enabled() ? base64Encode(text) : text;
    }

    public static String profileDataDebase64(String base64Str) {
        return Slimefun.getDatabaseManager().isProfileDataBase64Enabled() ? base64Decode(base64Str) : base64Str;
    }

    public static String base64Encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String base64Decode(String base64Str) {
        return new String(Base64.getDecoder().decode(base64Str), StandardCharsets.UTF_8);
    }
}
