package city.norain.slimefun4;

import com.molean.folia.adapter.Folia;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;

class EnvironmentChecker {
    private static final List<String> UNSUPPORTED_PLUGINS = List.of(
            "BedrockTechnology", "SlimefunFix", "SlimefunBugFixer", "Slimefunbookfix", "PlaceItemsOnGroundRebuilt");

    static boolean checkIncompatiblePlugins(@Nonnull Logger logger) {
        List<String> plugins = UNSUPPORTED_PLUGINS.stream()
                .filter(name -> Bukkit.getServer().getPluginManager().isPluginEnabled(name))
                .toList();

        if (plugins.isEmpty()) {
            return false;
        }

        printBorder(logger);
        logger.log(Level.WARNING, "");
        logger.log(Level.WARNING, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        logger.log(Level.WARNING, "检测到不兼容的插件, 已自动禁用 Slimefun!");
        logger.log(Level.WARNING, "不兼容插件列表: ", String.join(", ", plugins));
        logger.log(Level.WARNING, "这些插件出现在这里是因为它们已不兼容现有");
        logger.log(Level.WARNING, "Slimefun 版本或是与 Slimefun 冲突.");
        logger.log(Level.WARNING, "如果你觉得这些插件能够与 Slimefun 并存,");
        logger.log(Level.WARNING, "请联系我们修改.");
        logger.log(Level.WARNING, "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        logger.log(Level.WARNING, "");
        printBorder(logger);

        return true;
    }

    static boolean checkHybridServer() {

        try {
            Class.forName("cpw.mods.modlauncher.Launcher", false, ClassLoader.getSystemClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName(
                    "net.minecraftforge.server.console.TerminalHandler", false, ClassLoader.getSystemClassLoader());

            return true;
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("org.cardboardpowered.mixin.CardboardMixinPlugin", false, ClassLoader.getSystemClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("net.fabricmc.loader.impl.FabricLoaderImpl", false, ClassLoader.getSystemClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
        }

        if (Bukkit.getPluginCommand("mohist") != null) {
            return true;
        }

        var serverVer = Bukkit.getVersion().toLowerCase();

        return serverVer.contains("arclight") || serverVer.contains("mohist");
    }

    static void scheduleSlimeGlueCheck(@Nonnull Slimefun sf) {
        Folia.getScheduler()
                .runTaskLaterAsync(
                        sf,
                        () -> {
                            if (Bukkit.getPluginManager().getPlugin("SlimeGlue") == null) {
                                sf.getLogger().log(Level.WARNING, "检测到没有安装 SlimeGlue (粘液胶), 你将缺失对一些插件的额外保护检查!");
                                sf.getLogger().log(Level.WARNING, "下载: https://github.com/Xzavier0722/SlimeGlue");
                            }
                        },
                        300); // 15s
    }

    private static void printBorder(@Nonnull Logger logger) {
        logger.log(Level.WARNING, "#######################################################");
    }
}
