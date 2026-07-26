package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.core.services.stability.ItemDoctorReport;
import io.github.thebusybiscuit.slimefun4.core.services.stability.ItemDoctorService;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Locale;
import javax.annotation.Nonnull;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Administrative item presentation diagnosis and repair commands. */
final class DoctorCommand extends SubCommand {

    DoctorCommand(@Nonnull Slimefun plugin, @Nonnull SlimefunCommand cmd) {
        super(plugin, cmd, "doctor", true);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!sender.hasPermission("slimefun.command.doctor")) {
            Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
            return;
        }

        ItemDoctorService service = Slimefun.getItemDoctorService();
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "status";
        switch (action) {
            case "status" -> sendStatus(sender, service);
            case "hand" -> repairHand(sender, service);
            case "inventory" -> repairInventory(sender, args, service);
            case "scan" -> startServerRun(sender, service, false);
            case "repair", "fix" -> {
                if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                    send(sender, "&eThis safely changes visible names and lore across stored items.");
                    send(sender, "&eBack up the server first, then run &6/slimefun doctor repair confirm&e.");
                    return;
                }
                startServerRun(sender, service, true);
            }
            default -> sendUsage(sender);
        }
    }

    private void sendStatus(CommandSender sender, ItemDoctorService service) {
        send(sender, "&6Slimefun Storage and Item Doctor");
        send(sender, "&7Previous clean shutdown: "
                + (Slimefun.getDatabaseManager().wasPreviousShutdownClean() ? "&aYes" : "&cNo"));
        send(sender, "&7Pending database writes: &e"
                + Slimefun.getDatabaseManager().getPendingWriteTaskCount());
        send(sender, "&7Paused machine circuits: &e"
                + Slimefun.getTickerTask().getPausedMachineCount());
        send(sender, "&7Automatic item repair: " + (service.isEnabled() ? "&aEnabled" : "&cDisabled"));

        ItemDoctorReport automatic = service.getAutomaticReport();
        send(sender, "&7Automatic repairs completed: &e" + automatic.getRepairedStacks());

        ItemDoctorReport current = service.getCurrentReport();
        if (current != null) {
            send(sender, "&7Server-wide " + current.getModeName() + ": &eRunning");
            sendProgress(sender, current);
            return;
        }

        ItemDoctorReport last = service.getLastReport();
        if (last != null) {
            send(sender, "&7Last server-wide " + last.getModeName() + ": &aComplete");
            sendProgress(sender, last);
        } else {
            send(sender, "&7Server-wide scan: &fNot run yet");
        }
        sendUsage(sender);
    }

    private void repairHand(CommandSender sender, ItemDoctorService service) {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly a player can repair the item in their hand.");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        ItemDoctorReport report = service.inspectItem(item, true);
        if (report.getRepairedStacks() > 0) {
            send(sender, "&aRepaired the visible English name and lore while preserving item data.");
        } else if (report.getCjkStacks() == 0) {
            send(sender, "&eNo registered Slimefun item with Chinese presentation was found in your hand.");
        } else {
            send(sender, "&cThe item could not be mapped to an English registered template.");
            sendProgress(sender, report);
        }
    }

    private void repairInventory(CommandSender sender, String[] args, ItemDoctorService service) {
        Player target;
        if (args.length > 2) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                send(sender, "&cThat player is not online. Offline inventories repair automatically on next join.");
                return;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            send(sender, "&cConsole usage: /sf doctor inventory <online-player>");
            return;
        }

        ItemDoctorReport report = service.inspectPlayer(target, true);
        send(sender, "&aFinished repairing &e" + target.getName() + "&a's inventory and ender chest.");
        sendProgress(sender, report);
    }

    private void startServerRun(CommandSender sender, ItemDoctorService service, boolean repair) {
        if (!service.isEnabled()) {
            send(sender, "&cThe item doctor is disabled in config.yml.");
            return;
        }

        boolean started = service.startServerRun(repair, report -> {
            send(sender, "&aSlimefun item doctor " + report.getModeName() + " completed.");
            sendProgress(sender, report);
            if (report.getUnknownIds() > 0 || report.getUnresolvedTemplates() > 0) {
                send(sender, "&eSome items were skipped because no safe English registered template was available.");
            }
            if (report.isRepairMode()) {
                send(sender, "&eBackpack database changes are queued. Keep the server running until");
                send(sender, "&e/slimefun doctor status shows 0 pending database writes, then stop normally.");
            }
        });

        if (!started) {
            send(sender, "&eA server-wide item doctor run is already active. Use /sf doctor status.");
            return;
        }

        send(sender, "&aStarted a batched server-wide item doctor " + (repair ? "repair" : "scan") + '.');
        if (!repair) {
            send(sender, "&7This is a dry run. It will report changes without modifying any item.");
        }
        send(sender, "&7It covers online inventories, loaded chests/machines, nested containers, and all backpacks.");
        send(sender, "&7Offline player inventories and unloaded chests are repaired automatically when loaded.");
    }

    private void sendProgress(CommandSender sender, ItemDoctorReport report) {
        send(sender, "&7Inventories: &e" + report.getInventories() + " &8| &7Backpacks: &e" + report.getBackpacks());
        send(sender, "&7Stacks scanned: &e" + report.getScannedStacks() + " &8| &7Slimefun: &e"
                + report.getSlimefunStacks());
        send(sender, "&7Chinese presentation: &e" + report.getCjkStacks() + " &8| &7Repaired: &a"
                + report.getRepairedStacks());
        send(sender, "&7Unknown IDs: &e" + report.getUnknownIds() + " &8| &7No English template: &e"
                + report.getUnresolvedTemplates() + " &8| &7Failures: &c" + report.getFailures());
        if (!report.getUnknownIdSamples().isEmpty()) {
            send(sender, "&7Unknown ID samples: &e" + String.join(", ", report.getUnknownIdSamples()));
        }
        if (!report.getUnresolvedTemplateSamples().isEmpty()) {
            send(sender, "&7Unresolved template samples: &e"
                    + String.join(", ", report.getUnresolvedTemplateSamples()));
        }
        if (report.isComplete()) {
            send(sender, "&7Duration: &e" + Math.max(1L, report.getDurationMillis() / 1000L) + " second(s)");
        }
    }

    private void sendUsage(CommandSender sender) {
        send(sender, "&eUsage: /slimefun doctor [status|hand|inventory [player]|scan|repair confirm]");
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ChatColors.color(message));
    }
}
