package io.github.starwishsama.utils;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.ResidencePermissions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.thebusybiscuit.cscorelib2.protection.ProtectableAction;
import io.github.thebusybiscuit.slimefun4.api.events.AndroidMineEvent;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.logging.Level;

/**
 * 保护插件权限检查器
 *
 * @author Nameless
 */
public class ProtectionChecker implements Listener {
    private static boolean resInstalled = false;

    @EventHandler
    public void onAndroidMine(AndroidMineEvent e) {
        if (e != null) {
            Player p = Bukkit.getPlayer(getOwnerFromJson(BlockStorage.getBlockInfoAsJson(e.getAndroid().getBlock())));

            if (!canInteract(p, e.getBlock(), ProtectableAction.BREAK_BLOCK)) {
                e.setCancelled(true);
                SlimefunPlugin.getLocalization().sendMessage(p, "android.no-permission");
            }
        }
    }

    public ProtectionChecker(SlimefunPlugin plugin) {
        resInstalled = plugin.getServer().getPluginManager().getPlugin("Residence") != null;

        if (!resInstalled) {
            plugin.getLogger().log(Level.WARNING, "未检测到领地插件, 相关功能将自动关闭");
            return;
        }

        plugin.getLogger().log(Level.INFO, "检测到领地插件, 相关功能已开启");

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 检查是否可以在领地/地皮内破坏/交互方块
     *
     * @param p      玩家
     * @param block  被破坏的方块
     * @param action 交互类型
     * @return 是否可以破坏
     */
    public static boolean canInteract(Player p, Block block, ProtectableAction action) {
        if (!resInstalled) {
            return true;
        }

        if (p == null || block == null) {
            return true;
        }

        if (p.isOp()) {
            return true;
        }

        ClaimedResidence res = Residence.getInstance().getResidenceManager().getByLoc(block.getLocation());

        if (res != null) {
            if (res.getOwnerUUID() == p.getUniqueId()) {
                return true;
            }

            ResidencePermissions perms = res.getPermissions();

            if (perms.playerHas(p, Flags.admin, true)) {
                return true;
            }

            if (!action.isBlockAction()) {
                return true;
            }

            switch (action) {
                case BREAK_BLOCK:
                    // 领地已支持 Slimefun
                    // 详情请见 https://github.com/Zrips/Residence/blob/d061c9d09c6e07b1dfe8b5ebf539bafa9f0aa61b/src/com/bekvon/bukkit/residence/slimeFun/SlimeFunResidenceModule.java
                    return SlimefunPlugin.getProtectionManager().hasPermission(p, block.getLocation(), action);
                case PLACE_BLOCK:
                    // move 是为了机器人而检查的, 防止机器人跑进别人领地然后还出不来
                    return perms.playerHas(p, Flags.place, true) || perms.playerHas(p, Flags.build, true) || !perms.playerHas(p, Flags.move, true);
                case INTERACT_BLOCK:
                    // 防止一些方法忘记修改导致绕过权限
                case ACCESS_INVENTORIES:
                    if (!perms.playerHas(p, Flags.use, true)) {
                        SlimefunPlugin.getLocalization().sendMessage(p, "inventory.no-access");
                        return false;
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
}
