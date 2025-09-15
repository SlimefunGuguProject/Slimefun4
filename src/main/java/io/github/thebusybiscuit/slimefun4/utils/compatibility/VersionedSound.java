package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import com.google.common.base.Preconditions;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.Method;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;

/**
 * Sound 多版本兼容
 *
 * @author ybw0014
 */
public final class VersionedSound {

    private VersionedSound() {
        // utility class
    }

    private static final boolean IS_ENUM;
    private static final Method GET_KEY_METHOD;
    private static final Method VALUE_OF_METHOD;

    static {
        boolean isEnum = false;
        Method getKeyMethod = null;
        Method valueOfMethod = null;

        try {
            if (Sound.class.isEnum()) {
                isEnum = true;
            } else {
                getKeyMethod = Sound.class.getMethod("getKey");
            }
            valueOfMethod = Sound.class.getMethod("valueOf", String.class);
        } catch (Exception e) {
            Slimefun.logger().severe("无法确定 Sound 类型：" + e.getMessage());
        }

        IS_ENUM = isEnum;
        GET_KEY_METHOD = getKeyMethod;
        VALUE_OF_METHOD = valueOfMethod;
    }

    public static boolean isEnum() {
        return IS_ENUM;
    }

    /**
     * 获取 Sound 的名称
     *
     * @param sound Sound 对象
     * @return Sound 名称
     */
    @Nonnull
    public static String getSoundName(@Nonnull Sound sound) {
        try {
            if (isEnum()) {
                return ((Enum<?>) sound).name();
            } else if (GET_KEY_METHOD != null) {
                Object key = GET_KEY_METHOD.invoke(sound);
                if (!(key instanceof NamespacedKey nsKey)) {
                    throw new IllegalStateException("Sound.getKey() 返回的不是 NamespacedKey");
                }
                return nsKey.getKey();
            }
        } catch (Exception e) {
            Slimefun.logger().severe("获取 Sound 名称失败: " + e.getMessage());
        }

        // fallback
        return sound.toString();
    }

    /**
     * valueOf() 方法兼容
     *
     * @param name Sound 名称
     * @return Sound 对象
     * @throws IllegalArgumentException 名称无效
     */
    @Nonnull
    public static Sound valueOf(@Nonnull String name) throws IllegalArgumentException {
        Preconditions.checkArgument(name != null, "Sound 名称不能为空");

        String enumName = name.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        try {
            if (VALUE_OF_METHOD != null) {
                return (Sound) VALUE_OF_METHOD.invoke(null, enumName);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("无法调用 Sound.valueOf: " + name, e);
        }

        throw new IllegalArgumentException("Sound.valueOf 方法不可用");
    }
}
