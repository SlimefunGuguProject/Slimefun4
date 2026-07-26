package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** Administrative health and recovery commands for Slimefun Legacy. */
final class StabilityCommand extends SubCommand {

    StabilityCommand(@Nonnull Slimefun plugin, @Nonnull SlimefunCommand cmd) {
        super(plugin, cmd, "stability", true);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (!sender.hasPermission("slimefun.command.stability")) {
            Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
            return;
        }

        String action = args.length > 1 ? args[1].toLowerCase() : "status";
        switch (action) {
            case "status" -> sendStatus(sender);
            case "retry" -> retryTarget(sender);
            case "retryall" -> {
                int count = Slimefun.getTickerTask().retryAllMachines();
                send(sender, "&aCleared the circuit breaker for &e" + count + " &apaused machine(s).");
            }
            default -> send(sender, "&eUsage: /sf stability [status|retry|retryall]");
        }
    }

    private void sendStatus(CommandSender sender) {
        send(sender, "&6Slimefun Legacy Stability Status");
        send(sender, "&7Previous clean shutdown: "
                + (Slimefun.getDatabaseManager().wasPreviousShutdownClean() ? "&aYes" : "&cNo"));
        send(sender, "&7Pending database writes: &e"
                + Slimefun.getDatabaseManager().getPendingWriteTaskCount());
        send(sender, "&7Paused machine circuits: &e"
                + Slimefun.getTickerTask().getPausedMachineCount());
        send(sender, "&7Ticker paused: &e" + Slimefun.getTickerTask().isPaused());
    }

    private void retryTarget(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            send(sender, "&cOnly a player can retry a targeted machine. Use /sf stability retryall from console.");
            return;
        }

        Block target = player.getTargetBlockExact(8, FluidCollisionMode.NEVER);
        if (target == null || target.getType().isAir()) {
            send(sender, "&cLook directly at a Slimefun machine within 8 blocks.");
            return;
        }

        if (Slimefun.getTickerTask().retryMachine(target.getLocation())) {
            send(sender, "&aThe targeted machine will be retried on its next ticker cycle.");
        } else {
            send(sender, "&eThe targeted machine was not paused by the circuit breaker.");
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(ChatColors.color(message));
    }
}
