package io.github.thebusybiscuit.slimefun4.core.config;

import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.ItemState;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.Getter;
import org.apache.commons.lang.Validate;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;

public class SlimefunConfigManager {
    /**
     * Slimefun plugin instance
     */
    private final Slimefun plugin;

    /**
     * Hold plugin config named "config.yml"
     */
    @Getter
    private final Config pluginConfig;

    /**
     * Hold item config named "Items.yml"
     */
    private final Config itemsConfig;

    /**
     * Hold research config named "Researches.yml"
     */
    private final Config researchesConfig;

    private boolean automaticallyLoadItems;
    private boolean enableResearches;
    private boolean freeCreativeResearches;
    private boolean researchFireworks;
    private boolean disableLearningAnimation;
    private boolean logDuplicateBlockEntries;
    private boolean talismanActionBarMessages;

    @Getter
    private boolean useMoneyUnlock;

    @Getter
    private boolean showVanillaRecipes;

    @Getter
    private boolean showHiddenItemGroupsInSearch;

    @Getter
    private boolean autoUpdate;

    @Getter
    private double researchCurrencyCostConvertRate;

    @Getter
    private boolean researchAutoConvert;

    @Getter
    private boolean bypassEnvironmentCheck;

    @Getter
    private boolean bypassItemLengthCheck;

    public SlimefunConfigManager(@Nonnull Slimefun plugin) {
        Validate.notNull(plugin, "The Plugin instance cannot be null");

        this.plugin = plugin;
        pluginConfig = getConfig(plugin, "config", () -> new Config(plugin));
        itemsConfig = getConfig(plugin, "Items", () -> new Config(plugin, "Items.yml"));
        researchesConfig = getConfig(plugin, "Researches", () -> new Config(plugin, "Researches.yml"));
    }

    @Nullable @ParametersAreNonnullByDefault
    private Config getConfig(Slimefun plugin, String name, Supplier<Config> supplier) {
        try {
            return supplier.get();
        } catch (Exception x) {
            plugin.getLogger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "An Exception was thrown while loading the config file \""
                                    + name
                                    + ".yml\" for Slimefun v"
                                    + plugin.getDescription().getVersion());
            return null;
        }
    }

    public boolean load() {
        return load(false);
    }

    public boolean load(boolean reload) {
        boolean isSuccessful = true;

        try {
            pluginConfig.reload();
            itemsConfig.reload();
            researchesConfig.reload();

            researchesConfig.setDefaultValue("enable-researching", true);
            enableResearches = researchesConfig.getBoolean("enable-researching");
            freeCreativeResearches = pluginConfig.getBoolean("researches.free-in-creative-mode");
            researchFireworks = pluginConfig.getBoolean("researches.enable-fireworks");
            disableLearningAnimation = pluginConfig.getBoolean("researches.disable-learning-animation");
            logDuplicateBlockEntries = pluginConfig.getBoolean("options.log-duplicate-block-entries");
            talismanActionBarMessages = pluginConfig.getBoolean("talismans.use-actionbar");
            useMoneyUnlock = pluginConfig.getBoolean("researches.use-money-unlock");
            showVanillaRecipes = pluginConfig.getBoolean("guide.show-vanilla-recipes");
            showHiddenItemGroupsInSearch = pluginConfig.getBoolean("guide.show-hidden-item-groups-in-search");
            autoUpdate = pluginConfig.getBoolean("options.auto-update");
            bypassEnvironmentCheck = pluginConfig.getBoolean("options.bypass-environment-check");
            bypassItemLengthCheck = pluginConfig.getBoolean("options.bypass-item-length-check");

            pluginConfig.setDefaultValue("researches.currency-cost-convert-rate", 25.0);
            researchCurrencyCostConvertRate = pluginConfig.getDouble("researches.currency-cost-convert-rate");

            pluginConfig.setDefaultValue("researches.auto-convert", false);
            researchAutoConvert = pluginConfig.getBoolean("researches.auto-convert");
        } catch (Exception x) {
            plugin.getLogger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "An Exception was caught while (re)loading the config files for Slimefun v"
                                    + plugin.getDescription().getVersion());
            isSuccessful = false;
        }

        if (!reload) {
            return true;
        }

        var researchSnapshot = new ArrayList<>(Slimefun.getRegistry().getResearches());

        // Reload Research costs
        for (Research research : researchSnapshot) {
            try {
                NamespacedKey key = research.getKey();
                int cost = researchesConfig.getInt(key.getNamespace() + '.' + key.getKey() + ".cost");
                research.setLevelCost(cost);

                if (researchAutoConvert) {
                    research.setCurrencyCost(researchCurrencyCostConvertRate * cost);
                } else {
                    research.setCurrencyCost(
                            researchesConfig.getDouble(key.getNamespace() + '.' + key.getKey() + ".currency-cost"));
                }

                var status = researchesConfig.getBoolean(key.getNamespace() + '.' + key.getKey() + ".enabled");

                if (research.isEnabled() != status) {
                    if (status) {
                        research.register();
                    } else {
                        research.disable();
                    }
                }
            } catch (Exception x) {
                plugin.getLogger()
                        .log(
                                Level.SEVERE,
                                x,
                                () -> "Something went wrong while trying to update the cost of a research: "
                                        + research);
                isSuccessful = false;
            }
        }

        var enabledItemSnapshot = new ArrayList<>(Slimefun.getRegistry().getAllSlimefunItems());

        for (SlimefunItem item : enabledItemSnapshot) {
            var newState = itemsConfig.getBoolean(item.getId() + ".enabled") ? ItemState.ENABLED : ItemState.DISABLED;

            if (item.getState() != newState) {
                switch (newState) {
                    case ENABLED -> item.enable();
                    case DISABLED -> {
                        item.disable();
                        continue;
                    }
                }
            }

            // Reload Item Settings
            try {
                for (ItemSetting<?> setting : item.getItemSettings()) {
                    setting.reload();
                }
            } catch (Exception x) {
                item.error("Something went wrong while updating the settings for this item!", x);
                isSuccessful = false;
            }

            // Reload permissions
            try {
                Slimefun.getPermissionsService().update(item, false);
            } catch (Exception x) {
                item.error("Something went wrong while updating the permission node for this item!", x);
                isSuccessful = false;
            }
        }

        return isSuccessful;
    }

    /**
     * This returns whether auto-loading is enabled.
     * Auto-Loading will automatically call {@link SlimefunItem#load()} when the item is registered.
     * Normally that method is called after the {@link Server} finished starting up.
     * But in the unusual scenario if a {@link SlimefunItem} is registered after that, this is gonna cover that.
     *
     * @return Whether auto-loading is enabled
     */
    public boolean isAutoLoadingEnabled() {
        return automaticallyLoadItems;
    }

    /**
     * This method will make any {@link SlimefunItem} which is registered automatically
     * call {@link SlimefunItem#load()}.
     * Normally this method call is delayed but when the {@link Server} is already running,
     * the method can be called instantaneously.
     *
     * @param mode Whether auto-loading should be enabled
     */
    public void setAutoLoadingMode(boolean mode) {
        automaticallyLoadItems = mode;
    }

    public void setResearchingEnabled(boolean enabled) {
        enableResearches = enabled;
    }

    public boolean isResearchingEnabled() {
        return enableResearches;
    }

    public void setFreeCreativeResearchingEnabled(boolean enabled) {
        freeCreativeResearches = enabled;
    }

    public boolean isFreeCreativeResearchingEnabled() {
        return freeCreativeResearches;
    }

    public boolean isResearchFireworkEnabled() {
        return researchFireworks;
    }

    /**
     * Returns whether the research learning animations is disabled
     *
     * @return Whether the research learning animations is disabled
     */
    public boolean isLearningAnimationDisabled() {
        return disableLearningAnimation;
    }

    public boolean logDuplicateBlockEntries() {
        return logDuplicateBlockEntries;
    }

    public boolean useActionbarForTalismans() {
        return talismanActionBarMessages;
    }

    public void setShowVanillaRecipes(boolean enabled) {
        showVanillaRecipes = enabled;
    }

    public void setShowHiddenItemGroupsInSearch(boolean enabled) {
        showHiddenItemGroupsInSearch = enabled;
    }
}
