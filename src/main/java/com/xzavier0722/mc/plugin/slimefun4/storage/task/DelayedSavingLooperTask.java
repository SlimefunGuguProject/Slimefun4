package com.xzavier0722.mc.plugin.slimefun4.storage.task;

import com.xzavier0722.mc.plugin.slimefun4.storage.common.ScopeKey;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DelayedSavingLooperTask implements Runnable {
    private final long forceSavePeriodInMillis;
    private final Supplier<Map<ScopeKey, DelayedTask>> taskGetter;
    private final Consumer<ScopeKey> executeCallback;
    private long nextForceRun;

    /**
     * @param forceSavePeriod: force save period in second
     */
    public DelayedSavingLooperTask(
            int forceSavePeriod, Supplier<Map<ScopeKey, DelayedTask>> taskGetter, Consumer<ScopeKey> executeCallback) {
        this.forceSavePeriodInMillis = forceSavePeriod * 1000L;
        this.executeCallback = executeCallback;
        this.taskGetter = taskGetter;
        updateNextForceRunTime();
    }

    @Override
    public void run() {
        var tasks = taskGetter.get();
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        if (nextForceRun > System.currentTimeMillis()) {
            tasks.forEach((key, task) -> {
                if (task.tryRun()) {
                    executeCallback.accept(key);
                }
            });
        } else {
            updateNextForceRunTime();
            tasks.forEach((key, task) -> {
                task.runUnsafely();
                executeCallback.accept(key);
            });
        }
    }

    private void updateNextForceRunTime() {
        nextForceRun = System.currentTimeMillis() + forceSavePeriodInMillis;
    }
}
