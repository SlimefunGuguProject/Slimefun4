package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import city.norain.slimefun4.utils.StringUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.StorageType;
import io.github.thebusybiscuit.slimefun4.core.debug.Debug;
import io.github.thebusybiscuit.slimefun4.core.debug.TestCase;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class DataUtils {
    /**
     * 使用 {@link BukkitObjectOutputStream} 序列化 {@link ItemStack}
     * 为 Base64 字符串，用于数据库存储.
     *
     * @param itemStack 要序列化的 {@link ItemStack}
     * @return 序列化后的 Base64 字符串
     */
    public static String serializeItemStack(ItemStack itemStack) {
        Debug.log(TestCase.BACKPACK, "Serializing itemstack: " + itemStack);

        if (itemStack == null) {
            return "";
        }

        try (var stream = new ByteArrayOutputStream();
                var bs = new BukkitObjectOutputStream(stream)) {
            bs.writeObject(itemStack);
            var itemStr = Base64.getEncoder().encodeToString(stream.toByteArray());

            if (!Slimefun.getConfigManager().isBypassItemLengthCheck()
                    && Slimefun.getDatabaseManager().getBlockDataStorageType() == StorageType.MYSQL
                    && itemStr.length() > 65535) {

                throw new IllegalArgumentException("检测到过大物品, 请联系物品对应插件开发者解决: " + StringUtil.itemStackToString(itemStack)
                        + ", size = " + itemStr.length());
            }

            return itemStr;
        } catch (Throwable e) {
            Slimefun.logger().log(Level.SEVERE, "序列化物品时出现错误, 将存储空值", e);
            return "";
        }
    }

    /**
     * 使用 {@link BukkitObjectInputStream} 反序列化 Base64 字符串
     * 为 {@link ItemStack} 对象.
     *
     * @param base64Str 要反序列化的 Base64 字符串
     * @return 反序列化后的 {@link ItemStack} 对象
     */
    @Nullable public static ItemStack deserializeItemStack(String base64Str) {
        if (base64Str == null || base64Str.isEmpty() || base64Str.isBlank()) {
            return null;
        }

        Debug.log(TestCase.BACKPACK, "Deserializing itemstack: " + base64Str);

        try (var stream = new ByteArrayInputStream(Base64.getMimeDecoder().decode(base64Str));
                var bs = new BukkitObjectInputStream(stream)) {
            var result = (ItemStack) bs.readObject();

            Debug.log(TestCase.BACKPACK, "Deserialized itemstack: " + result);

            if (result.getType().isAir()) {
                Slimefun.logger().log(Level.SEVERE, "反序列化数据库中的物品失败! 对应物品无法显示.");
            }

            return result;
        } catch (Exception ex) {
            throw new RuntimeException("反序列化物品时出现错误, 对应物品无法显示", ex);
        }
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
