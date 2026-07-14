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
    /**
     * 将 {@link ItemStack} 序列化为兼容旧 API 的 Base64 字符串。
     *
     * @param itemStack 要序列化的 {@link ItemStack}
     * @return Base64 编码的物品数据
     * @deprecated 请使用 {@link #serializeItemStackBytes(ItemStack)}，避免 Base64 带来的额外空间占用
     */
    @Deprecated
    public static String serializeItemStack(ItemStack itemStack) {
        var itemData = serializeItemStackBytes(itemStack);
        return itemData.length == 0 ? "" : Base64.getEncoder().encodeToString(itemData);
    }

    public static byte[] serializeItemStackBytes(ItemStack itemStack) {
        Debug.log(TestCase.BACKPACK, "Serializing itemstack: " + itemStack);

        if (itemStack == null) {
            return new byte[0];
        }

        try {
            var itemData = ItemStackDataCodec.serialize(itemStack);

            if (!Slimefun.getConfigManager().isBypassItemLengthCheck()
                    && Slimefun.getDatabaseManager().getBlockDataStorageType() == StorageType.MYSQL
                    && itemData.length > 16_777_215) {

                throw new IllegalArgumentException("检测到过大物品, 请联系物品对应插件开发者解决: " + StringUtil.itemStackToString(itemStack)
                        + ", size = " + itemData.length);
            }

            return itemData;
        } catch (Throwable e) {
            Slimefun.logger().log(Level.SEVERE, "序列化物品时出现错误, 将存储空值", e);
            return new byte[0];
        }
    }

    /**
     * 反序列化数据库中的 {@link ItemStack}。支持当前原生二进制格式和旧版 Bukkit 对象流格式。
     *
     * @param itemData 要反序列化的二进制数据
     * @return 反序列化后的 {@link ItemStack} 对象
     */
    @Nullable public static ItemStack deserializeItemStack(byte[] itemData) {
        if (itemData == null || itemData.length == 0) {
            return null;
        }

        Debug.log(TestCase.BACKPACK, "Deserializing itemstack: " + itemData.length + " bytes");

        try {
            var result = ItemStackDataCodec.deserialize(itemData);

            Debug.log(TestCase.BACKPACK, "Deserialized itemstack: " + result);

            if (result.getType().isAir()) {
                Slimefun.logger().log(Level.SEVERE, "反序列化数据库中的物品失败! 对应物品无法显示.");
            }

            return result;
        } catch (Exception ex) {
            throw new RuntimeException("反序列化物品时出现错误, 对应物品无法显示", ex);
        }
    }

    /**
     * 从兼容旧 API 的 Base64 字符串反序列化物品。
     *
     * @param base64Str Base64 编码的物品数据
     * @return 反序列化后的物品
     * @deprecated 请使用 {@link #deserializeItemStack(byte[])}
     */
    @Deprecated
    @Nullable public static ItemStack deserializeItemStack(String base64Str) {
        if (base64Str == null || base64Str.isBlank()) {
            return null;
        }

        var encodedData = base64Str.getBytes(StandardCharsets.US_ASCII);
        var decodedData = Base64.getMimeDecoder().decode(base64Str);
        return deserializeItemStack(ItemStackDataCodec.isCurrent(decodedData) ? decodedData : encodedData);
    }

    public static boolean isLegacyItemStack(byte[] serializedItemStack) {
        return serializedItemStack != null
                && serializedItemStack.length > 0
                && ItemStackDataCodec.isLegacy(serializedItemStack);
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
