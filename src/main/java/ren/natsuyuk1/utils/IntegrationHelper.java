package ren.natsuyuk1.utils;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.thebusybiscuit.cscorelib2.protection.ProtectableAction;
import io.github.thebusybiscuit.slimefun4.api.events.AndroidMineEvent;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
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
  private static Logger logger;

  public IntegrationHelper(@Nonnull SlimefunPlugin plugin) {
    resInstalled = plugin.getServer().getPluginManager().getPlugin(RESIDENCE) != null;
    qsInstalled = plugin.getServer().getPluginManager().getPlugin(QUICKSHOP) != null;
    logger = plugin.getLogger();

    if (!qsInstalled) {
      plugin.getLogger().log(Level.WARNING, "未检测到 Quickshop-Reremake, 相关功能将自动关闭");
    } else {
      String[] version = plugin.getServer().getPluginManager().getPlugin(QUICKSHOP).getDescription().getVersion().split("\\.");
      int major = Integer.parseInt(version[2]);

      if (major < 8 || (major == 8 && Integer.parseInt(version[3]) < 2)) {
        try {
          qsMethod = Class.forName("org.maxgamer.quickshop.api.ShopAPI").getDeclaredMethod("getShopWithCaching", Location.class);
          qsMethod.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException ignored) {
          logger.log(Level.INFO, "无法接入 Quickshop-Reremake, 相关功能将自动关闭");
          qsInstalled = false;
        }
      }
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
      Player p =
              Bukkit.getPlayer(
                      getOwnerFromJson(BlockStorage.getBlockInfoAsJson(e.getAndroid().getBlock())));

      if (!checkPermission(p, e.getBlock(), ProtectableAction.BREAK_BLOCK)) {
        e.setCancelled(true);
        SlimefunPlugin.getLocalization().sendMessage(p, "android.no-permission");
      }
    }
  }

  /**
   * 检查是否可以在领地/地皮内破坏/交互方块
   *
   * @param p 玩家
   * @param block 被破坏的方块
   * @param action 交互类型
   * @return 是否可以破坏
   */
  public static boolean checkPermission(OfflinePlayer p, Block block, ProtectableAction action) {
    if (!resInstalled || p == null || !p.isOnline() || block == null || p.isOp()) {
      return true;
    }

    ClaimedResidence res =
        Residence.getInstance().getResidenceManager().getByLoc(block.getLocation());

    if (res != null) {
      if (res.getOwnerUUID() == p.getUniqueId()) {
        return true;
      }

      ResidencePermissions perms = res.getPermissions();

      if (perms != null) {
        Player online = p.getPlayer();

        if (!action.isBlockAction() || perms.playerHas(online, Flags.admin, FlagPermissions.FlagCombo.OnlyTrue)) {
          return true;
        }

        switch (action) {
          case BREAK_BLOCK:
          case INTERACT_BLOCK:
            // 领地已支持 Slimefun
            // 详见
            // https://github.com/Zrips/Residence/blob/master/src/com/bekvon/bukkit/residence/slimeFun/SlimeFunResidenceModule.java
            return true;
          case PLACE_BLOCK:
            // move 是为了机器人而检查的, 防止机器人跑进别人领地然后还出不来
            return perms.playerHas(online, Flags.place, FlagPermissions.FlagCombo.OnlyTrue)
                || perms.playerHas(online, Flags.build, FlagPermissions.FlagCombo.OnlyTrue)
                || !perms.playerHas(online, Flags.move, FlagPermissions.FlagCombo.OnlyTrue);
        }
      }
    }
    return true;
  }

  public static UUID getOwnerFromJson(String json) {
    if (json != null) {
      JsonElement element = new JsonParser().parse(json);
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
        return qsMethod.invoke(QuickShopAPI.getShopAPI(),l) != null;
      } catch (IllegalAccessException | InvocationTargetException e) {
        logger.log(Level.WARNING, "在获取箱子商店时出现了问题", e);
        return true;
      }
    }

    return QuickShopAPI.getShopAPI().getShop(l).isPresent();
  }
}
