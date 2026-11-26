package com.xzavier0722.mc.plugin.slimefun4.storage.task;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.LinkedKey;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelayedSavingLooperTask implements Runnable {
    private final long forceSavePeriodInMillis;
    private final Supplier<Map<LinkedKey, DelayedTask>> taskGetter;
    private final BiConsumer<LinkedKey, DelayedTask> executeCallback;
    private long nextForceRun;

    /**
     * @param forceSavePeriod: force save period in second
     */
    public DelayedSavingLooperTask(
            int forceSavePeriod,
            Supplier<Map<LinkedKey, DelayedTask>> taskGetter,
            BiConsumer<LinkedKey, DelayedTask> executeCallback) {
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
                    executeCallback.accept(key, task);
                }
            });
        } else {
            updateNextForceRunTime();
            tasks.forEach((key, task) -> {
                task.runUnsafely();
                executeCallback.accept(key, task);
            });
        }
    }

    private void updateNextForceRunTime() {
        nextForceRun = System.currentTimeMillis() + forceSavePeriodInMillis;
    }
}
