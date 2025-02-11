package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import com.molean.folia.adapter.Folia;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class ClearDataCommand extends SubCommand {
    public static final List<String> ValidClearTypes = List.of("block", "oil");

    @ParametersAreNonnullByDefault
    public ClearDataCommand(Slimefun plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "cleardata", false);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (sender.hasPermission("slimefun.command.cleardata") || sender instanceof ConsoleCommandSender) {
            if (args.length == 4 && args[3].equalsIgnoreCase("confirm")) {
                List<World> worlds = new CopyOnWriteArrayList<>();
                List<String> clearTypes = new CopyOnWriteArrayList<>();
                String block = Slimefun.getLocalization().getMessage("commands.cleardata.block");
                String oil = Slimefun.getLocalization().getMessage("commands.cleardata.oil");
                BlockDataController controller = Slimefun.getDatabaseManager().getBlockDataController();
                if (args[1].equals("*")) {
                    worlds.addAll(Bukkit.getWorlds());
                } else {
                    World toAdd = Bukkit.getWorld(args[1]);
                    if (toAdd == null) {
                        Slimefun.getLocalization().sendMessage(sender, "commands.cleardata.worldNotFound", true);
                        return;
                    }
                }

                if (args[2].equals("*")) {
                    clearTypes.addAll(ValidClearTypes);
                } else if (ValidClearTypes.contains(args[2])) {
                    clearTypes.add(args[2]);
                }

                for (World world : worlds) {
                    for (String cleartype : clearTypes) {
                        if (cleartype.equals("block")) {
                            controller.removeAllDataInWorldAsync(world, () -> Folia.getScheduler()
                                    .runTaskAsynchronously(plugin, () -> Slimefun.getLocalization()
                                            .sendMessage(sender, "commands.cleardata.success", true, msg -> msg.replace(
                                                            "{0}", world.getName())
                                                    .replace("{1}", block))));
                        } else if (cleartype.equals("oil")) {
                            GEOResource oilresource = null;
                            for (GEOResource resource :
                                    Slimefun.getRegistry().getGEOResources().values()) {
                                if (resource.getKey()
                                        .toString()
                                        .equals(new NamespacedKey(Slimefun.instance(), "oil").toString())) {
                                    oilresource = resource;
                                }
                            }
                            controller.removeFromAllChunkInWorldAsync(
                                    world, oilresource.getKey().toString().replace(":", "-"), () -> Folia.getScheduler()
                                            .runTaskAsynchronously(plugin, () -> Slimefun.getLocalization()
                                                    .sendMessage(
                                                            sender,
                                                            "commands.cleardata.success",
                                                            true,
                                                            msg -> msg.replace("{0}", world.getName())
                                                                    .replace("{1}", oil))));
                        }
                    }
                }
                return;
            } else if (args.length == 3) {
                Slimefun.getLocalization().sendMessage(sender, "commands.cleardata.confirm", true);
                return;
            }
            Slimefun.getLocalization()
                    .sendMessage(
                            sender,
                            "messages.usage",
                            true,
                            msg -> msg.replace("%usage%", "/sf cleardata <World> <Type>"));
        } else {
            Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
        }
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "commands.cleardata.description";
    }
}
