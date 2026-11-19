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
import io.github.bakedlibs.dough.blocks.BlockPosition;
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
import java.util.concurrent.CopyOnWriteArrayList;
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

    private final Map<BlockPosition, UUID> universalBlockLocationCache;
    /**
     * 每个区块当前在途 (已入队但尚未执行完) 的写任务计数。
     * 区块驱逐时排队的写入全部完成后, 才允许重载读取, 避免读到旧数据。
     */
    private final Map<String, Integer> pendingChunkWrites = new ConcurrentHashMap<>();
    /**
     * 等待区块写任务完成的回调, 在写计数归零时执行。
     */
    private final Map<String, List<Runnable>> chunkWriteCompletionListeners = new ConcurrentHashMap<>();
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
        universalBlockLocationCache = new ConcurrentHashMap<>();
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

        uniData.initLastPresent();
        universalBlockLocationCache.put(new BlockPosition(l), uniData.getUUID());

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

        if (toRemove instanceof SlimefunUniversalBlockData ubd) {
            //            universalBlockLocationCache.remove(ubd.getLastPresent());
            if (ubd.getLastPresent() != null) {
                universalBlockLocationCache.remove(ubd.getLastPresent());
            }
        }
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
        return CompletableFuture.supplyAsync(
                () -> {
                    // The chunk may have been unloaded (and its cache evicted) while the read was queued
                    if (!loadedChunk.containsKey(LocationUtils.getChunkKey(l))) {
                        return null;
                    }
                    return loadBlockData(l);
                },
                this.readExecutor);
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

        var pos = new BlockPosition(l);
        var uuid = universalBlockLocationCache.get(pos);
        if (uuid == null) {
            return Optional.empty();
        }

        var data = loadedUniversalData.get(uuid);
        if (data instanceof SlimefunUniversalBlockData ubd) {
            return Optional.of(ubd);
        }

        universalBlockLocationCache.remove(pos, uuid);
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

            if (uniData.getLastPresent() != null) {
                universalBlockLocationCache.remove(uniData.getLastPresent());
            }

            uniData.setLastPresent(target);

            if (uniData.getLastPresent() != null) {
                universalBlockLocationCache.put(uniData.getLastPresent(), uniData.getUUID());
            }

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
        loadChunkInternal(isNewChunk, forceReadData, getChunkDataCache(chunk, true));
    }

    /**
     * 使用指定的缓存实例加载区块数据。
     * <p>
     * 异步读取路径在调用前必须先确认传入的实例仍然是 {@link #loadedChunk} 中存活的条目,
     * 防止区块卸载驱逐缓存之后, 在途的读取任务把条目重新写回。
     */
    private void loadChunkInternal(boolean isNewChunk, boolean forceReadData, SlimefunChunkData chunkData) {
        if (hasPendingWrites(chunkData.getKey())) {
            // The chunk was recently evicted and the writes queued during eviction have
            // not finished yet. Reading the database now could resurrect stale rows
            // (e.g. items withdrawn before unloading). Defer the load until they complete.
            runAfterChunkWrites(chunkData.getKey(), () -> {
                if (!chunkData.isUnloaded()) {
                    loadChunkInternal(isNewChunk, forceReadData, chunkData);
                }
            });
            return;
        }

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
            if (chunkData.isUnloaded()) {
                // The chunk was unloaded (and its cache evicted) while this read was in flight
                return;
            }

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
                scheduleReadTask(() -> {
                    if (!chunkData.isUnloaded()) {
                        loadBlockData(blockData);
                    }
                });
            }
        });

        if (!chunkData.isUnloaded()) {
            Bukkit.getPluginManager().callEvent(new SlimefunChunkDataLoadEvent(chunkData));

            // Requeue network discovery for every block location in this chunk.
            // Unloading evicted their cache, which made Network#discoverStep drop them
            // from the network's node sets. Without requeueing here, cargo/energy nodes
            // stay disconnected until a full network rebuild.
            var locations = new HashSet<Location>();
            for (var blockData : chunkData.getAllBlockData()) {
                locations.add(blockData.getLocation());
            }
            if (!locations.isEmpty()) {
                var networkManager = Slimefun.getNetworkManager();
                Slimefun.runSync(() -> locations.forEach(networkManager::updateAllNetworks));
            }
        }
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

        var chunkKey = LocationUtils.getChunkKey(blockData.getLocation());
        if (hasPendingWrites(chunkKey)) {
            // Same reasoning as in loadChunkInternal(): wait for the eviction
            // writes to finish before reading, so inventories cannot resurrect
            // items that were withdrawn before the unload.
            runAfterChunkWrites(chunkKey, () -> loadBlockData(blockData));
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

            // The owning chunk may have been unloaded (and its cache evicted) while this
            // read task was queued. Loading into a stale container would write into an
            // evicted cache and re-enable tickers for an unloaded chunk.
            var owner = loadedChunk.get(LocationUtils.getChunkKey(blockData.getLocation()));
            if (owner == null || owner.isUnloaded() || owner.getBlockCacheInternal(blockData.getKey()) != blockData) {
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
        scheduleReadTask(() -> {
            blockDataList.forEach(this::loadBlockData);
            invokeCallback(callback, blockDataList);
        });
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
                        Location loc = LocationUtils.toLocation(lStr);
                        if (loc != null) {
                            ubd.setLastPresent(loc);
                            universalBlockLocationCache.put(ubd.getLastPresent(), ubd.getUUID());
                        }
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
        var cKey = LocationUtils.getChunkKey(chunk);
        return CompletableFuture.runAsync(
                        () -> {
                            // The chunk may have been unloaded (and its cache evicted) while this
                            // read was queued. Only proceed if our entry is still the live one.
                            if (loadedChunk.get(cKey) == cdata && !cdata.isUnloaded() && chunk.isLoaded()) {
                                loadChunkInternal(false, false, cdata);
                            }
                        },
                        readExecutor)
                .thenApply(v -> loadedChunk.get(cKey) == cdata ? cdata : null);
    }

    public void getChunkDataAsync(Chunk chunk, IAsyncReadCallback<SlimefunChunkData> callback) {
        getChunkDataAsync(chunk).whenComplete((result, throwable) -> invokeCallback(callback, result));
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

    /**
     * 处理区块卸载事件。
     * <p>
     * 在 {@link ChunkDataLoadMode#LOAD_WITH_CHUNK} 模式下, 区块卸载时应将缓存中的数据落盘并驱逐,
     * 避免 {@link #loadedChunk} 随着玩家探索的区块数量增长而无上限膨胀。
     * {@link ChunkDataLoadMode#LOAD_ON_STARTUP} 模式的缓存设计为常驻内存, 不随区块卸载而清除。
     *
     * @param chunk 正在卸载的区块
     */
    public void onChunkUnload(Chunk chunk) {
        if (isDestroyed() || chunkDataLoadMode.readCacheOnly()) {
            return;
        }
        evictChunkCache(LocationUtils.getChunkKey(chunk));
    }

    /**
     * 处理世界卸载事件: 驱逐该世界所有仍缓存在 {@link #loadedChunk} 中的区块数据。
     * <p>
     * 注意: 这里只清理内存缓存, 不会删除数据库中的任何数据。
     *
     * @param world 正在卸载的世界
     */
    public void onWorldUnload(World world) {
        if (isDestroyed() || chunkDataLoadMode.readCacheOnly()) {
            return;
        }
        var prefix = world.getName() + ";";
        // Two passes in case an in-flight async read re-created an entry during the first pass.
        for (int pass = 0; pass < 2; pass++) {
            var evictingKeys = new HashSet<String>();
            loadedChunk.forEach((cKey, cache) -> {
                if (cKey.startsWith(prefix)) {
                    evictingKeys.add(cKey);
                }
            });
            if (evictingKeys.isEmpty()) {
                return;
            }
            evictingKeys.forEach(this::evictChunkCache);
        }
    }

    /**
     * 从 {@link #loadedChunk} 中驱逐指定区块的缓存, 并完成落盘与资源清理。
     *
     * @param cKey 区块键 {@link LocationUtils#getChunkKey(Chunk)}
     */
    private void evictChunkCache(String cKey) {
        var cache = loadedChunk.remove(cKey);
        if (cache == null) {
            return;
        }
        evictChunkData(cKey, cache);
    }

    /**
     * 正确驱逐单个区块缓存数据的完整流程:
     * <ol>
     *   <li>标记该实例已卸载, 使在途的异步读取任务失效;</li>
     *   <li>保存脏物品栏并冲刷该区块范围内的延迟写任务, 保证数据落盘;</li>
     *   <li>关闭 ticker、更新网络并清理延迟任务与物品栏快照;</li>
     *   <li>清空块级缓存, 使整个实例可被回收。</li>
     * </ol>
     * 注意: 区块卸载不等于数据删除, 因此这里不会删除数据库中的任何数据。
     *
     * @param cKey  区块键
     * @param cache 待驱逐的区块缓存实例
     */
    private void evictChunkData(String cKey, SlimefunChunkData cache) {
        if (!cache.markUnloaded()) {
            // Already evicted by another unload event
            return;
        }

        // Close any open menu views for this chunk before touching the data.
        // BlockMenu views are virtual inventories independent of vanilla chunks, so they
        // survive the unload. Closing them first lets their final edits go through the
        // close callbacks and the save step below while the cache is still authoritative.
        BlockMenu.closeAllInChunk(cKey);

        var blockDataList = cache.getAllBlockData();
        if (blockDataList.isEmpty()
                && (!cache.isDataLoaded() || cache.getAllData().isEmpty())) {
            // Fast path: an empty chunk holds neither block data nor chunk-scoped kv.
            // Pending delayed tasks scoped to this chunk may still exist though
            // (e.g. the scheduled deletion of its last kv). Flush them, otherwise
            // their closure would keep this instance alive and could delete data
            // written after a reload. flushDelayedTasksInChunk() itself exits
            // early when the global task map is empty.
            flushDelayedTasksInChunk(cKey);
            cache.removeAllCacheInternal();
            return;
        }

        // 1. persist dirty inventories first: they may enqueue delayed inventory update tasks
        for (var blockData : blockDataList) {
            if (blockData.isDataLoaded() && !blockData.isPendingRemove()) {
                var menu = blockData.getBlockMenu();
                if (menu != null && menu.isDirty()) {
                    saveBlockInventory(blockData);
                }
            }
        }

        // 2. flush delayed tasks scoped to this chunk, so no queued task keeps this instance alive
        flushDelayedTasksInChunk(cKey);

        // 3. mark evicted block data as pending-remove, freeze their menus, then clean
        //    up tickers, networks and inventory snapshots.
        //    Delayed tasks scoped to this chunk were already flushed and removed above.
        for (var blockData : blockDataList) {
            // 恢复 isPendingRemove 语义约定: 第三方持有的旧引用将看到 pendingRemove=true,
            // 其后续写入会被捕获缓冲 (setWhilePendingRemove) 而不会覆盖数据库中的新数据。
            blockData.setPendingRemove(true);

            // 冻结无打开视图的菜单 (有视图的已在 closeAllInChunk 中冻结),
            // 使附属持有的旧菜单引用无法再读写, 防止物品复制。
            var menu = blockData.getBlockMenu();
            if (menu != null) {
                menu.detach();
            }

            var location = blockData.getLocation();

            if (blockData.isDataLoaded()
                    && Slimefun.getRegistry().getTickerBlocks().contains(blockData.getSfId())) {
                Slimefun.getTickerTask().disableTicker(location);
            }

            Slimefun.getNetworkManager().updateAllNetworks(location);
            invSnapshots.remove(blockData.getKey());
        }

        // 4. drop the block cache so the whole chunk data instance becomes collectable
        cache.removeAllCacheInternal();
    }

    /**
     * 冲刷指定区块范围内的所有延迟写任务, 使它们立即进入写入队列,
     * 然后从 {@link #delayedWriteTasks} 中移除, 避免任务闭包继续持有已驱逐的区块数据实例。
     *
     * @param cKey 区块键
     */
    private void flushDelayedTasksInChunk(String cKey) {
        if (delayedWriteTasks.isEmpty()) {
            return;
        }

        // Parse once up front so the scan below does not allocate per entry
        var separator = cKey.indexOf(';');
        var worldName = cKey.substring(0, separator);
        var coordinate = cKey.substring(separator + 1);
        var colon = coordinate.indexOf(':');
        var chunkX = Integer.parseInt(coordinate.substring(0, colon));
        var chunkZ = Integer.parseInt(coordinate.substring(colon + 1));

        synchronized (delayedWriteTasks) {
            var iterator = delayedWriteTasks.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (!isDelayedTaskInChunk(entry.getKey(), cKey, worldName, chunkX, chunkZ)) {
                    continue;
                }

                var task = entry.getValue();
                if (!task.isExecuted()) {
                    task.runUnsafely();
                }
                iterator.remove();
            }
        }
    }

    private boolean isDelayedTaskInChunk(LinkedKey linkedKey, String cKey, String worldName, int chunkX, int chunkZ) {
        var scopeKey = linkedKey.getParent();
        if (scopeKey instanceof ChunkKey chunkKey) {
            return chunkKey.getChunkKey().equals(cKey);
        } else if (scopeKey instanceof LocationKey locationKey) {
            var location = locationKey.getLocation();
            var world = location.getWorld();
            if (world == null) {
                // the world of this location is no longer loaded; its pending task cannot
                // belong to a chunk that is being unloaded right now
                return false;
            }
            return world.getName().equals(worldName)
                    && (location.getBlockX() >> 4) == chunkX
                    && (location.getBlockZ() >> 4) == chunkZ;
        }
        return false;
    }

    private boolean hasPendingWrites(String cKey) {
        return pendingChunkWrites.getOrDefault(cKey, 0) > 0;
    }

    /**
     * 等待指定区块的所有在途写任务完成后执行 {@code run}。
     * <p>
     * 非阻塞: 写计数归零时在完成写任务的线程上执行。若等待期间又有新写任务入队,
     * 会继续等待 (见内部 wrapper 的自检逻辑)。
     */
    private void runAfterChunkWrites(String cKey, Runnable run) {
        var wrapper = new Runnable() {
            @Override
            public void run() {
                if (hasPendingWrites(cKey)) {
                    runAfterChunkWrites(cKey, this);
                } else {
                    run.run();
                }
            }
        };

        List<Runnable> toRun = null;
        synchronized (chunkWriteCompletionListeners) {
            if (!hasPendingWrites(cKey)) {
                toRun = List.of(wrapper);
            } else {
                chunkWriteCompletionListeners
                        .computeIfAbsent(cKey, k -> new CopyOnWriteArrayList<>())
                        .add(wrapper);
            }
        }

        if (toRun != null) {
            toRun.forEach(Runnable::run);
        }
    }

    @Override
    protected void onWriteTaskCreated(ScopeKey scopeKey) {
        var cKey = chunkKeyOf(scopeKey);
        if (cKey != null) {
            pendingChunkWrites.merge(cKey, 1, Integer::sum);
        }
    }

    @Override
    protected void onWriteTaskCompleted(ScopeKey scopeKey) {
        var cKey = chunkKeyOf(scopeKey);
        if (cKey == null) {
            return;
        }

        List<Runnable> toRun = null;
        synchronized (chunkWriteCompletionListeners) {
            var remaining = pendingChunkWrites.compute(cKey, (k, v) -> (v == null || v <= 1) ? null : v - 1);
            if (remaining == null) {
                toRun = chunkWriteCompletionListeners.remove(cKey);
            }
        }

        if (toRun != null) {
            for (var run : toRun) {
                try {
                    run.run();
                } catch (Exception | LinkageError x) {
                    logger.log(Level.WARNING, "执行区块写入完成后的延迟加载任务失败: " + cKey, x);
                }
            }
        }
    }

    /**
     * 从一个写任务的作用域键中解析出它所属的区块键, 无法解析时返回 null。
     */
    @Nullable private static String chunkKeyOf(ScopeKey scopeKey) {
        if (scopeKey instanceof ChunkKey chunkKey) {
            return chunkKey.getChunkKey();
        } else if (scopeKey instanceof LocationKey locationKey) {
            var location = locationKey.getLocation();
            var world = location.getWorld();
            if (world == null) {
                return null;
            }
            return LocationUtils.getChunkKey(location);
        } else if (scopeKey instanceof RecordKey recordKey) {
            for (var condition : recordKey.getConditions()) {
                if (condition.getFirstValue() == FieldKey.CHUNK) {
                    return condition.getSecondValue();
                }
            }
        }
        return null;
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
        universalBlockLocationCache.clear();
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
        var scopeKey = new ChunkKey(DataScope.NONE, chunkData.getKey());
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
