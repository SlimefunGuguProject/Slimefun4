package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import city.norain.slimefun4.api.menu.UniversalMenu;
import city.norain.slimefun4.api.menu.UniversalMenuPreset;
import city.norain.slimefun4.utils.InventoryUtil;
import city.norain.slimefun4.utils.StringUtil;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.IDataSourceAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataType;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.FieldKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.ScopeKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalBlock;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalDataTrait;
import com.xzavier0722.mc.plugin.slimefun4.storage.event.SlimefunChunkDataLoadEvent;
import com.xzavier0722.mc.plugin.slimefun4.storage.task.DelayedSavingLooperTask;
import com.xzavier0722.mc.plugin.slimefun4.storage.task.DelayedTask;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.DataUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.InvSnapshot;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.InvStorageUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.LocationUtils;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * 方块数据控制器
 * <p>
 * 用于管理区块中的 Slimefun 方块数据
 * <p>
 * {@link SlimefunBlockData}
 * {@link SlimefunUniversalData}
 *
 * @author Xzavier0722
 * @author NoRainCity
 */
public class BlockDataController extends ADataController {
    /**
     * 延迟写数据任务队列
     */
    private final Map<LinkedKey, DelayedTask> delayedWriteTasks;
    /**
     * 区块数据缓存
     */
    private final Map<String, SlimefunChunkData> loadedChunk;
    /**
     * 通用数据缓存
     */
    private final Map<UUID, SlimefunUniversalData> loadedUniversalData;
    /**
     * 方块物品栏快照
     */
    private final Map<String, InvSnapshot> invSnapshots;
    /**
     * 全局控制器加载数据锁
     *
     * {@link ScopedLock}
     */
    private final ScopedLock lock;
    /**
     * 延时加载模式标志
     */
    private boolean enableDelayedSaving = false;

    private int delayedSecond = 0;
    private BukkitTask looperTask;
    /**
     * 区块数据加载模式
     * {@link ChunkDataLoadMode}
     */
    private ChunkDataLoadMode chunkDataLoadMode;

    /**
     * 初始化加载中标志
     */
    BlockDataController() {
        super(DataType.BLOCK_STORAGE);
        delayedWriteTasks = new ConcurrentHashMap<>();
        loadedChunk = new ConcurrentHashMap<>();
        loadedUniversalData = new ConcurrentHashMap<>();
        invSnapshots = new ConcurrentHashMap<>();
        lock = new ScopedLock();
    }

    /**
     * 初始化数据控制器
     *
     * @param dataAdapter    使用的 {@link IDataSourceAdapter}
     * @param maxReadThread  最大数据库读线程数
     * @param maxWriteThread 最大数据库写线程数
     */
    @Override
    public void init(IDataSourceAdapter<?> dataAdapter, int maxReadThread, int maxWriteThread) {
        super.init(dataAdapter, maxReadThread, maxWriteThread);
        this.chunkDataLoadMode = Slimefun.getDatabaseManager().getChunkDataLoadMode();
        initLoadData();
    }

    /**
     * 初始化加载数据
     */
    private void initLoadData() {
        switch (chunkDataLoadMode) {
            case LOAD_WITH_CHUNK -> loadLoadedChunks();
            case LOAD_ON_STARTUP -> loadLoadedWorlds();
        }

        Bukkit.getScheduler().runTaskLater(Slimefun.instance(), this::loadUniversalRecord, 1);
    }

    /**
     * 加载所有服务器已加载的世界中的数据
     */
    private void loadLoadedWorlds() {
        Bukkit.getScheduler()
                .runTaskLater(
                        Slimefun.instance(),
                        () -> {
                            for (var world : Bukkit.getWorlds()) {
                                loadWorld(world);
                            }
                        },
                        1);
    }

    /**
     * 加载所有服务器已加载的世界区块中的数据
     */
    private void loadLoadedChunks() {
        Bukkit.getScheduler()
                .runTaskLater(
                        Slimefun.instance(),
                        () -> {
                            for (var world : Bukkit.getWorlds()) {
                                for (var chunk : world.getLoadedChunks()) {
                                    loadChunk(chunk, false, true);
                                }
                            }
                        },
                        1);
    }

    /**
     * 初始化延时加载任务
     *
     * @param p               插件实例
     * @param delayedSecond   首次执行延时
     * @param forceSavePeriod 强制保存周期
     */
    public void initDelayedSaving(Plugin p, int delayedSecond, int forceSavePeriod) {
        checkDestroy();
        if (delayedSecond < 1 || forceSavePeriod < 1) {
            throw new IllegalArgumentException("save period second must be greater than 0!");
        }
        enableDelayedSaving = true;
        this.delayedSecond = delayedSecond;
        looperTask = Bukkit.getScheduler()
                .runTaskTimerAsynchronously(
                        p,
                        new DelayedSavingLooperTask(
                                forceSavePeriod, () -> new HashMap<>(delayedWriteTasks), delayedWriteTasks::remove),
                        20,
                        20);
    }

    public boolean isDelayedSavingEnabled() {
        return enableDelayedSaving;
    }

    public void setDelayedSavingEnable(boolean isEnable) {
        enableDelayedSaving = isEnable;
    }

    /**
     * 在指定位置创建一个新的 Slimefun 方块数据
     *
     * @param l    Slimefun 方块位置 {@link Location}
     * @param sfId Slimefun 物品 ID {@link SlimefunItem#getId()}
     * @return 方块数据, {@link SlimefunBlockData}
     */
    @Nonnull
    public SlimefunBlockData createBlock(Location l, String sfId) {
        checkDestroy();
        var sfItem = SlimefunItem.getById(sfId);

        if (sfItem instanceof UniversalBlock) {
            throw new IllegalArgumentException("Cannot create normal block data on UniversalBlock!");
        }

        var re = getChunkDataCache(l.getChunk(), true).createBlockData(l, sfId);
        if (Slimefun.getRegistry().getTickerBlocks().contains(sfId)) {
            Slimefun.getTickerTask().enableTicker(l);
        }
        return re;
    }

    /**
     * 创建一个新的 Slimefun 通用数据
     * <br/>
     * 提供一个可供读写的 KV 存储 Map
     *
     * @param sfId Slimefun 物品 ID {@link SlimefunItem#getId()}
     * @return 通用数据, {@link SlimefunUniversalData}
     */
    @Nonnull
    public SlimefunUniversalData createUniversalData(String sfId) {
        return createUniversalData(UUID.randomUUID(), sfId);
    }

    /**
     * 创建一个新的 Slimefun 通用数据
     * 提供一个可供读写的 KV 存储 Map
     *
     * @param uuid 通用数据的识别 UUID
     * @param sfId Slimefun 物品 ID {@link SlimefunItem#getId()}
     * @return 通用数据, {@link SlimefunUniversalData}
     */
    @Nonnull
    public SlimefunUniversalData createUniversalData(UUID uuid, String sfId) {
        checkDestroy();

        if (getUniversalDataFromCache(uuid) != null || getUniversalData(uuid) != null) {
            throw new IllegalArgumentException("A universal data with this UUID already exists: " + uuid);
        }

        var uniData = new SlimefunUniversalData(uuid, sfId);

        uniData.setIsDataLoaded(true);

        loadedUniversalData.put(uuid, uniData);

        Slimefun.getDatabaseManager().getBlockDataController().saveUniversalData(uniData);

        return uniData;
    }

    /**
     * 在指定位置创建一个新的 Slimefun 通用方块数据
     *
     * @param l    Slimefun 方块位置 {@link Location}
     * @param sfId Slimefun 物品 ID {@link SlimefunItem#getId()}
     * @return 通用方块数据, {@link SlimefunUniversalBlockData}
     */
    @Nonnull
    @ParametersAreNonnullByDefault
    public SlimefunUniversalBlockData createUniversalBlock(Location l, String sfId) {
        checkDestroy();

        var uuid = UUID.randomUUID();
        var uniData = new SlimefunUniversalBlockData(uuid, sfId, l);

        uniData.setIsDataLoaded(true);

        uniData.initTraits();

        loadedUniversalData.put(uuid, uniData);

        var preset = UniversalMenuPreset.getPreset(sfId);
        if (preset != null) {
            uniData.setMenu(new UniversalMenu(preset, uuid, l));
        }

        if (Slimefun.getRegistry().getTickerBlocks().contains(sfId)) {
            Slimefun.getTickerTask().enableTicker(l, uuid);
        }

        Slimefun.getDatabaseManager().getBlockDataController().saveUniversalData(uniData);

        if (Slimefun.getBlockDataService().isTileEntity(l.getBlock().getType())) {
            Slimefun.getBlockDataService().updateUniversalDataUUID(l.getBlock(), uniData.getKey());
        }

        uniData.initLastPresent();

        return uniData;
    }

    void saveNewBlock(Location l, String sfId) {
        var lKey = LocationUtils.getLocKey(l);

        var key = new RecordKey(DataScope.BLOCK_RECORD);
        // key.addCondition(FieldKey.LOCATION, lKey);

        var data = new RecordSet();
        data.put(FieldKey.LOCATION, lKey);
        data.put(FieldKey.CHUNK, LocationUtils.getChunkKey(l.getChunk()));
        data.put(FieldKey.SLIMEFUN_ID, sfId);

        var scopeKey = new LocationKey(DataScope.NONE, l);
        removeDelayedDataUpdates(scopeKey); // Shouldn't have.. But for safe..
        scheduleWriteTask(scopeKey, key, data, true);
    }

    /**
     * 立即计划保存一个通用数据
     *
     * @param universalData 欲写入数据库保存的通用数据
     */
    void saveUniversalData(SlimefunUniversalData universalData) {
        var uuid = universalData.getKey();
        var sfId = universalData.getSfId();
        var traitsStr = StringUtil.getTraitsStr(universalData.getTraits());

        var key = new RecordKey(DataScope.UNIVERSAL_RECORD);

        var data = new RecordSet();
        data.put(FieldKey.UNIVERSAL_UUID, uuid);
        data.put(FieldKey.SLIMEFUN_ID, sfId);
        data.put(FieldKey.UNIVERSAL_TRAITS, traitsStr);

        var scopeKey = new UUIDKey(DataScope.NONE, uuid);
        removeDelayedDataUpdates(scopeKey); // Shouldn't have.. But for safe..
        scheduleWriteTask(scopeKey, key, data, true);
    }

    /**
     * 移除指定位置上的 Slimefun 数据
     *
     * @param l 位置 {@link Location}
     */
    public void removeBlock(Location l) {
        checkDestroy();

        var removed = getChunkDataCache(l.getChunk(), true).removeBlockData(l);

        if (removed == null) {
            removeUniversalBlockData(l);

            return;
        }
        // fix issue # 992 # 1099
        invSnapshots.remove(removed.getKey());

        if (!removed.isDataLoaded()) {
            return;
        }

        if (Slimefun.getRegistry().getTickerBlocks().contains(removed.getSfId())) {
            Slimefun.getTickerTask().disableTicker(l);
        }

        var menu = removed.getBlockMenu();
        if (menu != null) {
            menu.lock();
        }
    }

    /**
     * 移除指定位置上的 Slimefun 方块数据
     *
     * @param l 位置 {@link Location}
     */
    public void removeBlockData(Location l) {
        checkDestroy();

        var removed = getChunkDataCache(l.getChunk(), true).removeBlockData(l);

        if (removed == null || !removed.isDataLoaded()) {
            return;
        }

        var menu = removed.getBlockMenu();
        if (menu != null) {
            InventoryUtil.closeInventory(menu.toInventory());
        }

        if (Slimefun.getRegistry().getTickerBlocks().contains(removed.getSfId())) {
            Slimefun.getTickerTask().disableTicker(l);
        }
    }

    /**
     * 移除指定位置对应的可能存在的 Slimefun 通用方块数据
     *
     * @param l {@link Location} 位置
     */
    public void removeUniversalBlockData(Location l) {
        checkDestroy();

        var toRemove = getUniversalBlockDataFromCache(l);

        if (toRemove.isEmpty()) {
            return;
        }

        removeUniversalBlockData(toRemove.get().getUUID());
    }

    /**
     * 移除指定 UUID 对应的 Slimefun 通用方块数据
     *
     * @param uuid 通用方块数据识别符
     */
    public void removeUniversalBlockData(UUID uuid) {
        checkDestroy();

        var toRemove = loadedUniversalData.get(uuid);

        if (toRemove == null) {
            return;
        }

        if (!toRemove.isDataLoaded()) {
            return;
        }

        toRemove.setPendingRemove(true);

        if (toRemove instanceof SlimefunUniversalBlockData ubd) {
            ubd.setPendingRemove(true);

            var menu = ubd.getMenu();
            if (menu != null) {
                menu.lock();
            }

            removeUniversalBlockDirectly(uuid);

            if (Slimefun.getRegistry().getTickerBlocks().contains(toRemove.getSfId())) {
                Slimefun.getTickerTask().disableTicker(ubd.getLastPresent().toLocation());
            }
        }

        loadedUniversalData.remove(uuid);
    }

    void removeBlockDirectly(Location l) {
        checkDestroy();
        var scopeKey = new LocationKey(DataScope.NONE, l);
        removeDelayedDataUpdates(scopeKey);

        var key = new RecordKey(DataScope.BLOCK_RECORD);
        key.addCondition(FieldKey.LOCATION, LocationUtils.getLocKey(l));
        scheduleDeleteTask(scopeKey, key, true);
    }

    void removeUniversalBlockDirectly(UUID uuid) {
        checkDestroy();
        var scopeKey = new UUIDKey(DataScope.NONE, uuid);
        removeDelayedDataUpdates(scopeKey);

        var key = new RecordKey(DataScope.UNIVERSAL_RECORD);
        key.addCondition(FieldKey.UNIVERSAL_UUID, uuid.toString());
        scheduleDeleteTask(scopeKey, key, true);
    }

    /**
     * Get slimefun block data at specific location
     *
     * @param l slimefun block location {@link Location}
     * @return {@link SlimefunBlockData}
     */
    @Nullable @ParametersAreNonnullByDefault
    public SlimefunBlockData getBlockData(Location l) {
        checkDestroy();
        if (chunkDataLoadMode.readCacheOnly()) {
            return getBlockDataFromCache(l);
        }
        var chunkData = getChunkDataCache(l, false);
        // fix issue #935
        if (chunkData != null) {
            var lKey = LocationUtils.getLocKey(l);
            var re = chunkData.getBlockCacheInternal(lKey);
            if (re != null || chunkData.hasBlockCache(lKey) || chunkData.isDataLoaded()) {
                return re;
            }
        }

        return loadBlockData(l);
    }

    private SlimefunBlockData loadBlockData(Location l) {
        var lKey = LocationUtils.getLocKey(l);
        var key = new RecordKey(DataScope.BLOCK_RECORD);
        key.addCondition(FieldKey.LOCATION, lKey);
        key.addField(FieldKey.SLIMEFUN_ID);

        var result = getData(key);
        var re =
                result.isEmpty() ? null : new SlimefunBlockData(l, result.get(0).get(FieldKey.SLIMEFUN_ID));
        if (re != null) {
            // fix issue #935
            SlimefunChunkData chunkData = getChunkDataCache(l, true);
            chunkData.addBlockCacheInternal(re, false);
            re = chunkData.getBlockCacheInternal(lKey);
        }
        return re;
    }

    public CompletableFuture<SlimefunBlockData> getBlockDataAsync(Location l) {
        checkDestroy();
        if (chunkDataLoadMode.readCacheOnly()) {
            return CompletableFuture.completedFuture(getBlockDataFromCache(l));
        }

        var chunkData = getChunkDataCache(l, false);
        // fix issue #935
        if (chunkData != null) {
            var lKey = LocationUtils.getLocKey(l);
            var re = chunkData.getBlockCacheInternal(lKey);
            if (re != null || chunkData.hasBlockCache(lKey) || chunkData.isDataLoaded()) {
                return CompletableFuture.completedFuture(re);
            }
        }
        return CompletableFuture.supplyAsync(() -> loadBlockData(l), this.readExecutor);
    }

    /**
     * Get slimefun block data at specific location asynchronous
     *
     * @param l        slimefun block location {@link Location}
     * @param callback operation when block data fetched {@link IAsyncReadCallback}
     */
    public void getBlockDataAsync(Location l, IAsyncReadCallback<SlimefunBlockData> callback) {
        scheduleReadTask(() -> invokeCallback(callback, getBlockData(l)));
    }

    /**
     * Get slimefun block data at specific location from cache
     *
     * @param l slimefun block location {@link Location}
     * @return {@link SlimefunBlockData}
     */
    public SlimefunBlockData getBlockDataFromCache(Location l) {
        return getBlockDataFromCache(LocationUtils.getChunkKey(l), LocationUtils.getLocKey(l));
    }

    /**
     * 从数据库中获取 {@link SlimefunUniversalData}
     */
    @Nullable public SlimefunUniversalData getUniversalData(@Nonnull UUID uuid) {
        checkDestroy();

        var key = new RecordKey(DataScope.UNIVERSAL_RECORD);
        key.addCondition(FieldKey.UNIVERSAL_UUID, uuid.toString());
        key.addField(FieldKey.SLIMEFUN_ID);
        key.addField(FieldKey.UNIVERSAL_TRAITS);

        var result = getData(key);

        if (result.isEmpty()) {
            return null;
        }

        var traits = StringUtil.getTraitsFromStr(result.get(0).get(FieldKey.UNIVERSAL_TRAITS));

        if (traits.contains(UniversalDataTrait.BLOCK)) {
            var ubd = new SlimefunUniversalBlockData(uuid, result.get(0).get(FieldKey.SLIMEFUN_ID));
            traits.forEach(ubd::addTrait);
            return ubd;
        } else {
            return new SlimefunUniversalData(uuid, result.get(0).get(FieldKey.SLIMEFUN_ID), traits);
        }
    }

    /**
     * Get slimefun universal data
     *
     * @param uuid universal data uuid {@link UUID}
     */
    @Nullable public SlimefunUniversalBlockData getUniversalBlockData(@Nonnull UUID uuid) {
        SlimefunUniversalData universalData = getUniversalData(uuid);

        if (universalData instanceof SlimefunUniversalBlockData ubd) {
            return ubd;
        } else {
            return null;
        }
    }

    /**
     * Get slimefun universal data asynchronous
     *
     * @param uuid     universal data uuid {@link UUID}
     * @param callback operation when block data fetched {@link IAsyncReadCallback}
     */
    public void getUniversalBlockData(@Nonnull UUID uuid, IAsyncReadCallback<SlimefunUniversalBlockData> callback) {
        scheduleReadTask(() -> invokeCallback(callback, getUniversalBlockData(uuid)));
    }

    /**
     * 从缓存中获取 {@link SlimefunUniversalData}
     *
     * @param uuid 通用数据 UUID
     * @return {@link SlimefunUniversalData}
     */
    @Nullable public SlimefunUniversalData getUniversalDataFromCache(@Nonnull UUID uuid) {
        checkDestroy();

        return loadedUniversalData.get(uuid);
    }

    /**
     * Get slimefun universal data from cache
     *
     * @param uuid universal data uuid {@link UUID}
     */
    @Nullable public SlimefunUniversalBlockData getUniversalBlockDataFromCache(@Nonnull UUID uuid) {
        var cache = getUniversalDataFromCache(uuid);

        if (cache instanceof SlimefunUniversalBlockData ubd) {
            return ubd;
        } else {
            return null;
        }
    }

    /**
     * Get slimefun universal data from cache by location
     *
     * @param l Slimefun block location {@link Location}
     */
    public Optional<SlimefunUniversalBlockData> getUniversalBlockDataFromCache(@Nonnull Location l) {
        checkDestroy();

        for (SlimefunUniversalData uniData : loadedUniversalData.values()) {
            if (uniData instanceof SlimefunUniversalBlockData ubd) {
                if (!ubd.isDataLoaded() || ubd.getLastPresent() == null) {
                    continue;
                }

                if (l.equals(ubd.getLastPresent().toLocation())) {
                    return Optional.of(ubd);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * @deprecated use {@link #move(SlimefunBlockData, Location)} instead
     */
    @Deprecated(forRemoval = true)
    public void setBlockDataLocation(SlimefunBlockData blockData, Location target) {
        move(blockData, target);
    }

    /**
     * Move block data to specific location
     * <p>
     * Similar to original BlockStorage#move.
     *
     * @param data   the block data {@link SlimefunBlockData} need to move
     * @param target move target {@link Location}
     */
    public void move(ASlimefunDataContainer data, Location target) {
        if (data instanceof SlimefunBlockData blockData) {
            move(blockData, target);
        } else if (data instanceof SlimefunUniversalBlockData universalBlockData) {
            move(universalBlockData, target);
        }
    }

    private void move(SlimefunBlockData blockData, Location target) {
        if (LocationUtils.isSameLoc(blockData.getLocation(), target)) {
            return;
        }

        var hasTicker = false;

        if (blockData.isDataLoaded() && Slimefun.getRegistry().getTickerBlocks().contains(blockData.getSfId())) {
            Slimefun.getTickerTask().disableTicker(blockData.getLocation());
            hasTicker = true;
        }

        BlockMenu menu = null;

        if (blockData.isDataLoaded() && blockData.getBlockMenu() != null) {
            menu = blockData.getBlockMenu();
            menu.lock();
        }

        try {
            var chunk = blockData.getLocation().getChunk();
            var chunkData = getChunkDataCache(chunk, false);
            if (chunkData != null) {
                chunkData.removeBlockDataCacheInternal(blockData.getKey());
            }

            var newBlockData = new SlimefunBlockData(target, blockData);
            var key = new RecordKey(DataScope.BLOCK_RECORD);
            if (LocationUtils.isSameChunk(blockData.getLocation().getChunk(), target.getChunk())) {
                if (chunkData == null) {
                    chunkData = getChunkDataCache(chunk, true);
                }
                key.addField(FieldKey.CHUNK);
            } else {
                chunkData = getChunkDataCache(target.getChunk(), true);
            }

            chunkData.addBlockCacheInternal(newBlockData, true);

            if (menu != null) {
                newBlockData.setBlockMenu(new BlockMenu(menu.getPreset(), target, menu.getInventory()));
            }

            key.addField(FieldKey.LOCATION);
            key.addCondition(FieldKey.LOCATION, blockData.getKey());

            var data = new RecordSet();
            data.put(FieldKey.LOCATION, newBlockData.getKey());
            data.put(FieldKey.CHUNK, chunkData.getKey());
            data.put(FieldKey.SLIMEFUN_ID, blockData.getSfId());
            var scopeKey = new LocationKey(DataScope.NONE, blockData.getLocation());
            synchronized (delayedWriteTasks) {
                var it = delayedWriteTasks.entrySet().iterator();
                while (it.hasNext()) {
                    var next = it.next();
                    if (scopeKey.equals(next.getKey().getParent())) {
                        next.getValue().runUnsafely();
                        it.remove();
                    }
                }
            }

            scheduleWriteTask(scopeKey, key, data, true);

            if (hasTicker) {
                Slimefun.getTickerTask().enableTicker(target);
            }
        } finally {
            if (menu != null) {
                menu.unlock();
            }
        }
    }

    private void move(SlimefunUniversalBlockData uniData, Location target) {
        var loc = uniData.getLastPresent().toLocation();

        if (LocationUtils.isSameLoc(loc, target)) {
            return;
        }

        var hasTicker = false;

        if (uniData.isDataLoaded() && Slimefun.getRegistry().getTickerBlocks().contains(uniData.getSfId())) {
            Slimefun.getTickerTask().disableTicker(loc);
            hasTicker = true;
        }

        UniversalMenu menu = null;

        if (uniData.isDataLoaded() && uniData.getMenu() != null) {
            menu = uniData.getMenu();
            menu.lock();
        }

        try {
            uniData.setLastPresent(target);

            Slimefun.getBlockDataService()
                    .updateUniversalDataUUID(
                            target.getBlock(), uniData.getUUID().toString());

            if (menu != null) {
                menu.update(target);
            }

            if (hasTicker) {
                Slimefun.getTickerTask().enableTicker(target, uniData.getUUID());
            }
        } finally {
            if (menu != null) {
                menu.unlock();
            }
        }
    }

    private SlimefunBlockData getBlockDataFromCache(String cKey, String lKey) {
        checkDestroy();
        var chunkData = loadedChunk.get(cKey);
        return chunkData == null ? null : chunkData.getBlockCacheInternal(lKey);
    }

    public void loadChunk(Chunk chunk, boolean isNewChunk) {
        loadChunk(chunk, isNewChunk, false);
    }

    public void loadChunk(Chunk chunk, boolean isNewChunk, boolean forceReadData) {
        checkDestroy();
        var chunkData = getChunkDataCache(chunk, true);
        // what if the database already contains data here but the WORLD CHUNK is newly generated

        // escape all return if forceRead Flag is true
        if (forceReadData) {
            if (chunkDataLoadMode.readCacheOnly()) {
                // if readCache only , then all the chunkData get from cache is DataLoaded
                // since we removed initLoading, so we set DataLoad false here, so it will trigger the loadChunkData
                chunkData.setIsDataLoaded(false);
            }
            // else force loading, escape all returns
        } else {
            // not force loading
            if (isNewChunk) {
                chunkData.setIsDataLoaded(true);
                Bukkit.getPluginManager().callEvent(new SlimefunChunkDataLoadEvent(chunkData));
                return;
            }

            if (chunkData.isDataLoaded()) {
                return;
            }
        }

        loadChunkData(chunkData);

        // 按区块加载方块数据

        var key = new RecordKey(DataScope.BLOCK_RECORD);
        key.addField(FieldKey.LOCATION);
        key.addField(FieldKey.SLIMEFUN_ID);
        key.addCondition(FieldKey.CHUNK, chunkData.getKey());

        getData(key).forEach(block -> {
            var lKey = block.get(FieldKey.LOCATION);
            var sfId = block.get(FieldKey.SLIMEFUN_ID);
            var sfItem = SlimefunItem.getById(sfId);
            if (sfItem == null) {
                return;
            }

            var cache = getBlockDataFromCache(chunkData.getKey(), lKey);
            var blockData = cache == null ? new SlimefunBlockData(LocationUtils.toLocation(lKey), sfId) : cache;
            chunkData.addBlockCacheInternal(blockData, false);

            if (sfItem.loadDataByDefault()) {
                scheduleReadTask(() -> loadBlockData(blockData));
            }
        });

        Bukkit.getPluginManager().callEvent(new SlimefunChunkDataLoadEvent(chunkData));
    }

    public void loadWorld(World world) {
        var start = System.currentTimeMillis();
        var worldName = world.getName();
        logger.log(Level.INFO, "正在加载世界 {0} 的 Slimefun 方块数据...", worldName);
        var chunkKeys = new HashSet<String>();
        var key = new RecordKey(DataScope.CHUNK_DATA);
        key.addField(FieldKey.CHUNK);
        key.addCondition(FieldKey.CHUNK, worldName + ";%");
        getData(key, true).forEach(data -> chunkKeys.add(data.get(FieldKey.CHUNK)));

        key = new RecordKey(DataScope.BLOCK_RECORD);
        key.addField(FieldKey.CHUNK);
        key.addCondition(FieldKey.CHUNK, world.getName() + ";%");
        getData(key, true).forEach(data -> chunkKeys.add(data.get(FieldKey.CHUNK)));

        chunkKeys.forEach(cKey -> loadChunk(LocationUtils.toChunk(world, cKey), false, true));
        logger.log(
                Level.INFO, "世界 {0} 数据加载完成, 耗时 {1}ms", new Object[] {worldName, (System.currentTimeMillis() - start)});
    }

    public void loadUniversalRecord() {
        var uniKey = new RecordKey(DataScope.UNIVERSAL_RECORD);
        uniKey.addField(FieldKey.UNIVERSAL_UUID);
        uniKey.addField(FieldKey.SLIMEFUN_ID);
        uniKey.addField(FieldKey.UNIVERSAL_TRAITS);

        var uniRecord = getData(uniKey);

        uniRecord.forEach(data -> {
            var sfId = data.get(FieldKey.SLIMEFUN_ID);
            var sfItem = SlimefunItem.getById(sfId);

            if (sfItem == null) {
                return;
            }

            var uuid = data.getUUID(FieldKey.UNIVERSAL_UUID);
            var traitsData = data.get(FieldKey.UNIVERSAL_TRAITS);
            var traits = new HashSet<UniversalDataTrait>();

            // Read trait(s) of universal data
            if (traitsData != null && !traitsData.isBlank()) {
                for (String traitStr : traitsData.split(",")) {
                    try {
                        traits.add(UniversalDataTrait.valueOf(traitStr.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        logger.log(Level.WARNING, "Invalid trait '{0}' for universal data {1}.", new Object[] {
                            traitStr, uuid
                        });
                    }
                }
            }

            var uniData = traits.contains(UniversalDataTrait.BLOCK)
                    ? new SlimefunUniversalBlockData(uuid, sfId)
                    : new SlimefunUniversalData(uuid, sfId);

            traits.forEach(uniData::addTrait);

            scheduleReadTask(() -> loadUniversalData(uniData));
        });
    }

    private void loadChunkData(SlimefunChunkData chunkData) {
        if (chunkData.isDataLoaded()) {
            return;
        }
        var key = new RecordKey(DataScope.CHUNK_DATA);
        key.addField(FieldKey.DATA_KEY);
        key.addField(FieldKey.DATA_VALUE);
        key.addCondition(FieldKey.CHUNK, chunkData.getKey());

        lock.lock(key);
        try {
            if (chunkData.isDataLoaded()) {
                return;
            }
            getData(key)
                    .forEach(data -> chunkData.setCacheInternal(
                            data.get(FieldKey.DATA_KEY),
                            DataUtils.blockDataDebase64(data.get(FieldKey.DATA_VALUE)),
                            false));
            chunkData.setIsDataLoaded(true);
        } finally {
            lock.unlock(key);
        }
    }

    public void loadBlockData(SlimefunBlockData blockData) {
        if (blockData.isDataLoaded()) {
            return;
        }
        var key = new RecordKey(DataScope.BLOCK_DATA);
        key.addCondition(FieldKey.LOCATION, blockData.getKey());
        key.addField(FieldKey.DATA_KEY);
        key.addField(FieldKey.DATA_VALUE);

        lock.lock(key);
        try {
            if (blockData.isDataLoaded()) {
                return;
            }

            var sfItem = SlimefunItem.getById(blockData.getSfId());
            var universal = sfItem instanceof UniversalBlock;

            var kvData = getData(key);

            var menuKey = new RecordKey(DataScope.BLOCK_INVENTORY);
            menuKey.addCondition(FieldKey.LOCATION, blockData.getKey());
            menuKey.addField(FieldKey.INVENTORY_SLOT);
            menuKey.addField(FieldKey.INVENTORY_ITEM);

            var invData = getData(menuKey);

            if (universal) {
                migrateUniversalData(blockData.getLocation(), blockData.getSfId(), kvData, invData);
            } else {
                kvData.forEach(recordSet -> blockData.setCacheInternal(
                        recordSet.get(FieldKey.DATA_KEY),
                        DataUtils.blockDataDebase64(recordSet.get(FieldKey.DATA_VALUE)),
                        false));

                blockData.setIsDataLoaded(true);

                var menuPreset = BlockMenuPreset.getPreset(blockData.getSfId());

                if (menuPreset != null) {
                    var inv = new ItemStack[54];

                    for (RecordSet record : invData) {
                        var slot = record.getInt(FieldKey.INVENTORY_SLOT);

                        try {
                            inv[slot] = record.getItemStack(FieldKey.INVENTORY_ITEM);
                        } catch (Exception ex) {
                            inv[slot] = null;
                            Slimefun.logger()
                                    .log(
                                            Level.SEVERE,
                                            "加载目标物品失败, 请检查实际数据 ["
                                                    + LocationUtils.locationToString(blockData.getLocation()) + ":"
                                                    + slot + "]",
                                            ex);
                        }
                    }

                    blockData.setBlockMenu(new BlockMenu(menuPreset, blockData.getLocation(), inv));

                    var content = blockData.getMenuContents();
                    if (content != null) {
                        invSnapshots.put(blockData.getKey(), new InvSnapshot(content));
                    }
                }
            }

            if (sfItem != null && sfItem.isTicking()) {
                Slimefun.getTickerTask().enableTicker(blockData.getLocation());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load block data: " + blockData.getKey(), e);
        } finally {
            lock.unlock(key);
        }
    }

    public void loadDataAsync(ASlimefunDataContainer container, IAsyncReadCallback<ASlimefunDataContainer> callback) {
        scheduleReadTask(() -> {
            if (container instanceof SlimefunBlockData blockData) {
                loadBlockData(blockData);
            } else if (container instanceof SlimefunUniversalData uniData) {
                loadUniversalData(uniData);
            }

            invokeCallback(callback, container);
        });
    }

    public void loadBlockDataAsync(SlimefunBlockData blockData, IAsyncReadCallback<SlimefunBlockData> callback) {
        scheduleReadTask(() -> {
            loadBlockData(blockData);
            invokeCallback(callback, blockData);
        });
    }

    public void loadBlockDataAsync(
            List<SlimefunBlockData> blockDataList, IAsyncReadCallback<List<SlimefunBlockData>> callback) {
        scheduleReadTask(() -> blockDataList.forEach(this::loadBlockData));
        invokeCallback(callback, blockDataList);
    }

    @ParametersAreNonnullByDefault
    public void loadUniversalData(SlimefunUniversalData uniData) {
        if (uniData.isDataLoaded()) {
            return;
        }

        // 构建 通用数据 kv 存储 查询条件
        var key = new RecordKey(DataScope.UNIVERSAL_DATA);
        key.addCondition(FieldKey.UNIVERSAL_UUID, uniData.getKey());
        key.addField(FieldKey.DATA_KEY);
        key.addField(FieldKey.DATA_VALUE);

        lock.lock(key);

        try {
            if (uniData.isDataLoaded()) {
                return;
            }

            getData(key)
                    .forEach(recordSet -> uniData.setCacheInternal(
                            recordSet.get(FieldKey.DATA_KEY),
                            DataUtils.blockDataDebase64(recordSet.get(FieldKey.DATA_VALUE)),
                            false));

            uniData.setIsDataLoaded(true);

            loadedUniversalData.putIfAbsent(uniData.getUUID(), uniData);

            if (uniData instanceof SlimefunUniversalBlockData ubd) {
                if (ubd.hasTrait(UniversalDataTrait.BLOCK)) {
                    // 初始化 上次出现位置
                    var lStr = ubd.getData(UniversalDataTrait.BLOCK.getReservedKey());

                    if (lStr != null && !lStr.isBlank()) {
                        ubd.setLastPresent(LocationUtils.toLocation(lStr));
                    }

                    var sfItem = SlimefunItem.getById(ubd.getSfId());

                    if (sfItem != null && sfItem.isTicking() && ubd.getLastPresent() != null) {
                        Slimefun.getTickerTask()
                                .enableTicker(ubd.getLastPresent().toLocation(), ubd.getUUID());
                    }
                }
            }

            if (uniData.hasTrait(UniversalDataTrait.INVENTORY)) {
                // 加载菜单
                var menuPreset = UniversalMenuPreset.getPreset(uniData.getSfId());
                if (menuPreset != null) {
                    var menuKey = new RecordKey(DataScope.UNIVERSAL_INVENTORY);
                    menuKey.addCondition(FieldKey.UNIVERSAL_UUID, uniData.getKey());
                    menuKey.addField(FieldKey.INVENTORY_SLOT);
                    menuKey.addField(FieldKey.INVENTORY_ITEM);

                    var inv = new ItemStack[54];

                    for (RecordSet recordSet : getData(menuKey)) {
                        var slot = recordSet.getInt(FieldKey.INVENTORY_SLOT);
                        try {
                            inv[slot] = recordSet.getItemStack(FieldKey.INVENTORY_ITEM);
                        } catch (Exception ex) {
                            inv[slot] = null;
                            Slimefun.logger()
                                    .log(Level.SEVERE, "加载目标物品失败, 请检查实际数据 [" + uniData.getKey() + ":" + slot + "]", ex);
                        }
                    }

                    Location location = null;

                    if (uniData instanceof SlimefunUniversalBlockData ubd && ubd.hasTrait(UniversalDataTrait.BLOCK)) {
                        if (ubd.getLastPresent() != null) {
                            location = ubd.getLastPresent().toLocation();
                        }
                    }

                    uniData.setMenu(new UniversalMenu(menuPreset, uniData.getUUID(), location, inv));

                    var content = uniData.getMenuContents();

                    if (content != null) {
                        invSnapshots.put(uniData.getKey(), new InvSnapshot(content));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load universal data: " + uniData.getKey(), e);
        } finally {
            lock.unlock(key);
        }
    }

    @ParametersAreNonnullByDefault
    public void loadUniversalDataAsync(
            SlimefunUniversalData uniData, IAsyncReadCallback<SlimefunUniversalData> callback) {
        scheduleReadTask(() -> {
            loadUniversalData(uniData);
            invokeCallback(callback, uniData);
        });
    }

    public SlimefunChunkData getChunkData(Chunk chunk) {
        checkDestroy();
        loadChunk(chunk, false);
        return getChunkDataCache(chunk, false);
    }

    public CompletableFuture<SlimefunChunkData> getChunkDataAsync(Chunk chunk) {
        checkDestroy();
        SlimefunChunkData cdata = getChunkDataCache(chunk, true);
        if (cdata.isDataLoaded()) {
            return CompletableFuture.completedFuture(cdata);
        }
        return CompletableFuture.runAsync(() -> loadChunk(chunk, false), readExecutor)
                .thenApply((v) -> getChunkDataCache(chunk, false));
    }

    public void getChunkDataAsync(Chunk chunk, IAsyncReadCallback<SlimefunChunkData> callback) {
        scheduleReadTask(() -> invokeCallback(callback, getChunkData(chunk)));
    }

    public void saveAllBlockInventories() {
        var chunks = new HashSet<>(loadedChunk.values());
        chunks.forEach(chunk -> chunk.getAllCacheInternal().forEach(block -> {
            if (block.isPendingRemove() || !block.isDataLoaded()) {
                return;
            }
            var menu = block.getBlockMenu();
            if (menu == null || !menu.isDirty()) {
                return;
            }

            saveBlockInventory(block);
        }));
    }

    public void saveAllUniversalInventories() {
        var uniData = new HashSet<>(loadedUniversalData.values());
        uniData.forEach(data -> {
            if (data.isPendingRemove() || !data.isDataLoaded()) {
                return;
            }
            var menu = data.getMenu();
            if (menu == null || !menu.isDirty()) {
                return;
            }

            saveUniversalInventory(data);
        });
    }

    public void saveBlockInventory(SlimefunBlockData blockData) {
        var newInv = blockData.getMenuContents();
        InvSnapshot lastSave;
        if (newInv == null) {
            lastSave = invSnapshots.remove(blockData.getKey());
            if (lastSave == null) {
                return;
            }
        } else {
            lastSave = invSnapshots.put(blockData.getKey(), new InvSnapshot(newInv));
        }

        var changed = InvStorageUtils.getChangedSlots(lastSave, newInv);
        if (changed.isEmpty()) {
            return;
        }

        changed.forEach(slot -> saveBlockInventorySlot(blockData, slot));
    }

    public void saveBlockInventorySlot(SlimefunBlockData blockData, int slot) {
        scheduleDelayedBlockInvUpdate(blockData, slot);
    }

    public Set<SlimefunChunkData> getAllLoadedChunkData() {
        return new HashSet<>(loadedChunk.values());
    }

    public void removeAllDataInChunk(Chunk chunk) {
        var cKey = LocationUtils.getChunkKey(chunk);
        var cache = loadedChunk.remove(cKey);

        if (cache != null && cache.isDataLoaded()) {
            cache.getAllBlockData().forEach(this::clearBlockCacheAndTasks);
        }
        deleteChunkAndBlockDataDirectly(cKey);
    }

    public void removeAllDataInChunkAsync(Chunk chunk, Runnable onFinishedCallback) {
        scheduleWriteTask(() -> {
            removeAllDataInChunk(chunk);
            onFinishedCallback.run();
        });
    }

    public void removeAllDataInWorld(World world) {
        // 1. remove block cache
        var loadedBlockData = new HashSet<SlimefunBlockData>();
        for (var chunkData : getAllLoadedChunkData(world)) {
            loadedBlockData.addAll(chunkData.getAllBlockData());
            chunkData.removeAllCacheInternal();
        }

        // 2. remove ticker and delayed tasks
        loadedBlockData.forEach(this::clearBlockCacheAndTasks);

        // 3. remove from database
        var prefix = world.getName() + ";";
        deleteChunkAndBlockDataDirectly(prefix + "%");

        // 4. remove chunk cache
        loadedChunk.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }

    public void removeAllDataInWorldAsync(World world, Runnable onFinishedCallback) {
        scheduleWriteTask(() -> {
            removeAllDataInWorld(world);
            onFinishedCallback.run();
        });
    }

    public void saveUniversalInventory(@Nonnull SlimefunUniversalData universalData) {
        var universalID = universalData.getUUID();

        var currentInv = universalData.getMenuContents();
        InvSnapshot lastSave;

        if (currentInv == null) {
            lastSave = invSnapshots.remove(universalID.toString());
            if (lastSave == null) {
                return;
            }
        } else {
            lastSave = invSnapshots.put(universalID.toString(), new InvSnapshot(currentInv));
        }

        var changed = InvStorageUtils.getChangedSlots(lastSave, currentInv);
        if (changed.isEmpty()) {
            return;
        }

        changed.forEach(slot -> scheduleDelayedUniversalInvUpdate(universalData, slot));
    }

    public Set<SlimefunChunkData> getAllLoadedChunkData(World world) {
        var prefix = world.getName() + ";";
        var re = new HashSet<SlimefunChunkData>();
        loadedChunk.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                re.add(v);
            }
        });
        return re;
    }

    public void removeFromAllChunkInWorld(World world, String key) {
        var req = new RecordKey(DataScope.CHUNK_DATA);
        req.addCondition(FieldKey.CHUNK, world.getName() + ";%");
        req.addCondition(FieldKey.DATA_KEY, key);
        deleteData(req);
        getAllLoadedChunkData(world).forEach(data -> data.removeData(key));
    }

    public void removeFromAllChunkInWorldAsync(World world, String key, Runnable onFinishedCallback) {
        scheduleWriteTask(() -> {
            removeFromAllChunkInWorld(world, key);
            onFinishedCallback.run();
        });
    }

    private void scheduleDelayedBlockInvUpdate(SlimefunBlockData blockData, int slot) {
        var scopeKey = new LocationKey(DataScope.NONE, blockData.getLocation());
        var reqKey = new RecordKey(DataScope.BLOCK_INVENTORY);
        reqKey.addCondition(FieldKey.LOCATION, blockData.getKey());
        reqKey.addCondition(FieldKey.INVENTORY_SLOT, slot + "");
        reqKey.addField(FieldKey.INVENTORY_ITEM);

        if (enableDelayedSaving) {
            scheduleDelayedUpdateTask(
                    new LinkedKey(scopeKey, reqKey),
                    () -> scheduleBlockInvUpdate(
                            scopeKey, reqKey, blockData.getKey(), blockData.getMenuContents(), slot));
        } else {
            scheduleBlockInvUpdate(scopeKey, reqKey, blockData.getKey(), blockData.getMenuContents(), slot);
        }
    }

    private void scheduleBlockInvUpdate(ScopeKey scopeKey, RecordKey reqKey, String lKey, ItemStack[] inv, int slot) {
        var item = inv != null && slot < inv.length ? inv[slot] : null;

        if (item == null) {
            scheduleDeleteTask(scopeKey, reqKey, true);
        } else {
            try {
                var data = new RecordSet();
                data.put(FieldKey.LOCATION, lKey);
                data.put(FieldKey.INVENTORY_SLOT, slot + "");
                data.put(FieldKey.INVENTORY_ITEM, item);
                scheduleWriteTask(scopeKey, reqKey, data, true);
            } catch (IllegalArgumentException e) {
                Slimefun.logger().log(Level.WARNING, e.getMessage());
            }
        }
    }

    /**
     * Save universal inventory by async way
     *
     * @param ubd  {@link SlimefunUniversalBlockData}
     * @param slot updated item slot
     */
    private void scheduleDelayedUniversalInvUpdate(SlimefunUniversalData ubd, int slot) {
        var scopeKey = new UUIDKey(DataScope.NONE, ubd.getKey());
        var reqKey = new RecordKey(DataScope.UNIVERSAL_INVENTORY);
        reqKey.addCondition(FieldKey.UNIVERSAL_UUID, ubd.getKey());
        reqKey.addCondition(FieldKey.INVENTORY_SLOT, slot + "");
        reqKey.addField(FieldKey.INVENTORY_ITEM);

        if (enableDelayedSaving) {
            scheduleDelayedUpdateTask(
                    new LinkedKey(scopeKey, reqKey),
                    () -> scheduleUniversalInvUpdate(scopeKey, reqKey, ubd.getKey(), ubd.getMenuContents(), slot));
        } else {
            scheduleUniversalInvUpdate(scopeKey, reqKey, ubd.getKey(), ubd.getMenuContents(), slot);
        }
    }

    private void scheduleUniversalInvUpdate(
            ScopeKey scopeKey, RecordKey reqKey, String uuid, ItemStack[] inv, int slot) {
        var item = inv != null && slot < inv.length ? inv[slot] : null;

        if (item == null) {
            scheduleDeleteTask(scopeKey, reqKey, true);
        } else {
            try {
                var data = new RecordSet();
                data.put(FieldKey.UNIVERSAL_UUID, uuid);
                data.put(FieldKey.INVENTORY_SLOT, slot + "");
                data.put(FieldKey.INVENTORY_ITEM, item);
                scheduleWriteTask(scopeKey, reqKey, data, true);
            } catch (IllegalArgumentException e) {
                Slimefun.logger().log(Level.WARNING, e.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        saveAllBlockInventories();
        saveAllUniversalInventories();
        if (enableDelayedSaving) {
            looperTask.cancel();
            executeAllDelayedTasks();
        }
        super.shutdown();
    }

    void scheduleDelayedBlockDataUpdate(SlimefunBlockData blockData, String key) {
        var scopeKey = new LocationKey(DataScope.NONE, blockData.getLocation());
        var reqKey = new RecordKey(DataScope.BLOCK_DATA);
        reqKey.addCondition(FieldKey.LOCATION, blockData.getKey());
        reqKey.addCondition(FieldKey.DATA_KEY, key);
        if (enableDelayedSaving) {
            scheduleDelayedUpdateTask(
                    new LinkedKey(scopeKey, reqKey),
                    () -> scheduleBlockDataUpdate(scopeKey, reqKey, blockData.getKey(), key, blockData.getData(key)));
        } else {
            scheduleBlockDataUpdate(scopeKey, reqKey, blockData.getKey(), key, blockData.getData(key));
        }
    }

    void scheduleDelayedUniversalDataUpdate(SlimefunUniversalData universalData, String key) {
        var scopeKey = new UUIDKey(DataScope.NONE, universalData.getKey());
        var reqKey = new RecordKey(DataScope.UNIVERSAL_DATA);
        reqKey.addCondition(FieldKey.UNIVERSAL_UUID, universalData.getKey());
        reqKey.addCondition(FieldKey.DATA_KEY, key);

        if (enableDelayedSaving) {
            scheduleDelayedUpdateTask(
                    new LinkedKey(scopeKey, reqKey),
                    () -> scheduleUniversalDataUpdate(
                            scopeKey, reqKey, universalData.getKey(), key, universalData.getData(key)));
        } else {
            scheduleUniversalDataUpdate(scopeKey, reqKey, universalData.getKey(), key, universalData.getData(key));
        }
    }

    private void removeDelayedDataUpdates(ScopeKey scopeKey) {
        synchronized (delayedWriteTasks) {
            delayedWriteTasks
                    .entrySet()
                    .removeIf(each -> scopeKey.equals(each.getKey().getParent()));
        }
    }

    private void scheduleBlockDataUpdate(ScopeKey scopeKey, RecordKey reqKey, String lKey, String key, String val) {
        if (val == null) {
            scheduleDeleteTask(scopeKey, reqKey, false);
        } else {
            var data = new RecordSet();
            reqKey.addField(FieldKey.DATA_VALUE);
            data.put(FieldKey.LOCATION, lKey);
            data.put(FieldKey.DATA_KEY, key);
            data.put(FieldKey.DATA_VALUE, DataUtils.blockDataBase64(val));
            scheduleWriteTask(scopeKey, reqKey, data, true);
        }
    }

    private void scheduleUniversalDataUpdate(ScopeKey scopeKey, RecordKey reqKey, String uuid, String key, String val) {
        if (val == null) {
            scheduleDeleteTask(scopeKey, reqKey, false);
        } else {
            var data = new RecordSet();
            reqKey.addField(FieldKey.DATA_VALUE);
            data.put(FieldKey.UNIVERSAL_UUID, uuid);
            data.put(FieldKey.DATA_KEY, key);
            data.put(FieldKey.DATA_VALUE, DataUtils.blockDataBase64(val));
            scheduleWriteTask(scopeKey, reqKey, data, true);
        }
    }

    void scheduleDelayedChunkDataUpdate(SlimefunChunkData chunkData, String key) {
        var scopeKey = new ChunkKey(DataScope.NONE, chunkData.getChunk());
        var reqKey = new RecordKey(DataScope.CHUNK_DATA);
        reqKey.addCondition(FieldKey.CHUNK, chunkData.getKey());
        reqKey.addCondition(FieldKey.DATA_KEY, key);

        if (enableDelayedSaving) {
            scheduleDelayedUpdateTask(
                    new LinkedKey(scopeKey, reqKey),
                    () -> scheduleChunkDataUpdate(scopeKey, reqKey, chunkData.getKey(), key, chunkData.getData(key)));
        } else {
            scheduleChunkDataUpdate(scopeKey, reqKey, chunkData.getKey(), key, chunkData.getData(key));
        }
    }

    private void scheduleDelayedUpdateTask(LinkedKey key, Runnable run) {
        synchronized (delayedWriteTasks) {
            var task = delayedWriteTasks.get(key);

            if (task != null && !task.isExecuted()) {
                task.setRunAfter(delayedSecond, TimeUnit.SECONDS);
                return;
            }

            task = new DelayedTask(delayedSecond, TimeUnit.SECONDS, run);
            delayedWriteTasks.put(key, task);
        }
    }

    private void scheduleChunkDataUpdate(ScopeKey scopeKey, RecordKey reqKey, String cKey, String key, String val) {
        if (val == null) {
            scheduleDeleteTask(scopeKey, reqKey, false);
        } else {
            var data = new RecordSet();
            reqKey.addField(FieldKey.DATA_VALUE);
            data.put(FieldKey.CHUNK, cKey);
            data.put(FieldKey.DATA_KEY, key);
            data.put(FieldKey.DATA_VALUE, DataUtils.blockDataBase64(val));
            scheduleWriteTask(scopeKey, reqKey, data, false);
        }
    }

    private void executeAllDelayedTasks() {
        synchronized (delayedWriteTasks) {
            delayedWriteTasks.values().forEach(DelayedTask::runUnsafely);
        }
    }

    public SlimefunChunkData getChunkDataFromCache(Location chunk) {
        return getChunkDataCache(chunk, false);
    }

    public SlimefunChunkData getChunkDataFromCache(Chunk chunk) {
        return getChunkDataCache(chunk, false);
    }

    private SlimefunChunkData getChunkDataCache(Chunk chunk, boolean createOnNotExists) {
        return createOnNotExists
                ? loadedChunk.computeIfAbsent(LocationUtils.getChunkKey(chunk), k -> {
                    var re = new SlimefunChunkData(chunk);
                    if (chunkDataLoadMode.readCacheOnly()) {
                        re.setIsDataLoaded(true);
                    }
                    return re;
                })
                : loadedChunk.get(LocationUtils.getChunkKey(chunk));
    }

    // Fixed #935: use cache chunk data to generate chunkKey by location first.
    private SlimefunChunkData getChunkDataCache(Location loc, boolean createOnNotExists) {
        var re = loadedChunk.get(LocationUtils.getChunkKey(loc));
        if (re != null) {
            return re;
        } else {
            // If cache not exists, use `getChunkDataCache` and trigger chunk loading
            return getChunkDataCache(loc.getChunk(), createOnNotExists);
        }
    }

    private void deleteChunkAndBlockDataDirectly(String cKey) {
        var req = new RecordKey(DataScope.BLOCK_RECORD);
        req.addCondition(FieldKey.CHUNK, cKey);
        deleteData(req);

        req = new RecordKey(DataScope.CHUNK_DATA);
        req.addCondition(FieldKey.CHUNK, cKey);
        deleteData(req);
    }

    private void clearBlockCacheAndTasks(SlimefunBlockData blockData) {
        var l = blockData.getLocation();
        if (blockData.isDataLoaded() && Slimefun.getRegistry().getTickerBlocks().contains(blockData.getSfId())) {
            Slimefun.getTickerTask().disableTicker(l);
        }
        Slimefun.getNetworkManager().updateAllNetworks(l);

        var scopeKey = new LocationKey(DataScope.NONE, l);
        removeDelayedDataUpdates(scopeKey);
        abortScopeTask(scopeKey);
    }

    /**
     * 迁移旧 Slimefun 机器数据至通用数据
     */
    private void migrateUniversalData(
            @Nonnull Location l,
            @Nonnull String sfId,
            @Nonnull List<RecordSet> kvData,
            @Nonnull List<RecordSet> invData) {
        try {
            if (l == null || sfId == null) {
                return;
            }

            var universalData = createUniversalBlock(l, sfId);

            Slimefun.runSync(
                    () -> {
                        if (Slimefun.getBlockDataService()
                                .isTileEntity(l.getBlock().getType())) {
                            Slimefun.getBlockDataService()
                                    .updateUniversalDataUUID(l.getBlock(), String.valueOf(universalData.getUUID()));
                        }
                    },
                    10L);

            kvData.forEach(recordSet -> universalData.setData(
                    recordSet.get(FieldKey.DATA_KEY), DataUtils.blockDataDebase64(recordSet.get(FieldKey.DATA_VALUE))));

            var preset = UniversalMenuPreset.getPreset(sfId);
            if (preset != null) {
                final var inv = new ItemStack[54];

                for (RecordSet record : invData) {
                    var slot = record.getInt(FieldKey.INVENTORY_SLOT);
                    try {
                        inv[slot] = record.getItemStack(FieldKey.INVENTORY_ITEM);
                    } catch (Exception ex) {
                        inv[slot] = null;
                        Slimefun.logger()
                                .log(
                                        Level.SEVERE,
                                        "加载目标物品失败, 请检查实际数据 [" + universalData.getKey() + ":" + slot + "]",
                                        ex);
                    }
                }

                universalData.setMenu(new UniversalMenu(preset, universalData.getUUID(), l, inv));

                var content = universalData.getMenuContents();
                if (content != null) {
                    invSnapshots.put(universalData.getKey(), new InvSnapshot(content));
                }
            }

            removeBlockData(l);

            if (Slimefun.getRegistry().getTickerBlocks().contains(universalData.getSfId())) {
                Slimefun.getTickerTask()
                        .enableTicker(universalData.getLastPresent().toLocation(), universalData.getUUID());
            }
        } catch (Exception e) {
            Slimefun.logger().log(Level.WARNING, "迁移机器人数据时出现错误", e);
        }
    }
}
