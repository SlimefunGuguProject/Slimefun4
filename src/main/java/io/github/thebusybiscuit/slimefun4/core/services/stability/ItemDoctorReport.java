package io.github.thebusybiscuit.slimefun4.core.services.stability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;

/** Thread-safe progress and result counters for an item doctor run. */
public final class ItemDoctorReport {

    private static final int SAMPLE_LIMIT = 12;

    private final boolean repairMode;
    private final long startedAtNanos = System.nanoTime();
    private final AtomicBoolean complete = new AtomicBoolean();
    private final AtomicLong inventories = new AtomicLong();
    private final AtomicLong backpacks = new AtomicLong();
    private final AtomicLong scannedStacks = new AtomicLong();
    private final AtomicLong slimefunStacks = new AtomicLong();
    private final AtomicLong cjkStacks = new AtomicLong();
    private final AtomicLong repairedStacks = new AtomicLong();
    private final AtomicLong unknownIds = new AtomicLong();
    private final AtomicLong unresolvedTemplates = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final Set<String> unknownIdSamples = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Set<String> unresolvedTemplateSamples = Collections.synchronizedSet(new LinkedHashSet<>());
    private volatile long completedAtNanos;

    public ItemDoctorReport(boolean repairMode) {
        this.repairMode = repairMode;
    }

    void inventoryScanned() {
        inventories.incrementAndGet();
    }

    void backpackScanned() {
        backpacks.incrementAndGet();
    }

    void stackScanned() {
        scannedStacks.incrementAndGet();
    }

    void slimefunStackFound() {
        slimefunStacks.incrementAndGet();
    }

    void cjkStackFound() {
        cjkStacks.incrementAndGet();
    }

    void stackRepaired() {
        repairedStacks.incrementAndGet();
    }

    void unknownIdFound(@Nonnull String itemId) {
        unknownIds.incrementAndGet();
        addSample(unknownIdSamples, itemId);
    }

    void unresolvedTemplateFound(@Nonnull String itemId) {
        unresolvedTemplates.incrementAndGet();
        addSample(unresolvedTemplateSamples, itemId);
    }

    void failure() {
        failures.incrementAndGet();
    }

    void markComplete() {
        if (complete.compareAndSet(false, true)) {
            completedAtNanos = System.nanoTime();
        }
    }

    public boolean isRepairMode() {
        return repairMode;
    }

    public @Nonnull String getModeName() {
        return repairMode ? "repair" : "scan";
    }

    public boolean isComplete() {
        return complete.get();
    }

    public long getInventories() {
        return inventories.get();
    }

    public long getBackpacks() {
        return backpacks.get();
    }

    public long getScannedStacks() {
        return scannedStacks.get();
    }

    public long getSlimefunStacks() {
        return slimefunStacks.get();
    }

    public long getCjkStacks() {
        return cjkStacks.get();
    }

    public long getRepairedStacks() {
        return repairedStacks.get();
    }

    public long getUnknownIds() {
        return unknownIds.get();
    }

    public long getUnresolvedTemplates() {
        return unresolvedTemplates.get();
    }

    public long getFailures() {
        return failures.get();
    }

    public long getDurationMillis() {
        long end = isComplete() ? completedAtNanos : System.nanoTime();
        return Math.max(0L, (end - startedAtNanos) / 1_000_000L);
    }

    public @Nonnull List<String> getUnknownIdSamples() {
        return snapshot(unknownIdSamples);
    }

    public @Nonnull List<String> getUnresolvedTemplateSamples() {
        return snapshot(unresolvedTemplateSamples);
    }

    private static void addSample(Set<String> samples, String value) {
        synchronized (samples) {
            if (samples.size() < SAMPLE_LIMIT) {
                samples.add(value);
            }
        }
    }

    private static List<String> snapshot(Set<String> samples) {
        synchronized (samples) {
            return Collections.unmodifiableList(new ArrayList<>(samples));
        }
    }
}
