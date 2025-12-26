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
import org.apache.logging.log4j.core.config.Configurator;

public final class SlimefunExtended {
    private static SlimefunMigrateListener migrateListener = new SlimefunMigrateListener();

    @Getter
    private static boolean databaseDebugMode = false;

    @Getter
    private static MinecraftVersion minecraftVersion;

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
        } catch (UnknownServerVersionException e) {
            sf.getLogger().log(Level.WARNING, "无法识别你正在使用的服务端版本 :(");
            return false;
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
