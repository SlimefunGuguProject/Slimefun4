package city.norain.slimefun4;

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
        logger.log(Level.WARNING, "Incompatible plugins detected, Slimefun has been automatically disabled!");
        logger.log(Level.WARNING, "Incompatible plugin list: ", String.join(", ", plugins));
        logger.log(Level.WARNING, "These plugins appear here because they are incompatible with");
        logger.log(Level.WARNING, "the current Slimefun version or conflict with Slimefun.");
        logger.log(Level.WARNING, "If you believe these plugins can coexist with Slimefun,");
        logger.log(Level.WARNING, "please contact us to update the check.");
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
        Slimefun.getSchedulerService().runLater(() -> {
            if (Bukkit.getPluginManager().getPlugin("SlimeGlue") == null) {
                sf.getLogger()
                        .log(
                                Level.WARNING,
                                "SlimeGlue (slime adhesive) is not installed; additional protection checks for some plugins will be unavailable.");
                sf.getLogger().log(Level.WARNING, "Download: https://github.com/Xzavier0722/SlimeGlue");
            }
        }, 300L); // 15s
    }

    private static void printBorder(@Nonnull Logger logger) {
        logger.log(Level.WARNING, "#######################################################");
    }
}
