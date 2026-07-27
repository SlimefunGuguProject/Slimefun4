package io.github.thebusybiscuit.slimefun4.core.services.stability;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ProfileDataController;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunChunkData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import com.xzavier0722.mc.plugin.slimefun4.storage.event.SlimefunChunkDataLoadEvent;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.core.services.scheduling.TaskHandle;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Automatic and operator-triggered repair service for localized item metadata. */
public final class ItemDoctorService implements Listener {

    private static final int CHUNK_MENU_LOAD_ATTEMPTS = 20;
    private static final long CHUNK_MENU_RETRY_DELAY_TICKS = 2L;

    private final Slimefun plugin;
    private final ItemPresentationDoctor doctor = new ItemPresentationDoctor();
    private final ItemDoctorReport automaticReport = new ItemDoctorReport(true);
    private final AtomicBoolean serverRunActive = new AtomicBoolean();
    private volatile ItemDoctorReport currentReport;
    private volatile ItemDoctorReport lastReport;
    private volatile ServerRun activeRun;
    private volatile boolean shuttingDown;

    public ItemDoctorService(@Nonnull Slimefun plugin) {
        this.plugin = plugin;
    }

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        if (isEnabled()) {
            plugin.getLogger().info("Slimefun item doctor is enabled for safe English presentation repair.");
        } else {
            plugin.getLogger().info("Slimefun item doctor is disabled in config.yml.");
        }
    }

    public boolean isEnabled() {
        return Slimefun.getCfg().getBoolean("stability.item-doctor.enabled");
    }

    public boolean isServerRunActive() {
        return serverRunActive.get();
    }

    public void shutdown() {
        shuttingDown = true;
        ServerRun run = activeRun;
        if (run != null) {
            run.abort();
        }
    }

    public @Nonnull ItemDoctorReport getAutomaticReport() {
        return automaticReport;
    }

    public @Nullable ItemDoctorReport getCurrentReport() {
        return currentReport;
    }

    public @Nullable ItemDoctorReport getLastReport() {
        return lastReport;
    }

    public @Nonnull ItemDoctorReport inspectInventory(@Nonnull Inventory inventory, boolean repair) {
        ItemDoctorReport report = new ItemDoctorReport(repair);
        doctor.repairInventory(inventory, repair, report);
        report.markComplete();
        return report;
    }

    public @Nonnull ItemDoctorReport inspectPlayer(@Nonnull Player player, boolean repair) {
        ItemDoctorReport report = new ItemDoctorReport(repair);
        doctor.repairInventory(player.getInventory(), repair, report);
        doctor.repairInventory(player.getEnderChest(), repair, report);
        report.markComplete();
        return report;
    }

    public @Nonnull ItemDoctorReport inspectItem(@Nonnull ItemStack item, boolean repair) {
        ItemDoctorReport report = new ItemDoctorReport(repair);
        doctor.inspectItem(item, repair, report);
        report.markComplete();
        return report;
    }

    /**
     * Starts a batched scan or repair of online players, loaded storage, machines, and all database backpacks.
     *
     * @return {@code false} when another server-wide run is already active
     */
    public boolean startServerRun(boolean repair, @Nonnull Consumer<ItemDoctorReport> completion) {
        if (shuttingDown || !isEnabled() || !serverRunActive.compareAndSet(false, true)) {
            return false;
        }

        ItemDoctorReport report = new ItemDoctorReport(repair);
        currentReport = report;
        ServerRun run = new ServerRun(report, completion);
        activeRun = run;
        try {
            run.collectLoadedInventories();
            run.startInventoryTask();
            run.startBackpackTask();
        } catch (RuntimeException ex) {
            report.failure();
            run.abort();
            plugin.getLogger().log(Level.SEVERE, "The Slimefun item doctor could not start safely.", ex);
            try {
                completion.accept(report);
            } catch (RuntimeException callbackError) {
                plugin.getLogger().log(Level.WARNING, "Item doctor completion callback failed.", callbackError);
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (shuttingDown
                || !isEnabled()
                || !Slimefun.getCfg().getBoolean("stability.item-doctor.repair-player-on-join")) {
            return;
        }
        Player player = event.getPlayer();
        Slimefun.getSchedulerService().runForLater(player, () -> {
            if (player.isOnline()) {
                repairAutomatic(automaticReport, player.getInventory(), player.getEnderChest());
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (shuttingDown
                || !isEnabled()
                || !Slimefun.getCfg().getBoolean("stability.item-doctor.repair-opened-inventories")) {
            return;
        }
        Slimefun.getSchedulerService().runFor(event.getPlayer(), () -> {
            repairAutomatic(automaticReport, event.getInventory(), event.getPlayer().getInventory());
            try {
                ItemStack cursor = event.getPlayer().getItemOnCursor();
                if (doctor.inspectItem(cursor, true, automaticReport)) {
                    event.getPlayer().setItemOnCursor(cursor);
                }
            } catch (RuntimeException ex) {
                automaticReport.failure();
                plugin.getLogger().log(Level.WARNING, "Item doctor could not repair the cursor item.", ex);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (shuttingDown
                || !isEnabled()
                || !Slimefun.getCfg().getBoolean("stability.item-doctor.repair-chunks-on-load")) {
            return;
        }

        Chunk chunk = event.getChunk();
        Slimefun.getSchedulerService().runAt(chunk.getBlock(0, 0, 0).getLocation(), () -> {
            if (!shuttingDown && chunk.isLoaded()) {
                repairChunk(chunk, automaticReport);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSlimefunChunkDataLoad(SlimefunChunkDataLoadEvent event) {
        if (shuttingDown
                || !isEnabled()
                || !Slimefun.getCfg().getBoolean("stability.item-doctor.repair-chunks-on-load")) {
            return;
        }

        scheduleSlimefunMenuRepair(event.getChunkData(), CHUNK_MENU_LOAD_ATTEMPTS);
    }

    private void scheduleSlimefunMenuRepair(SlimefunChunkData chunkData, int attemptsRemaining) {
        Slimefun.getSchedulerService().runAtLater(
                chunkData.getChunk().getBlock(0, 0, 0).getLocation(),
                () -> {
                    if (shuttingDown || !chunkData.getChunk().isLoaded()) {
                        return;
                    }

                    if (!areBlockInventoriesLoaded(chunkData) && attemptsRemaining > 0) {
                        scheduleSlimefunMenuRepair(chunkData, attemptsRemaining - 1);
                        return;
                    }

                    BlockDataController controller = Slimefun.getDatabaseManager().getBlockDataController();
                    repairSlimefunChunkMenus(controller, chunkData, automaticReport);
                },
                CHUNK_MENU_RETRY_DELAY_TICKS);
    }

    private boolean areBlockInventoriesLoaded(SlimefunChunkData chunkData) {
        for (SlimefunBlockData blockData : chunkData.getAllBlockData()) {
            if (!blockData.isDataLoaded()) {
                return false;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)
                || shuttingDown
                || !isEnabled()
                || !Slimefun.getCfg().getBoolean("stability.item-doctor.repair-picked-up-items")) {
            return;
        }

        Item itemEntity = event.getItem();
        try {
            ItemStack item = itemEntity.getItemStack();
            if (doctor.inspectItem(item, true, automaticReport)) {
                itemEntity.setItemStack(item);
            }
        } catch (RuntimeException ex) {
            automaticReport.failure();
            plugin.getLogger().log(Level.WARNING, "Item doctor could not repair a picked-up item.", ex);
        }
    }

    private void repairAutomatic(ItemDoctorReport report, Inventory... inventories) {
        for (Inventory inventory : inventories) {
            if (inventory == null) {
                continue;
            }
            try {
                doctor.repairInventory(inventory, true, report);
            } catch (RuntimeException ex) {
                report.failure();
                plugin.getLogger().log(Level.WARNING, "Item doctor could not repair an automatic inventory.", ex);
            }
        }
    }

    private void repairChunk(Chunk chunk, ItemDoctorReport report) {
        Set<Inventory> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof InventoryHolder holder && seen.add(holder.getInventory())) {
                repairAutomatic(report, holder.getInventory());
            }
        }
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof InventoryHolder holder && seen.add(holder.getInventory())) {
                repairAutomatic(report, holder.getInventory());
            } else if (entity instanceof Item itemEntity) {
                try {
                    ItemStack item = itemEntity.getItemStack();
                    if (doctor.inspectItem(item, true, report)) {
                        itemEntity.setItemStack(item);
                    }
                } catch (RuntimeException ex) {
                    report.failure();
                    plugin.getLogger().log(Level.WARNING, "Item doctor could not repair a dropped item.", ex);
                }
            }
        }
    }

    private void repairSlimefunChunkMenus(
            BlockDataController controller, SlimefunChunkData chunkData, ItemDoctorReport report) {
        for (SlimefunBlockData blockData : chunkData.getAllBlockData()) {
            BlockMenu menu = blockData.getBlockMenu();
            if (menu == null) {
                continue;
            }
            try {
                doctor.repairInventory(menu.toInventory(), true, report);
                // Always reconcile the database snapshot. The physical inventory pass may have
                // repaired the same menu before its database-backed representation was reached.
                controller.saveBlockInventory(blockData);
            } catch (RuntimeException ex) {
                report.failure();
                plugin.getLogger().log(Level.WARNING, "Item doctor could not repair a Slimefun block menu.", ex);
            }
        }
    }

    private final class ServerRun {
        private final ItemDoctorReport report;
        private final Consumer<ItemDoctorReport> completion;
        private final Queue<Player> players = new ConcurrentLinkedQueue<>();
        private final Queue<InventoryTarget> inventories = new ConcurrentLinkedQueue<>();
        private final Queue<Item> droppedItems = new ConcurrentLinkedQueue<>();
        private final Queue<SlimefunChunkData> slimefunChunks = new ConcurrentLinkedQueue<>();
        private final Queue<SlimefunUniversalData> universalData = new ConcurrentLinkedQueue<>();
        private final Queue<Chunk> physicalChunks = new ConcurrentLinkedQueue<>();
        private final Map<Inventory, InventoryTarget> inventoryTargets = new IdentityHashMap<>();
        private final AtomicInteger pendingOwnedWork = new AtomicInteger();
        private volatile boolean inventoriesDone;
        private volatile boolean backpacksDone;
        private volatile boolean aborted;
        private volatile TaskHandle inventoryTask;
        private Iterator<String> backpackIds = Collections.emptyIterator();

        private ServerRun(ItemDoctorReport report, Consumer<ItemDoctorReport> completion) {
            this.report = report;
            this.completion = completion;
        }

        private void collectLoadedInventories() {
            players.addAll(Bukkit.getOnlinePlayers());

            // Queue database-backed and physical storage for incremental collection. This keeps
            // the command responsive even when a server has thousands of loaded machines/chunks.
            BlockDataController controller = Slimefun.getDatabaseManager().getBlockDataController();
            slimefunChunks.addAll(controller.getAllLoadedChunkData());
            universalData.addAll(controller.getAllLoadedUniversalData());

            for (World world : Bukkit.getWorlds()) {
                Collections.addAll(physicalChunks, world.getLoadedChunks());
            }
        }

        private void collectChunk(Chunk chunk) {
            for (BlockState state : chunk.getTileEntities()) {
                if (state instanceof InventoryHolder holder) {
                    addInventory(holder.getInventory(), null, null, state.getLocation());
                }
            }
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof InventoryHolder holder) {
                    addInventory(holder.getInventory(), null, entity, null);
                } else if (entity instanceof Item itemEntity) {
                    droppedItems.add(itemEntity);
                }
            }
        }

        private void collectSlimefunChunk(SlimefunChunkData chunkData) {
            BlockDataController controller = Slimefun.getDatabaseManager().getBlockDataController();
            for (SlimefunBlockData blockData : chunkData.getAllBlockData()) {
                BlockMenu menu = blockData.getBlockMenu();
                if (menu != null) {
                    addInventory(
                            menu.toInventory(),
                            () -> controller.saveBlockInventory(blockData),
                            null,
                            blockData.getLocation());
                }
            }
        }

        private void collectUniversalData(SlimefunUniversalData data) {
            var menu = data.getMenu();
            if (menu != null) {
                BlockDataController controller = Slimefun.getDatabaseManager().getBlockDataController();
                addInventory(menu.toInventory(), () -> controller.saveUniversalInventory(data), null, null);
            }
        }

        private void addInventory(
                Inventory inventory,
                @Nullable Runnable saveAction,
                @Nullable Entity ownerEntity,
                @Nullable Location ownerLocation) {
            if (inventory == null) {
                return;
            }

            synchronized (inventoryTargets) {
                InventoryTarget existing = inventoryTargets.get(inventory);
                if (existing != null) {
                    existing.merge(saveAction, ownerEntity, ownerLocation);
                    return;
                }

                InventoryTarget target = new InventoryTarget(inventory, saveAction, ownerEntity, ownerLocation);
                inventoryTargets.put(inventory, target);
                inventories.add(target);
            }
        }

        private void startInventoryTask() {
            int perTick = Math.max(1, Slimefun.getCfg().getInt("stability.item-doctor.inventories-per-tick"));
            inventoryTask = Slimefun.getSchedulerService()
                    .runAtFixedRate(() -> processInventoryBatch(perTick), 1L, 1L);
        }

        private void processInventoryBatch(int perTick) {
            if (aborted || shuttingDown) {
                cancelInventoryTask();
                return;
            }

            for (int i = 0; i < perTick; i++) {
                Player player = players.poll();
                if (player != null) {
                    dispatchFor(player, () -> {
                        if (player.isOnline()) {
                            addInventory(player.getInventory(), null, player, null);
                            addInventory(player.getEnderChest(), null, player, null);
                        }
                    });
                    continue;
                }

                InventoryTarget target = inventories.poll();
                if (target != null) {
                    scheduleInventoryTarget(target);
                    continue;
                }

                Item itemEntity = droppedItems.poll();
                if (itemEntity != null) {
                    dispatchFor(itemEntity, () -> inspectDroppedItem(itemEntity));
                    continue;
                }

                SlimefunChunkData slimefunChunk = slimefunChunks.poll();
                if (slimefunChunk != null) {
                    Location owner = slimefunChunk.getChunk().getBlock(0, 0, 0).getLocation();
                    dispatchAt(owner, () -> collectSlimefunChunk(slimefunChunk));
                    continue;
                }

                SlimefunUniversalData data = universalData.poll();
                if (data != null) {
                    dispatchGlobal(() -> collectUniversalData(data));
                    continue;
                }

                Chunk physicalChunk = physicalChunks.poll();
                if (physicalChunk != null) {
                    Location owner = physicalChunk.getBlock(0, 0, 0).getLocation();
                    dispatchAt(owner, () -> {
                        if (physicalChunk.isLoaded()) {
                            collectChunk(physicalChunk);
                        }
                    });
                    continue;
                }

                tryFinishInventoryPhase();
                return;
            }
        }

        private void cancelInventoryTask() {
            TaskHandle task = inventoryTask;
            if (task != null) {
                task.cancel();
            }
        }

        private void scheduleInventoryTarget(InventoryTarget target) {
            Entity ownerEntity = target.ownerEntity();
            if (ownerEntity != null) {
                dispatchFor(ownerEntity, () -> inspectInventoryTarget(target));
                return;
            }

            Location ownerLocation = target.ownerLocation();
            if (ownerLocation != null) {
                dispatchAt(ownerLocation, () -> inspectInventoryTarget(target));
                return;
            }

            dispatchGlobal(() -> inspectInventoryTarget(target));
        }

        private void dispatchGlobal(Runnable work) {
            dispatchOwnedWork(work, (task, retired) -> Slimefun.getSchedulerService().run(task));
        }

        private void dispatchAt(Location location, Runnable work) {
            dispatchOwnedWork(
                    work,
                    (task, retired) -> Slimefun.getSchedulerService().runAt(location, task));
        }

        private void dispatchFor(Entity entity, Runnable work) {
            dispatchOwnedWork(
                    work,
                    (task, retired) -> Slimefun.getSchedulerService().runFor(entity, task, retired));
        }

        private void dispatchOwnedWork(
                Runnable work, BiFunction<Runnable, Runnable, TaskHandle> scheduler) {
            pendingOwnedWork.incrementAndGet();
            AtomicBoolean completed = new AtomicBoolean();
            Runnable completion = () -> {
                if (completed.compareAndSet(false, true)) {
                    pendingOwnedWork.decrementAndGet();
                }
            };
            Runnable trackedWork = () -> {
                try {
                    if (!aborted && !shuttingDown) {
                        work.run();
                    }
                } finally {
                    completion.run();
                }
            };

            try {
                TaskHandle scheduled = scheduler.apply(trackedWork, completion);
                if (scheduled.isCancelled()) {
                    completion.run();
                }
            } catch (RuntimeException | LinkageError ex) {
                completion.run();
                report.failure();
                plugin.getLogger().log(Level.WARNING, "Item doctor could not dispatch owned work.", ex);
            }
        }

        private void tryFinishInventoryPhase() {
            if (pendingOwnedWork.get() != 0
                    || !players.isEmpty()
                    || !inventories.isEmpty()
                    || !droppedItems.isEmpty()
                    || !slimefunChunks.isEmpty()
                    || !universalData.isEmpty()
                    || !physicalChunks.isEmpty()) {
                return;
            }

            inventoriesDone = true;
            cancelInventoryTask();
            finishIfReady();
        }

        private void inspectInventoryTarget(InventoryTarget target) {
            synchronized (inventoryTargets) {
                inventoryTargets.remove(target.inventory());
            }
            try {
                boolean changed = doctor.repairInventory(target.inventory(), report.isRepairMode(), report);
                if (changed && target.saveAction() != null) {
                    target.saveAction().run();
                }
            } catch (RuntimeException ex) {
                report.failure();
                plugin.getLogger().log(Level.WARNING, "Item doctor failed to inspect an inventory.", ex);
            }
        }

        private void inspectDroppedItem(Item itemEntity) {
            try {
                if (!itemEntity.isValid()) {
                    return;
                }
                ItemStack item = itemEntity.getItemStack();
                if (doctor.inspectItem(item, report.isRepairMode(), report)) {
                    itemEntity.setItemStack(item);
                }
            } catch (RuntimeException ex) {
                report.failure();
                plugin.getLogger().log(Level.WARNING, "Item doctor failed to inspect a dropped item.", ex);
            }
        }

        private void startBackpackTask() {
            ProfileDataController controller = Slimefun.getDatabaseManager().getProfileDataController();
            controller.getAllBackpackIdsAsync().whenComplete((ids, error) -> {
                if (aborted || shuttingDown) {
                    return;
                }
                Slimefun.getSchedulerService().run(() -> {
                    if (aborted || shuttingDown) {
                        return;
                    }
                    if (error != null) {
                        report.failure();
                        plugin.getLogger()
                                .log(
                                        Level.WARNING,
                                        "Item doctor could not enumerate stored backpacks.",
                                        error);
                        backpacksDone = true;
                        finishIfReady();
                        return;
                    }

                    backpackIds = ids.iterator();
                    processNextBackpack();
                });
            });
        }

        private void processNextBackpack() {
            if (aborted || shuttingDown) {
                return;
            }
            if (!backpackIds.hasNext()) {
                backpacksDone = true;
                finishIfReady();
                return;
            }

            String id = backpackIds.next();
            ProfileDataController controller = Slimefun.getDatabaseManager().getProfileDataController();
            controller.getBackpackForMaintenanceAsync(id).whenComplete((loadedBackpack, error) -> {
                if (aborted || shuttingDown) {
                    releaseMaintenanceBackpack(controller, loadedBackpack);
                    return;
                }
                TaskHandle scheduled = Slimefun.getSchedulerService().run(() -> {
                    if (aborted || shuttingDown) {
                        releaseMaintenanceBackpack(controller, loadedBackpack);
                        return;
                    }
                    if (error != null) {
                        report.failure();
                        plugin.getLogger()
                                .log(
                                        Level.WARNING,
                                        "Item doctor could not load backpack " + id + '.',
                                        error);
                    } else if (loadedBackpack != null) {
                        repairBackpack(
                                controller,
                                loadedBackpack.backpack(),
                                loadedBackpack.maintenanceOwned());
                    }
                    processNextBackpack();
                });
                if (scheduled.isCancelled() && !plugin.isEnabled()) {
                    releaseMaintenanceBackpack(controller, loadedBackpack);
                }
            });
        }

        private void releaseMaintenanceBackpack(
                ProfileDataController controller,
                @Nullable ProfileDataController.MaintenanceBackpack loadedBackpack) {
            if (loadedBackpack != null && loadedBackpack.maintenanceOwned()) {
                controller.releaseMaintenanceBackpack(loadedBackpack.backpack());
            }
        }

        private void repairBackpack(
                ProfileDataController controller, PlayerBackpack backpack, boolean maintenanceLoaded) {
            try {
                if (backpack.isInvalid()) {
                    return;
                }
                report.backpackScanned();
                boolean changed = doctor.repairInventory(backpack.getInventory(), report.isRepairMode(), report);
                if (changed) {
                    controller.saveBackpackInventory(backpack);
                }
            } catch (RuntimeException ex) {
                report.failure();
                plugin.getLogger().log(
                        Level.WARNING,
                        "Item doctor failed to inspect backpack " + backpack.getUniqueId() + '.',
                        ex);
            } finally {
                if (maintenanceLoaded) {
                    controller.releaseMaintenanceBackpack(backpack);
                }
            }
        }

        private synchronized void abort() {
            if (aborted) {
                return;
            }

            aborted = true;
            cancelInventoryTask();
            if (!report.isComplete()) {
                report.markComplete();
            }
            currentReport = null;
            lastReport = report;
            activeRun = null;
            serverRunActive.set(false);
        }

        private synchronized void finishIfReady() {
            if (aborted || !inventoriesDone || !backpacksDone || report.isComplete()) {
                return;
            }

            report.markComplete();
            currentReport = null;
            lastReport = report;
            activeRun = null;
            serverRunActive.set(false);
            logCompletion(report);
            try {
                completion.accept(report);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING, "Item doctor completion callback failed.", ex);
            }
        }
    }

    private void logCompletion(ItemDoctorReport report) {
        plugin.getLogger().info("Slimefun item doctor " + report.getModeName() + " completed: "
                + report.getScannedStacks() + " stacks scanned, "
                + report.getCjkStacks() + " with Chinese presentation, "
                + report.getRepairedStacks() + " repaired, "
                + report.getFailures() + " failures.");

        if (!report.getUnknownIdSamples().isEmpty()) {
            plugin.getLogger().warning("Item doctor skipped unknown Slimefun IDs: "
                    + String.join(", ", report.getUnknownIdSamples()));
        }
        if (!report.getUnresolvedTemplateSamples().isEmpty()) {
            plugin.getLogger()
                    .warning(
                            "Item doctor found registered templates that still contain CJK text or unsafe state: "
                                    + String.join(", ", report.getUnresolvedTemplateSamples()));
        }
    }

    private static final class InventoryTarget {
        private final Inventory inventory;
        private volatile Runnable saveAction;
        private volatile Entity ownerEntity;
        private volatile Location ownerLocation;

        private InventoryTarget(
                Inventory inventory,
                @Nullable Runnable saveAction,
                @Nullable Entity ownerEntity,
                @Nullable Location ownerLocation) {
            this.inventory = inventory;
            this.saveAction = saveAction;
            this.ownerEntity = ownerEntity;
            this.ownerLocation = ownerLocation;
        }

        private Inventory inventory() {
            return inventory;
        }

        private @Nullable Runnable saveAction() {
            return saveAction;
        }

        private @Nullable Entity ownerEntity() {
            return ownerEntity;
        }

        private @Nullable Location ownerLocation() {
            return ownerLocation;
        }

        private synchronized void merge(
                @Nullable Runnable additionalAction,
                @Nullable Entity additionalEntity,
                @Nullable Location additionalLocation) {
            if (additionalAction != null && additionalAction != saveAction) {
                if (saveAction == null) {
                    saveAction = additionalAction;
                } else {
                    Runnable previousAction = saveAction;
                    saveAction = () -> {
                        previousAction.run();
                        additionalAction.run();
                    };
                }
            }

            if (ownerEntity == null && additionalEntity != null) {
                ownerEntity = additionalEntity;
                ownerLocation = null;
            } else if (ownerEntity == null && ownerLocation == null && additionalLocation != null) {
                ownerLocation = additionalLocation;
            }
        }
    }
}
