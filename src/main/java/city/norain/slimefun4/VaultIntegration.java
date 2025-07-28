package city.norain.slimefun4;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Objects;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

/**
 * Vault 集成类, 用于处理游戏币相关的操作.
 *
 * @author StarWishsama
 */
public class VaultIntegration {
    private static Economy econ = null;

    protected static void register(@Nonnull Slimefun plugin) {
        if (plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            var rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                econ = rsp.getProvider();
                plugin.getLogger().log(Level.INFO, "成功接入 Vault");
            } else {
                plugin.getLogger().log(Level.WARNING, "无法接入 Vault. 如果你是 CMI 用户, 请至配置文件启用经济系统");
            }
        } else {
            plugin.getLogger().log(Level.WARNING, "服务端未安装 Vault, 游戏币解锁研究特性将无法使用");
        }
    }

    protected static void cleanup() {
        econ = null;
    }

    public static double getPlayerBalance(OfflinePlayer p) {
        Objects.requireNonNull(p, "Player cannot be null!");
        Objects.requireNonNull(econ, "Vault instance cannot be null!");

        return econ.getBalance(p);
    }

    public static void withdrawPlayer(OfflinePlayer p, double withdraw) {
        Objects.requireNonNull(p, "Player cannot be null!");
        Objects.requireNonNull(econ, "Vault instance cannot be null!");

        econ.withdrawPlayer(p, withdraw);
    }

    public static boolean isEnabled() {
        return econ != null && Slimefun.getConfigManager().isUseMoneyUnlock();
    }
}
