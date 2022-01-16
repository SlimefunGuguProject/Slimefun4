package ren.natsuyuk1.utils;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.bakedlibs.dough.protection.ActionType;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.AndroidMineEvent;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.maxgamer.quickshop.api.QuickShopAPI;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 保护插件权限检查器
 *
 * @author StarWishsama
 */
public class IntegrationHelper implements Listener {

    private static final String RESIDENCE = "Residence";
    private static final String QUICKSHOP = "QuickShop";

    private static boolean resInstalled;
    private static boolean qsInstalled;
    private static Method qsMethod = null;
    private static Object shopAPI = null;
    private static Logger logger;

    public IntegrationHelper(@Nonnull Slimefun plugin) {
        resInstalled = plugin.getServer().getPluginManager().getPlugin(RESIDENCE) != null;
        qsInstalled = plugin.getServer().getPluginManager().getPlugin(QUICKSHOP) != null;
        logger = plugin.getLogger();

        if (!qsInstalled) {
            plugin.getLogger().log(Level.WARNING, "未检测到 Quickshop-Reremake, 相关功能将自动关闭");
        } else {
            registerQuickShop(plugin);
        }

        if (!resInstalled) {
            logger.log(Level.WARNING, "未检测到领地插件, 相关功能将自动关闭");
            return;
        }

        logger.log(Level.INFO, "检测到领地插件, 相关功能已开启");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onAndroidMine(AndroidMineEvent e) {
        if (e != null) {
            Player p = Bukkit.getPlayer(getOwnerFromJson(BlockStorage.getBlockInfoAsJson(e.getAndroid().getBlock())));
            Location androidLoc = e.getAndroid().getBlock().getLocation();
            String location = androidLoc.getWorld().getName() + " [" + androidLoc.getBlockX() + "," + androidLoc.getBlockY() + "," + androidLoc.getBlockZ() + "]";

            if (p == null) {
                logger.log(Level.FINE, "检测到在 " + location + " 有机器人尝试挖矿, 但无法获取其主人");
                return;
            }

            if (!checkPermission(p, e.getBlock(), Interaction.BREAK_BLOCK)) {
                e.setCancelled(true);
                Slimefun.getLocalization().sendMessage(p, "android.no-permission");
            }
        }
    }

    /**
     * 检查是否可以在领地内破坏/交互方块
     *
     * @param p      玩家
     * @param block  被破坏的方块
     * @param action 交互类型
     * @return 是否可以破坏
     */
    public static boolean checkPermission(OfflinePlayer p, Block block, Interaction action) {
        if (!resInstalled || block == null || p == null || !p.isOnline() || p.isOp()) {
            return true;
        }

        ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(block.getLocation());

        if (res != null) {
            if (res.getOwnerUUID() == p.getUniqueId()) {
                return true;
            }

            ResidencePermissions perms = res.getPermissions();

            if (perms != null) {
                Player online = p.getPlayer();

                if (action.getType() == ActionType.BLOCK && perms.playerHas(online, Flags.admin, FlagPermissions.FlagCombo.OnlyTrue)) {
                    return true;
                }

                switch (action) {
                    case BREAK_BLOCK:
                        return perms.playerHas(online, Flags.destroy, FlagPermissions.FlagCombo.OnlyTrue);
                    case INTERACT_BLOCK:
                        // 领地已支持 Slimefun
                        // 详见
                        // https://github.com/Zrips/Residence/blob/master/src/com/bekvon/bukkit/residence/slimeFun/SlimeFunResidenceModule.java
                        return perms.playerHas(online, Flags.container, FlagPermissions.FlagCombo.OnlyTrue);
                    case PLACE_BLOCK:
                        // move 是为了机器人而检查的, 防止机器人跑进别人领地然后还出不来
                        return perms.playerHas(online, Flags.place, FlagPermissions.FlagCombo.OnlyTrue)
                                || perms.playerHas(online, Flags.build, FlagPermissions.FlagCombo.OnlyTrue)
                                && perms.playerHas(online, Flags.move, FlagPermissions.FlagCombo.TrueOrNone);
                }
            }
        }
        return true;
    }

    public static UUID getOwnerFromJson(String json) {
        if (json != null) {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonNull()) {
                JsonObject object = element.getAsJsonObject();
                return UUID.fromString(object.get("owner").getAsString());
            }
        }
        return null;
    }

    public static boolean checkForQuickShop(@Nonnull Location l) {
        if (!qsInstalled) {
            return false;
        }

        if (qsMethod != null) {
            try {
                if (shopAPI == null) {
                    return false;
                }

                Object result = qsMethod.invoke(shopAPI, l);

                if (result instanceof Optional) {
                    return ((Optional<?>) result).isPresent();
                } else {
                    return result != null;
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                logger.log(Level.WARNING, "在获取箱子商店时出现了问题", e);
                return true;
            }
        }

        QuickShopAPI qs = (QuickShopAPI) Bukkit.getPluginManager().getPlugin("QuickShop");

        return qs.getShopManager().getShop(l) != null;
    }

    private static void registerQuickShop(@Nonnull Slimefun plugin) {
        String version = plugin.getServer().getPluginManager().getPlugin(QUICKSHOP).getDescription().getVersion();
        String[] splitVersion = version.split("-")[0].split("\\.");
        int major = Integer.parseInt(splitVersion[0]);
        int sub = Integer.parseInt(splitVersion[2]);
        int last = Integer.parseInt(splitVersion[3]);

        if (major < 5) {
            logger.warning("QuickShop 版本过低, 建议你更新到 5.0.0+!");

            try {
                Method shopAPIMethod = Class.forName("org.maxgamer.quickshop.QuickShopAPI").getDeclaredMethod("getShopAPI");
                shopAPIMethod.setAccessible(true);
                shopAPI = shopAPIMethod.invoke(null);

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                logger.log(Level.INFO, "无法接入 Quickshop-Reremake " + version + " , 请更新到最新版, 相关功能将自动关闭");
                qsInstalled = false;
            }

            if (sub >= 8 && last >= 2) {
                // For 5.0.0-
                try {
                    qsMethod = Class.forName("org.maxgamer.quickshop.api.ShopAPI").getDeclaredMethod("getShop", Location.class);
                    qsMethod.setAccessible(true);
                } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                    logger.log(Level.INFO, "无法接入 Quickshop-Reremake " + version + " , 请更新到最新版, 相关功能将自动关闭");
                    qsInstalled = false;
                }
            } else {
                // For 4.0.8-
                try {
                    qsMethod = Class.forName("org.maxgamer.quickshop.api.ShopAPI").getDeclaredMethod("getShopWithCaching", Location.class);
                    qsMethod.setAccessible(true);
                } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                    logger.log(Level.INFO, "无法接入 Quickshop-Reremake " + version + " , 请更新到最新版, 相关功能将自动关闭");
                    qsInstalled = false;
                }
            }
        }
    }
}
