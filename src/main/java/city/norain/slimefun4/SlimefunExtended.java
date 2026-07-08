package city.norain.slimefun4;

import city.norain.slimefun4.compatibillty.VersionedEvent;
import city.norain.slimefun4.listener.SlimefunMigrateListener;
import city.norain.slimefun4.utils.EnvUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import io.github.bakedlibs.dough.versions.MinecraftVersion;
import io.github.bakedlibs.dough.versions.UnknownServerVersionException;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import lombok.Getter;
import net.guizhanss.guizhanlib.minecraft.utils.MinecraftVersionUtil;
import org.apache.logging.log4j.core.config.Configurator;
import org.bukkit.Server;

public final class SlimefunExtended {
    private static SlimefunMigrateListener migrateListener = new SlimefunMigrateListener();

    @Getter
    private static boolean databaseDebugMode = false;

    @Deprecated(since = "2026.1.1", forRemoval = true)
    private static MinecraftVersion minecraftVersion;

    @Deprecated(since = "2026.1.1", forRemoval = true)
    public static MinecraftVersion getMinecraftVersion() {
        return minecraftVersion;
    }

    /**
     * 返回当前服务器的 Minecraft 版本详情，包含主版本号、次版本号和补丁版本号。
     * 例如：26.1.2 将返回 (26, 1, 2)，而 26.1 将返回 (26, 1, 0)。
     *
     * 当无法识别服务器版本时，返回 null。
     *
     * @since 2026.1
     * @param server
     * @return
     */
    public static ServerVersion getServerVerDetail(Server server) {
        String mcVersion = server.getMinecraftVersion();

        if (mcVersion.isBlank()) {
            return null;
        }

        // 提取版本号中的数字部分
        String[] versionPart = mcVersion.split("\\.");

        // 可能是快照版本或者是预发布版?
        if (versionPart.length < 2) {
            return null;
        }

        try {
            int majorVersion = Integer.parseInt(versionPart[0]);

            // 自 26.1 开始，Minecraft 版本号格式变为以年份作为主版本号
            if (majorVersion != 1 && majorVersion < 26) {
                return null;
            }

            int minorVersion = Integer.parseInt(versionPart[1]);
            int patchVersion = versionPart.length > 2 ? Integer.parseInt(versionPart[2]) : 0;
            return new ServerVersion(majorVersion, minorVersion, patchVersion);
        } catch (NumberFormatException e) {
            server.getLogger().log(Level.WARNING, "无法解析当前服务器版本号: " + mcVersion, e);
            return null;
        }
    }

    /**
     * @since 2026.1
     * @param major the major version number (e.g., 26 for Minecraft 26.1)
     * @param minor
     * @return
     */
    public static boolean isAtLeast(int major, int minor) {
        return MinecraftVersionUtil.isAtLeast(major, minor);
    }

    /**
     * @since 2026.1
     * @param major the major version number (e.g., 26 for Minecraft 26.1)
     * @param minor
     * @return
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        return MinecraftVersionUtil.isAtLeast(major, minor, patch);
    }

    private static void checkDebug() {
        if ("true".equals(System.getProperty("slimefun.database.debug"))) {
            databaseDebugMode = true;

            Slimefun.getSQLProfiler().start();
            Slimefun.logger().log(Level.INFO, "已启动数据库调试模式");
        } else {
            Configurator.setLevel(HikariConfig.class.getName(), org.apache.logging.log4j.Level.OFF);
            Configurator.setLevel(HikariDataSource.class.getName(), org.apache.logging.log4j.Level.OFF);
            Configurator.setLevel(HikariPool.class.getName(), org.apache.logging.log4j.Level.OFF);
        }
    }

    public static boolean checkEnvironment(@Nonnull Slimefun sf) {
        try {
            minecraftVersion = MinecraftVersion.of(sf.getServer());
        } catch (UnknownServerVersionException ignored) {
            // sf.getLogger().log(Level.WARNING, "无法识别你正在使用的服务端版本 :(");
            // return false;
        }

        if (EnvironmentChecker.checkHybridServer()) {
            sf.getLogger().log(Level.WARNING, "#######################################################");
            sf.getLogger().log(Level.WARNING, "");
            sf.getLogger().log(Level.WARNING, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            sf.getLogger().log(Level.WARNING, "检测到正在使用混合端, Slimefun 将会被禁用!");
            sf.getLogger().log(Level.WARNING, "混合端已被多个用户报告有使用问题,");
            sf.getLogger().log(Level.WARNING, "强制绕过检测将不受任何反馈支持.");
            sf.getLogger().log(Level.WARNING, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            sf.getLogger().log(Level.WARNING, "");
            sf.getLogger().log(Level.WARNING, "#######################################################");
            return false;
        }

        if (Slimefun.getConfigManager().isBypassEnvironmentCheck()) {
            sf.getLogger().log(Level.WARNING, "#######################################################");
            sf.getLogger().log(Level.WARNING, "");
            sf.getLogger().log(Level.WARNING, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            sf.getLogger().log(Level.WARNING, "检测到你禁用了环境兼容性检查!");
            sf.getLogger().log(Level.WARNING, "未通过兼容性检查将无法受到反馈支持.");
            sf.getLogger().log(Level.WARNING, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            sf.getLogger().log(Level.WARNING, "");
            sf.getLogger().log(Level.WARNING, "#######################################################");
            return true;
        } else {
            return !EnvironmentChecker.checkIncompatiblePlugins(sf.getLogger());
        }
    }

    public static void init(@Nonnull Slimefun sf) {
        EnvironmentChecker.scheduleSlimeGlueCheck(sf);

        EnvUtil.init();

        checkDebug();

        VaultIntegration.register(sf);

        migrateListener.register(sf);

        VersionedEvent.init();
    }

    public static void shutdown() {
        migrateListener = null;

        VaultIntegration.cleanup();

        databaseDebugMode = false;
    }
}
