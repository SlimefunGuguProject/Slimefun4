package io.github.thebusybiscuit.slimefun4.integrations;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.Method;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

/**
 * This handles CoreProtect API calls that are not represented by Dough's protection logger API.
 */
final class CoreProtectIntegration {

    private final Object api;
    private final Method logInteraction;

    CoreProtectIntegration(@Nonnull Plugin coreProtect) {
        try {
            api = coreProtect.getClass().getMethod("getAPI").invoke(coreProtect);
            logInteraction = api.getClass().getMethod("logInteraction", String.class, Location.class);
        } catch (ReflectiveOperationException | RuntimeException x) {
            throw new IllegalStateException("CoreProtect does not expose the interaction logging API", x);
        }
    }

    void logInteraction(@Nonnull OfflinePlayer player, @Nonnull Block block) {
        try {
            logInteraction.invoke(api, player.getName(), block.getLocation());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError x) {
            Slimefun.logger().log(Level.WARNING, x, () -> "Failed to log an interaction to CoreProtect");
        }
    }
}
