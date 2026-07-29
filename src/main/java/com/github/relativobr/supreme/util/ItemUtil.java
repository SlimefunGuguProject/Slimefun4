package com.github.relativobr.supreme.util;

import com.github.relativobr.supreme.Supreme;
import com.github.relativobr.supreme.machine.AbstractQuarry;
import com.github.relativobr.supreme.machine.AbstractQuarryOutputItem;
import com.github.relativobr.supreme.machine.tech.MobTechGeneric;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactivity;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.github.relativobr.supreme.Supreme.getSupremeOptions;
import static com.github.relativobr.supreme.util.CompatibilySupremeLegacy.getNewIdSupremeLegacy;
import static com.github.relativobr.supreme.util.CompatibilySupremeLegacy.getOldIdSupremeLegacy;

public class ItemUtil {


    /** Applies the optional production limiter to generator output and buffers only. */
    public static int getValueGeneratorsWithLimit(int value) {
        return Math.min((getSupremeOptions().isLimitProductionGenerators() ? (value / 5) : value), 16000000);
    }

    /**
     * Capacitor capacity is storage, not generator production, and must never be reduced by the
     * production limiter or the historical 16 MJ generator ceiling.
     */
    public static int getCapacitorCapacity(int value) {
        return Math.max(1, value);
    }

    public static SupremeQuarryOutput getOutputQuarry(@Nonnull SlimefunItemStack item) {

        ConfigurationSection typeSection = Supreme.inst().getConfig()
                .getConfigurationSection("quarry-custom-output");

        if (typeSection == null) {
            Supreme.inst().log(Level.SEVERE,
                    "Config section \"quarry-custom-output\" missing, Check your config and report this!");
            return null;
        }

        // find path
        String itemPath = getNewIdSupremeLegacy(item.getItemId()).toLowerCase();
        ConfigurationSection itemSection = typeSection.getConfigurationSection(itemPath);

        List<AbstractQuarryOutputItem> outputItems = new ArrayList<>();
        if (itemSection != null) {
            int checkLimitChance = 0;
            for (int i = 1; i <= 12; i++) {
                ConfigurationSection itemConfig = itemSection.getConfigurationSection(String.valueOf(i));
                if (itemConfig == null || checkLimitChance >= 100) {
                    break;
                }
                int chance = itemConfig.getInt("chance");
                if (checkLimitChance + chance >= 100) {
                    chance = 100 - checkLimitChance;
                }
                final String itemId = itemConfig.getString("item");
                if (itemId == null) {
                    continue;
                }
                if (itemConfig.getBoolean("is-slimefun")) {
                    SlimefunItem slimefunItem = SlimefunItem.getById(itemId);
                    //check Legacy Supreme
                    if (slimefunItem == null && getSupremeOptions().isUseLegacySupremeexpansionItemId()) {
                        slimefunItem = SlimefunItem.getById(getOldIdSupremeLegacy(itemId));
                    }
                    if (slimefunItem != null) {
                        outputItems.add(
                                AbstractQuarryOutputItem.builder().itemStack(slimefunItem.getItem().clone())
                                        .chance(getSupremeOptions().isLimitProductionQuarry() ? (chance / 2) : chance)
                                        .build());
                    }
                } else {
                    final Material material = Material.matchMaterial(itemId);
                    if (material != null) {
                        outputItems.add(AbstractQuarryOutputItem.builder().itemStack(new ItemStack(material, 1))
                                .chance(getSupremeOptions().isLimitProductionQuarry() ? (chance / 2) : chance)
                                .build());
                    }
                }
                checkLimitChance = checkLimitChance + chance;
            }
        } else {
            Supreme.inst().log(Level.SEVERE,
                    "Config section for " + itemPath + " missing, Check your config and report this!");
        }

        return SupremeQuarryOutput.builder().outputItems(outputItems).build();
    }

    public static void addLoreQuarry(@Nonnull AbstractQuarry quarry) {
        ItemStack item = quarry.getItem();
        ItemMeta meta = item.getItemMeta();
        // lore
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        Optional<List<String>> lastElementLore = Optional.empty();
        if (meta.hasLore()) {
            lastElementLore = Optional.of(meta.getLore());
        }
        final SupremeQuarryOutput output = quarry.getOutput();
        final List<AbstractQuarryOutputItem> outputItems = output.getOutputItems().stream().filter(Objects::nonNull)
                .collect(Collectors.toList());
        for (AbstractQuarryOutputItem outputItem : outputItems) {
            if (outputItem != null && outputItem.getItemStack() != null && outputItem.getItemStack().getItemMeta() != null) {
                String name = outputItem.getItemStack().getType().name();
                if (outputItem.getItemStack().getItemMeta().hasDisplayName()) {
                    name = outputItem.getItemStack().getItemMeta().getDisplayName();
                } else {
                    name = name.replace("_", " ");
                    name = name.substring(0, 1).toUpperCase().concat(name.substring(1).toLowerCase());
                    name = ChatColor.AQUA + name;
                }
                lore.add(name + " " + ChatColor.YELLOW + outputItem.getChance() + "%");
            }
        }

        if (getSupremeOptions().isLimitProductionQuarry()) {
            lore.add(" ");
            lore.add(ChatColor.GOLD + "Limit Production Quarry");
        }

        lastElementLore.ifPresent(lore::addAll);
        meta.setLore(lore);
        // add meta
        item.setItemMeta(meta);
    }


    public static ItemStack getItemQuarry(SupremeQuarryOutput output, int randomInt) {
        AtomicInteger startValue = new AtomicInteger(0);
        AtomicInteger nextValue = new AtomicInteger(0);
        ItemStack item = null;
        final List<AbstractQuarryOutputItem> outputItems = output.getOutputItems().stream().filter(Objects::nonNull)
                .collect(Collectors.toList());
        for (AbstractQuarryOutputItem outputItem : outputItems) {
            nextValue.set(startValue.get() + outputItem.getChance());
            if (startValue.get() <= randomInt && nextValue.get() >= randomInt) {
                item = outputItem.getItemStack();
                break;
            }
            startValue.set(nextValue.get());
        }
        return item;
    }


    public static String buildNameTier(String newName, Integer tier) {
        switch (tier) {
            case 1:
                return ChatColor.GRAY + newName + " I";
            case 2:
                return ChatColor.DARK_GREEN + newName + " II";
            case 3:
                return ChatColor.GREEN + newName + " III";
            case 4:
                return ChatColor.DARK_BLUE + newName + " IV";
            case 5:
                return ChatColor.BLUE + newName + " V";
            case 6:
                return ChatColor.GOLD + newName + " VI";
            case 7:
                return ChatColor.YELLOW + newName + " VII";
            case 8:
                return ChatColor.DARK_RED + newName + " VIII";
            case 9:
                return ChatColor.DARK_PURPLE + newName + " IX";
            default:
                return ChatColor.DARK_GRAY + newName;
        }
    }

    public static String buildIdTier(String newName, Integer tier) {
        switch (tier) {
            case 1:
                return newName + "_I";
            case 2:
                return newName + "_II";
            case 3:
                return newName + "_III";
            case 4:
                return newName + "_IV";
            case 5:
                return newName + "_V";
            case 6:
                return newName + "_VI";
            case 7:
                return newName + "_VII";
            case 8:
                return newName + "_VIII";
            case 9:
                return newName + "_IX";
            default:
                return newName + "_0";
        }
    }

    public static String buildLoreRadioactivityType(MobTechGeneric.MobTechType mobTechType) {
        Radioactivity radioactivity;
        switch (mobTechType) {
            case MUTATION_INTELLIGENCE:
            case MUTATION_BERSERK:
            case MUTATION_LUCK:
                radioactivity = Radioactivity.VERY_DEADLY;
                break;
            case ROBOTIC_CLONING:
            case ROBOTIC_ACCELERATION:
            case ROBOTIC_EFFICIENCY:
                radioactivity = Radioactivity.HIGH;
                break;
            case SIMPLE:
            default:
                radioactivity = Radioactivity.LOW;
        }
        return radioactivity.getLore();
    }

    public static String buildLoreType(MobTechGeneric.MobTechType mobTechType, Integer tier) {
        String tierString = String.valueOf(tier + 1);
        switch (mobTechType) {
            case MUTATION_BERSERK:
            case ROBOTIC_ACCELERATION:
                return ChatColor.GREEN + tierString + "x " + ChatColor.GRAY + "increase speed " +
                        ChatColor.RED + tierString + "0%" + ChatColor.GRAY + " increase energy";
            case MUTATION_LUCK:
            case ROBOTIC_CLONING:
                return buildLoreTypeLuckAndCloning(tier);
            case MUTATION_INTELLIGENCE:
            case ROBOTIC_EFFICIENCY:
                return ChatColor.GREEN + tierString + "0% " + ChatColor.GRAY + " decrease energy";
            case SIMPLE:
            default:
                return ChatColor.GRAY + "Increases processing speed";
        }
    }

    private static String buildLoreTypeLuckAndCloning(Integer tier) {
        if (tier >= 9) {
            return ChatColor.GREEN + "+4 stack" + ChatColor.GRAY + " clone";
        } else if (tier >= 6) {
            return ChatColor.GREEN + "+3 stack" + ChatColor.GRAY + " clone";
        } else if (tier >= 4) {
            return ChatColor.GREEN + "+2 stack" + ChatColor.GRAY + " clone";
        } else {
            return ChatColor.GREEN + "+1 stack" + ChatColor.GRAY + " clone";
        }
    }

    private static String buildLoreTypeAmount(MobTechGeneric.MobTechType mobTechType, Integer tier) {
        String tierString = String.valueOf(tier + 1);
        switch (mobTechType) {
            case MUTATION_BERSERK:
            case ROBOTIC_ACCELERATION:
            case MUTATION_INTELLIGENCE:
            case ROBOTIC_EFFICIENCY:
                return ChatColor.GRAY + "Value process " + ChatColor.YELLOW
                        + "(" + tierString + " * amount stack * 0.15625)";
            case MUTATION_LUCK:
            case ROBOTIC_CLONING:
                return ChatColor.GRAY + "Value process " + ChatColor.YELLOW
                        + "(" + Supreme.getSupremeOptions().getMaxAmountTechGenerator() + "x per stack)";
            case SIMPLE:
            default:
                return ChatColor.GRAY + "Value process " + ChatColor.YELLOW
                        + "(amount stack * 0.15625)";
        }
    }

    public static SlimefunItemStack buildItemFromMobTechDTO(MobTechGeneric mobTechGeneric, Integer tier) {
        SlimefunItemStack itemStack = new SlimefunItemStack(buildIdTier(mobTechGeneric.getId(), tier),
                mobTechGeneric.getTexture(),
                buildNameTier(mobTechGeneric.getName(), tier), "",
                buildLoreRadioactivityType(mobTechGeneric.getMobTechType()),
                buildLoreType(mobTechGeneric.getMobTechType(), tier),
                buildLoreTypeAmount(mobTechGeneric.getMobTechType(), tier), "", "&3Supreme Component");
        ItemMeta itemMeta = itemStack.getItemMeta();
        PersistentDataAPI.setInt(itemMeta, new NamespacedKey(Supreme.inst(), "mob_tech_tier"), tier);
        PersistentDataAPI.setString(itemMeta, new NamespacedKey(Supreme.inst(), "mob_tech_type"), mobTechGeneric.getMobTechType().name());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
