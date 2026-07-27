package io.github.thebusybiscuit.slimefun4.core.commands.subcommands;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.commands.SlimefunCommand;
import io.github.thebusybiscuit.slimefun4.core.commands.SubCommand;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class ItemIdCommand extends SubCommand {
    protected ItemIdCommand(Slimefun plugin, SlimefunCommand cmd) {
        super(plugin, cmd, "id", false);
    }

    @Override
    public void onExecute(@Nonnull CommandSender sender, @Nonnull String[] args) {
        if (sender instanceof Player p) {
            if (sender.hasPermission("slimefun.command.id")) {
                var item = p.getInventory().getItemInMainHand();
                if (item.getType() != Material.AIR) {
                    var sfItem = SlimefunItem.getByItem(item);
                    if (sfItem != null) {
                        var sfId = sfItem.getId();
                        Component idComponent = Component.text(sfId, NamedTextColor.GRAY)
                                .decorate(TextDecoration.UNDERLINED)
                                .decorate(TextDecoration.ITALIC)
                                .hoverEvent(HoverEvent.showText(Component.text("Click to copy to clipboard")))
                                .clickEvent(ClickEvent.copyToClipboard(sfId));
                        sender.sendMessage(Component.text("The ID of this item is: ").append(idComponent));
                    } else {
                        Slimefun.getLocalization().sendMessage(sender, "messages.invalid-item-in-hand", true);
                    }
                } else {
                    sender.sendMessage(ChatColors.color("&bPlease hold the item you want to check in your main hand!"));
                }
            } else {
                Slimefun.getLocalization().sendMessage(sender, "messages.no-permission", true);
            }
        } else {
            Slimefun.getLocalization().sendMessage(sender, "messages.only-players", true);
        }
    }
}
