package io.github.thebusybiscuit.slimefun4.implementation;

import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This class stores some startup warnings we occasionally need to print.
 * If you setup your server the recommended way, you are never going to see
 * any of these messages.
 *
 * @author TheBusyBiscuit
 *
 */
final class StartupWarnings {

    private static final String BORDER = "****************************************************";
    private static final String PREFIX = "* ";

    private StartupWarnings() {}

    @ParametersAreNonnullByDefault
    static void discourageCSCoreLib(Logger logger) {
        logger.log(Level.SEVERE, BORDER);
        logger.log(Level.SEVERE, PREFIX + "你好像安装了 CS-CoreLib。");
        logger.log(Level.SEVERE, PREFIX);
        logger.log(Level.SEVERE, PREFIX + "自 2021/01/30 起就不再强制依赖 CS-CoreLib 了");
        logger.log(Level.SEVERE, PREFIX + "你需要卸载 CS-CoreLib 才能让 Slimefun 正常运行。");
        logger.log(Level.SEVERE, BORDER);
    }

    @ParametersAreNonnullByDefault
    static void invalidMinecraftVersion(Logger logger, String detectedVer, String slimefunVersion) {
        logger.log(Level.SEVERE, BORDER);
        logger.log(Level.SEVERE, PREFIX + "Slimefun 加载失败!");
        logger.log(Level.SEVERE, PREFIX + "你正在使用不支持的 Minecraft 版本!");
        logger.log(Level.SEVERE, PREFIX);
        logger.log(Level.SEVERE, PREFIX + "你正在使用 Minecraft {0}", detectedVer);
        logger.log(Level.SEVERE, PREFIX + "但 Slimefun {0} 只支持以下版本:", slimefunVersion);
        logger.log(Level.SEVERE, PREFIX + "Minecraft {0}", String.join(" / ", Slimefun.getSupportedVersions()));
        logger.log(Level.SEVERE, BORDER);
    }

    @ParametersAreNonnullByDefault
    static void invalidServerSoftware(Logger logger) {
        logger.log(Level.SEVERE, BORDER);
        logger.log(Level.SEVERE, PREFIX + "Slimefun 加载失败!");
        logger.log(Level.SEVERE, PREFIX + "我们不再支持 CraftBukkit 服务端了!");
        logger.log(Level.SEVERE, PREFIX);
        logger.log(Level.SEVERE, PREFIX + "你需要使用 Paper 或其分支的服务端");
        logger.log(Level.SEVERE, PREFIX + "(我们推荐 Paper)");
        logger.log(Level.SEVERE, BORDER);
    }

    @ParametersAreNonnullByDefault
    static void oldJavaVersion(Logger logger, int recommendedJavaVersion) {
        int javaVersion = NumberUtils.getJavaVersion();

        logger.log(Level.WARNING, BORDER);
        logger.log(Level.WARNING, PREFIX + "正在使用的 Java 版本 (Java {0}) 已过时.", javaVersion);
        logger.log(Level.WARNING, PREFIX);
        logger.log(Level.WARNING, PREFIX + "由于高版本 Minecraft 对 Java {0} 的强制依赖,", recommendedJavaVersion);
        logger.log(Level.WARNING, PREFIX + "我们推荐您尽快升级到 Java {0}.", recommendedJavaVersion);
        logger.log(Level.WARNING, PREFIX + "同时，为尽快使用到新版本 Java 带来的特性,");
        logger.log(Level.WARNING, PREFIX + "Slimefun 也会在不久的将来依赖于 Java {0}.", recommendedJavaVersion);
        logger.log(Level.WARNING, PREFIX + "为了不影响您以后的正常使用，请尽快更新!");
        logger.log(Level.WARNING, BORDER);
    }
}
