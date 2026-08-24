package io.github.thebusybiscuit.slimefun4.core.commands;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.generator.WorldInfo;

class SlimefunTabCompleter implements TabCompleter {

    private static final int MAX_SUGGESTIONS = 80;

    private final SlimefunCommand command;

    public SlimefunTabCompleter(@Nonnull SlimefunCommand command) {
        this.command = command;
    }

    @Nullable @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return createReturnList(command.getSubCommandNames(), args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("banitem")) {
                return createReturnList(getSlimefunItems(), args[1]);
            } else if (args[0].equalsIgnoreCase("unbanitem")) {
                List<String> list = Slimefun.getRegistry().getDisabledSlimefunItems().stream()
                        .map(SlimefunItem::getId)
                        .collect(Collectors.toList());
                return createReturnList(list, args[1]);
            } else if (args[0].equalsIgnoreCase("cleardata")) {
                List<String> list = new ArrayList<>(
                        Bukkit.getWorlds().stream().map(WorldInfo::getName).toList());
                list.add("*");
                return createReturnList(list, args[1]);
            } else if (args[0].equalsIgnoreCase("tick")) {
                return createReturnList(List.of("at", "freeze", "query", "rate", "show", "unfreeze"), args[1]);
            }
            return null;
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                return createReturnList(getSlimefunItems(), args[2]);
            } else if (args[0].equalsIgnoreCase("research")) {
                List<Research> researches = Slimefun.getRegistry().getResearches();
                List<String> suggestions = new LinkedList<>();

                suggestions.add("all");
                suggestions.add("reset");

                for (Research research : researches) {
                    suggestions.add(research.getKey().toString().toLowerCase(Locale.ROOT));
                }

                return createReturnList(suggestions, args[2]);
            } else if (args[0].equalsIgnoreCase("cleardata")) {
                return createReturnList(List.of("block", "oil", "*"), args[2]);
            } else {
                // Returning null will make it fallback to the default arguments (all online players)
                return null;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            return createReturnList(Arrays.asList("1", "2", "4", "8", "16", "32", "64"), args[3]);
        } else {
            // Returning null will make it fallback to the default arguments (all online players)
            return null;
        }
    }

    /***
     * Returns a sublist from a given list containing items that start with the given string if string is not empty
     *
     * @param list
     *            The list to process
     * @param string
     *            The typed string
     * @return Sublist if string is not empty
     */
    @Nonnull
    private List<String> createReturnList(@Nonnull List<String> list, @Nonnull String string) {
        if (string.isEmpty()) {
            if (list.size() >= MAX_SUGGESTIONS) {
                return list.subList(0, MAX_SUGGESTIONS);
            } else {
                return list;
            }
        }

        String input = string.toLowerCase(Locale.ROOT);
        List<String> returnList = new LinkedList<>();

        for (String item : list) {
            if (item.toLowerCase(Locale.ROOT).contains(input)) {
                returnList.add(item);

                if (returnList.size() >= MAX_SUGGESTIONS) {
                    break;
                }
            } else if (item.equalsIgnoreCase(input)) {
                return Collections.emptyList();
            }
        }

        return returnList;
    }

    @Nonnull
    private List<String> getSlimefunItems() {
        List<SlimefunItem> items = Slimefun.getRegistry().getEnabledSlimefunItems();
        List<String> list = new ArrayList<>(items.size());

        for (SlimefunItem item : items) {
            list.add(item.getId());
        }

        return list;
    }
}
