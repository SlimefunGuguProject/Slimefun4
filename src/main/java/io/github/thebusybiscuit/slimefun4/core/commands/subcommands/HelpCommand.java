package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import org.bukkit.command.CommandSender;

import javax.annotation.ParametersAreNonnullByDefault;

class HelpCommand extends SubCommand {

    @ParametersAreNonnullByDefault
    HelpCommand(SlimefunPlugin plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "help", false);
    }

    @Override
    public void onExecute(CommandSender sender, String[] args) {
        cmd.sendHelp(sender);
    }

}
