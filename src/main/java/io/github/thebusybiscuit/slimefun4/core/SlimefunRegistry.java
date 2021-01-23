package io.github.thebusybiscuit.slimefun4.core;

import io.github.thebusybiscuit.cscorelib2.collections.KeyMap;
import io.github.thebusybiscuit.cscorelib2.config.Config;
import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlock;
import io.github.thebusybiscuit.slimefun4.core.researching.Research;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import io.github.thebusybiscuit.slimefun4.implementation.guide.CheatSheetSlimefunGuide;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunBlockHandler;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemHandler;
import me.mrCookieSlime.Slimefun.api.BlockInfoConfig;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.UniversalBlockMenu;
import org.apache.commons.lang.Validate;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class houses a lot of instances of {@link Map} and {@link List} that hold
 * various mappings and collections related to {@link SlimefunItem}.
 *
 * @author TheBusyBiscuit
 */
public final class SlimefunRegistry {

    private final Map<String, SlimefunItem> slimefunIds = new HashMap<>();
    private final List<SlimefunItem> slimefunItems = new ArrayList<>();
    private final List<SlimefunItem> enabledItems = new ArrayList<>();

    private final List<Category> categories = new ArrayList<>();
    private final List<MultiBlock> multiblocks = new LinkedList<>();

    private final List<Research> researches = new LinkedList<>();
    private final List<String> researchRanks = new ArrayList<>();
    private final Set<UUID> researchingPlayers = Collections.synchronizedSet(new HashSet<>());

    private boolean backwardsCompatibility;
    private boolean automaticallyLoadItems;
    private boolean enableResearches;
    private boolean freeCreativeResearches;
    private boolean researchFireworks;
    private boolean logDuplicateBlockEntries;
    private boolean useMoneyUnlock;

    private final Set<String> tickers = new HashSet<>();
    private final Set<SlimefunItem> radioactive = new HashSet<>();

    private final Set<ItemStack> barterDrops = new HashSet<>();

    private NamespacedKey soulboundKey;
    private NamespacedKey itemChargeKey;
    private NamespacedKey guideKey;

    private final KeyMap<GEOResource> geoResources = new KeyMap<>();

    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();
    private final Map<String, BlockStorage> worlds = new ConcurrentHashMap<>();
    private final Map<String, BlockInfoConfig> chunks = new HashMap<>();
    private final Map<SlimefunGuideMode, SlimefunGuideImplementation> guides = new EnumMap<>(SlimefunGuideMode.class);
    private final Map<EntityType, Set<ItemStack>> mobDrops = new EnumMap<>(EntityType.class);
    private final Map<String, BlockMenuPreset> blockMenuPresets = new HashMap<>();
    private final Map<String, UniversalBlockMenu> universalInventories = new HashMap<>();
    private final Map<Class<? extends ItemHandler>, Set<ItemHandler>> globalItemHandlers = new HashMap<>();
    private final Map<String, SlimefunBlockHandler> blockHandlers = new HashMap<>();

    public void load(@Nonnull SlimefunPlugin plugin, @Nonnull Config cfg) {
        Validate.notNull(plugin, "The Plugin cannot be null!");
        Validate.notNull(cfg, "The Config cannot be null!");

        soulboundKey = new NamespacedKey(plugin, "soulbound");
        itemChargeKey = new NamespacedKey(plugin, "item_charge");
        guideKey = new NamespacedKey(plugin, "slimefun_guide_mode");

        boolean showVanillaRecipes = cfg.getBoolean("guide.show-vanilla-recipes");

        guides.put(SlimefunGuideMode.SURVIVAL_MODE, new SurvivalSlimefunGuide(showVanillaRecipes));
        guides.put(SlimefunGuideMode.CHEAT_MODE, new CheatSheetSlimefunGuide());

        researchRanks.addAll(cfg.getStringList("research-ranks"));

        backwardsCompatibility = cfg.getBoolean("options.backwards-compatibility");
        freeCreativeResearches = cfg.getBoolean("researches.free-in-creative-mode");
        researchFireworks = cfg.getBoolean("researches.enable-fireworks");
        logDuplicateBlockEntries = cfg.getBoolean("options.log-duplicate-block-entries");
        useMoneyUnlock = cfg.getBoolean("researches.use-money-unlock");
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
     * This method returns whether backwards-compatibility is enabled.
     * Backwards compatibility allows Slimefun to recognize items from older versions but comes
     * at a huge performance cost.
     *
     * @return Whether backwards compatibility is enabled
     */
    public boolean isBackwardsCompatible() {
        return backwardsCompatibility;
    }

    public void setBackwardsCompatible(boolean compatible) {
        backwardsCompatibility = compatible;
    }

    public void setAutoLoadingMode(boolean mode) {
        automaticallyLoadItems = mode;
    }

    public List<Category> getCategories() {
        return categories;
    }

    /**
     * This {@link List} contains every {@link SlimefunItem}, even disabled items.
     *
     * @return A {@link List} containing every {@link SlimefunItem}
     */
    public List<SlimefunItem> getAllSlimefunItems() {
        return slimefunItems;
    }

    /**
     * This {@link List} contains every <strong>enabled</strong> {@link SlimefunItem}.
     *
     * @return A {@link List} containing every enabled {@link SlimefunItem}
     */
    public List<SlimefunItem> getEnabledSlimefunItems() {
        return enabledItems;
    }

    public List<Research> getResearches() {
        return researches;
    }

    public Set<UUID> getCurrentlyResearchingPlayers() {
        return researchingPlayers;
    }

    public List<String> getResearchRanks() {
        return researchRanks;
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

    public List<MultiBlock> getMultiBlocks() {
        return multiblocks;
    }

    /**
     * This returns the corresponding {@link SlimefunGuideImplementation} for a certain
     * {@link SlimefunGuideMode}.
     *
     * @param mode The {@link SlimefunGuideMode}
     * @return The corresponding {@link SlimefunGuideImplementation}
     */
    @Nonnull
    public SlimefunGuideImplementation getSlimefunGuide(@Nonnull SlimefunGuideMode mode) {
        Validate.notNull(mode, "The Guide mode cannot be null");

        SlimefunGuideImplementation guide = guides.get(mode);

        if (guide == null) {
            throw new IllegalStateException("Slimefun Guide '" + mode + "' has no registered implementation.");
        }

        return guide;
    }

    /**
     * This returns a {@link Map} connecting the {@link EntityType} with a {@link Set}
     * of {@link ItemStack ItemStacks} which would be dropped when an {@link Entity} of that type was killed.
     *
     * @return The {@link Map} of custom mob drops
     */
    public Map<EntityType, Set<ItemStack>> getMobDrops() {
        return mobDrops;
    }

    /**
     * This returns a {@link Set} of {@link ItemStack ItemStacks} which can be obtained by bartering
     * with {@link Piglin Piglins}.
     *
     * @return A {@link Set} of bartering drops
     */
    public Set<ItemStack> getBarteringDrops() {
        return barterDrops;
    }

    public Set<SlimefunItem> getRadioactiveItems() {
        return radioactive;
    }

    public Set<String> getTickerBlocks() {
        return tickers;
    }

    public Map<String, SlimefunItem> getSlimefunItemIds() {
        return slimefunIds;
    }

    public Map<String, BlockMenuPreset> getMenuPresets() {
        return blockMenuPresets;
    }

    public Map<String, UniversalBlockMenu> getUniversalInventories() {
        return universalInventories;
    }

    public Map<UUID, PlayerProfile> getPlayerProfiles() {
        return profiles;
    }

    public Map<Class<? extends ItemHandler>, Set<ItemHandler>> getPublicItemHandlers() {
        return globalItemHandlers;
    }

    public Map<String, SlimefunBlockHandler> getBlockHandlers() {
        return blockHandlers;
    }

    public Map<String, BlockStorage> getWorlds() {
        return worlds;
    }

    public Map<String, BlockInfoConfig> getChunks() {
        return chunks;
    }

    public KeyMap<GEOResource> getGEOResources() {
        return geoResources;
    }

    public boolean logDuplicateBlockEntries() {
        return logDuplicateBlockEntries;
    }

    public boolean isUseMoneyUnlock() {
        return useMoneyUnlock;
    }

    @Nonnull
    public NamespacedKey getSoulboundDataKey() {
        return soulboundKey;
    }

    @Nonnull
    public NamespacedKey getItemChargeDataKey() {
        return itemChargeKey;
    }

    @Nonnull
    public NamespacedKey getGuideDataKey() {
        return guideKey;
    }
}