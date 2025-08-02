package io.github.thebusybiscuit.slimefun4.utils.compatibility;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import org.bukkit.block.Biome;

/**
 * Biome 多版本兼容
 *
 * @author ybw0014
 */
public final class VersionedBiome {

    private VersionedBiome() {
        // utility class
    }

    private static final Method VALUE_OF_METHOD;

    static {
        Method valueOfMethod = null;

        try {
            valueOfMethod = Biome.class.getMethod("valueOf", String.class);
        } catch (Exception e) {
            Slimefun.logger().severe("初始化 Biome 多版本兼容失败：" + e.getMessage());
        }

        VALUE_OF_METHOD = valueOfMethod;
    }

    @Nonnull
    public static Biome valueOf(@Nonnull String biomeName) throws IllegalArgumentException {
        if (biomeName == null || biomeName.isEmpty()) {
            throw new IllegalArgumentException("Biome 名称不能为空");
        }
        try {
            if (VALUE_OF_METHOD != null) {
                return (Biome) VALUE_OF_METHOD.invoke(null, biomeName.toUpperCase());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("无法调用 Biome.valueOf: " + biomeName, e);
        }
        throw new IllegalArgumentException("Biome.valueOf 方法不可用");
    }
}
