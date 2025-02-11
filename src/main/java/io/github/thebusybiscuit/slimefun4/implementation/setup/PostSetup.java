package io.github.thebusybiscuit.slimefun4.implementation.setup;

import com.molean.folia.adapter.Folia;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.GrindStone;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.MakeshiftSmeltery;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.OreCrusher;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.Smeltery;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.AContainer;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import net.guizhanss.slimefun4.utils.WikiUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public final class PostSetup {

    private PostSetup() {}

    public static void setupWiki() {
        Slimefun.logger().log(Level.INFO, "加载 Wiki 页面...");

        WikiUtils.setupJson(Slimefun.instance(), (page) -> page.replace("#", "?id="));
    }

    public static void loadItems() {
        Iterator<SlimefunItem> iterator =
                Slimefun.getRegistry().getEnabledSlimefunItems().iterator();

        while (iterator.hasNext()) {
            SlimefunItem item = iterator.next();

            if (item == null) {
                Slimefun.logger().log(Level.WARNING, "Removed bugged Item ('NULL?')");
                iterator.remove();
            } else {
                try {
                    item.load();
                } catch (Exception | LinkageError x) {
                    item.error("Failed to properly load this Item", x);
                }
            }
        }

        Folia.getPluginManager().ce(new SlimefunItemRegistryFinalizedEvent());

        loadOreGrinderRecipes();
        loadSmelteryRecipes();

        CommandSender sender = Bukkit.getConsoleSender();

        int total = Slimefun.getRegistry().getEnabledSlimefunItems().size();
        int slimefunOnly = countNonAddonItems();

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN
                + "######################### - Slimefun v"
                + Slimefun.getVersion()
                + " - #########################");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN
                + "成功加载了 "
                + total
                + " 个物品和 "
                + Slimefun.getRegistry().getResearches().size()
                + " 个研究");
        sender.sendMessage(ChatColor.GREEN
                + "( "
                + slimefunOnly
                + " 物品来自本体, "
                + (total - slimefunOnly)
                + " 个物品来自 "
                + Slimefun.getInstalledAddons().size()
                + " 扩展 )");
        sender.sendMessage("");

        sender.sendMessage("");
        sender.sendMessage(ChatColor.GREEN + " - 源码:      https://github.com/SlimefunGuguProject/Slimefun4");
        sender.sendMessage(ChatColor.GREEN + " - Bug 反馈:  https://github.com/SlimefunGuguProject/Slimefun4/issues");
        sender.sendMessage(ChatColor.GREEN + " - Folia 相关BUG反馈: @梓莎(QQ2719322893)");

        sender.sendMessage("");

        Slimefun.getItemCfg().save();
        Slimefun.getResearchCfg().save();
        Slimefun.getConfigManager().setAutoLoadingMode(true);
    }

    /**
     * This method counts the amount of {@link SlimefunItem SlimefunItems} registered
     * by Slimefun itself and not by any addons.
     *
     * @return The amount of {@link SlimefunItem SlimefunItems} added by Slimefun itself
     */
    private static int countNonAddonItems() {
        // @formatter:off
        return (int) Slimefun.getRegistry().getEnabledSlimefunItems().stream()
                .filter(item -> item.getAddon() instanceof Slimefun)
                .count();
        // @formatter:on
    }

    private static void loadOreGrinderRecipes() {
        List<ItemStack[]> grinderRecipes = new CopyOnWriteArrayList<>();

        GrindStone grinder = (GrindStone) SlimefunItems.GRIND_STONE.getItem();
        if (grinder != null) {
            ItemStack[] input = null;

            for (ItemStack[] recipe : grinder.getRecipes()) {
                if (input == null) {
                    input = recipe;
                } else {
                    if (input[0] != null && recipe[0] != null) {
                        grinderRecipes.add(new ItemStack[] {input[0], recipe[0]});
                    }

                    input = null;
                }
            }
        }

        OreCrusher crusher = (OreCrusher) SlimefunItems.ORE_CRUSHER.getItem();
        if (crusher != null) {
            ItemStack[] input = null;

            for (ItemStack[] recipe : crusher.getRecipes()) {
                if (input == null) {
                    input = recipe;
                } else {
                    if (input[0] != null && recipe[0] != null) {
                        grinderRecipes.add(new ItemStack[] {input[0], recipe[0]});
                    }

                    input = null;
                }
            }
        }

        // Favour 8 Cobblestone -> 1 Sand Recipe over 1 Cobblestone -> 1 Gravel Recipe
        Stream<ItemStack[]> stream = grinderRecipes.stream();

        if (!Slimefun.getCfg().getBoolean("options.legacy-ore-grinder")) {
            stream = stream.sorted((a, b) -> Integer.compare(b[0].getAmount(), a[0].getAmount()));
        }

        stream.forEach(recipe -> registerMachineRecipe(
                "ELECTRIC_ORE_GRINDER", 4, new ItemStack[] {recipe[0]}, new ItemStack[] {recipe[1]}));
    }

    private static void loadSmelteryRecipes() {
        Smeltery smeltery = (Smeltery) SlimefunItems.SMELTERY.getItem();

        if (smeltery != null && !smeltery.isDisabled()) {
            MakeshiftSmeltery makeshiftSmeltery = ((MakeshiftSmeltery) SlimefunItems.MAKESHIFT_SMELTERY.getItem());
            ItemStack[] input = null;

            for (ItemStack[] output : smeltery.getRecipes()) {
                if (input == null) {
                    input = output;
                } else {
                    if (input[0] != null && output[0] != null) {
                        addSmelteryRecipe(input, output, makeshiftSmeltery);
                    }

                    input = null;
                }
            }

            for (SlimefunItem item : Slimefun.getRegistry().getEnabledSlimefunItems()) {
                if (item instanceof AContainer machine) {
                    if (machine.getMachineIdentifier().equals("ELECTRIC_SMELTERY")) {
                        List<MachineRecipe> recipes = machine.getMachineRecipes();
                        Collections.sort(
                                recipes,
                                Comparator.comparingInt(recipe -> recipe == null ? 0 : -recipe.getInput().length));
                    }
                }
            }
        }
    }

    private static void addSmelteryRecipe(ItemStack[] input, ItemStack[] output, MakeshiftSmeltery makeshiftSmeltery) {
        List<ItemStack> ingredients = new CopyOnWriteArrayList<>();

        // Filter out 'null' items
        for (ItemStack item : input) {
            if (item != null) {
                ingredients.add(item);
            }
        }

        // We want to redirect Dust to Ingot Recipes
        if (ingredients.size() == 1 && isDust(ingredients.get(0))) {
            makeshiftSmeltery.addRecipe(new ItemStack[] {ingredients.get(0)}, output[0]);

            registerMachineRecipe(
                    "ELECTRIC_INGOT_FACTORY", 8, new ItemStack[] {ingredients.get(0)}, new ItemStack[] {output[0]});
            registerMachineRecipe(
                    "ELECTRIC_INGOT_PULVERIZER", 3, new ItemStack[] {output[0]}, new ItemStack[] {ingredients.get(0)});
        } else {
            registerMachineRecipe(
                    "ELECTRIC_SMELTERY", 12, ingredients.toArray(new ItemStack[0]), new ItemStack[] {output[0]});
        }
    }

    private static boolean isDust(@Nonnull ItemStack item) {
        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        return sfItem != null && sfItem.getId().endsWith("_DUST");
    }

    private static void registerMachineRecipe(String machine, int seconds, ItemStack[] input, ItemStack[] output) {
        for (SlimefunItem item : Slimefun.getRegistry().getEnabledSlimefunItems()) {
            if (item instanceof AContainer container
                    && container.getMachineIdentifier().equals(machine)) {
                container.registerRecipe(seconds, input, output);
            }
        }
    }
}
