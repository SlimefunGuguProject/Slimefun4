package io.github.thebusybiscuit.slimefun4.core.services.stability;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ProfileDataController;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunChunkData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunUniversalData;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerBackpack;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/** Automatic and operator-triggered repair service for localized item metadata. */
public final class ItemDoctorService implements Listener {

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
        Slimefun.runSync(() -> {
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
        Slimefun.runSync(() -> {
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
        Slimefun.runSync(() -> repairChunk(chunk, automaticReport));
        BlockDataController controller = Slimefun.getDatabaseManager().getBlockDataController();
        controller.getChunkDataAsync(chunk).whenComplete((chunkData, error) -> {
            if (shuttingDown) {
                return;
            }
            if (error != null) {
                automaticReport.failure();
                plugin.getLogger()
                        .log(
                                Level.WARNING,
                                "Item doctor could not inspect Slimefun storage in a chunk.",
                                error);
                return;
            }
            if (chunkData != null) {
                Slimefun.runSync(() -> repairSlimefunChunkMenus(controller, chunkData, automaticReport));
            }
        });
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
        private final Queue<InventoryTarget> inventories = new ArrayDeque<>();
        private final Queue<Item> droppedItems = new ArrayDeque<>();
        private final Queue<SlimefunChunkData> slimefunChunks = new ArrayDeque<>();
        private final Queue<SlimefunUniversalData> universalData = new ArrayDeque<>();
        private final Queue<Chunk> physicalChunks = new ArrayDeque<>();
        private final Map<Inventory, InventoryTarget> inventoryTargets = new IdentityHashMap<>();
        private volatile boolean inventoriesDone;
        private volatile boolean backpacksDone;
        private volatile boolean aborted;
        private volatile BukkitTask inventoryTask;
        private Iterator<String> backpackIds = Collections.emptyIterator();

        private ServerRun(ItemDoctorReport report, Consumer<ItemDoctorReport> completion) {
            this.report = report;
            this.completion = completion;
        }

        private void collectLoadedInventories() {
            for (Player player : Bukkit.getOnlinePlayers()) {
                addInventory(player.getInventory(), null);
                addInventory(player.getEnderChest(), null);
            }

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
                    addInventory(holder.getInventory(), null);
                }
            }
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof InventoryHolder holder) {
                    addInventory(holder.getInventory(), null);
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
                    addInventory(menu.toInventory(), () -> controller.saveBlockInventory(blockData));
                }
            }
        }

        private void collectUniversalData(SlimefunUniversalData data) {
            var menu = data.getMenu();
            if (menu != null) {
                BlockDataController controller = Slimefun.getDatabaseManager().getBlockDataController();
                addInventory(menu.toInventory(), () -> controller.saveUniversalInventory(data));
            }
        }

        private void addInventory(Inventory inventory, @Nullable Runnable saveAction) {
            if (inventory == null) {
                return;
            }

            InventoryTarget existing = inventoryTargets.get(inventory);
            if (existing != null) {
                existing.addSaveAction(saveAction);
                return;
            }

            InventoryTarget target = new InventoryTarget(inventory, saveAction);
            inventoryTargets.put(inventory, target);
            inventories.add(target);
        }

        private void startInventoryTask() {
            int perTick = Math.max(1, Slimefun.getCfg().getInt("stability.item-doctor.inventories-per-tick"));
            inventoryTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (aborted || shuttingDown) {
                        cancel();
                        return;
                    }
                    for (int i = 0; i < perTick; i++) {
                        InventoryTarget target = inventories.poll();
                        if (target != null) {
                            inspectInventoryTarget(target);
                            continue;
                        }

                        Item itemEntity = droppedItems.poll();
                        if (itemEntity != null) {
                            inspectDroppedItem(itemEntity);
                            continue;
                        }

                        SlimefunChunkData slimefunChunk = slimefunChunks.poll();
                        if (slimefunChunk != null) {
                            collectSlimefunChunk(slimefunChunk);
                            continue;
                        }

                        SlimefunUniversalData data = universalData.poll();
                        if (data != null) {
                            collectUniversalData(data);
                            continue;
                        }

                        Chunk physicalChunk = physicalChunks.poll();
                        if (physicalChunk != null) {
                            if (physicalChunk.isLoaded()) {
                                collectChunk(physicalChunk);
                            }
                            continue;
                        }

                        inventoriesDone = true;
                        cancel();
                        finishIfReady();
                        return;
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }

        private void inspectInventoryTarget(InventoryTarget target) {
            inventoryTargets.remove(target.inventory());
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
                Slimefun.runSync(() -> {
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
                BukkitTask scheduled = Slimefun.runSync(() -> {
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
                if (scheduled == null && !plugin.isEnabled()) {
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
            BukkitTask task = inventoryTask;
            if (task != null) {
                task.cancel();
            }
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
        private Runnable saveAction;

        private InventoryTarget(Inventory inventory, @Nullable Runnable saveAction) {
            this.inventory = inventory;
            this.saveAction = saveAction;
        }

        private Inventory inventory() {
            return inventory;
        }

        private @Nullable Runnable saveAction() {
            return saveAction;
        }

        private void addSaveAction(@Nullable Runnable additionalAction) {
            if (additionalAction == null || additionalAction == saveAction) {
                return;
            }
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
    }
}
