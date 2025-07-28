package city.norain.slimefun4.utils;

import city.norain.slimefun4.SlimefunExtended;
import lombok.experimental.UtilityClass;
import org.bukkit.OfflinePlayer;

@UtilityClass
public class PlayerUtil {
    public boolean isConnected(OfflinePlayer player) {
        if (SlimefunExtended.getMinecraftVersion().isAtLeast(1, 20)) {
            return player.isConnected();
        } else {
            return player.isOnline();
        }
    }
}
