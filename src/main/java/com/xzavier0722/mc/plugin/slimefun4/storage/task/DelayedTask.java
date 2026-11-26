package com.xzavier0722.mc.plugin.slimefun4.storage.task;

import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DelayedTask {
    private final Runnable task;
    private long runAfter = 0;
    private boolean executed = false;

    public DelayedTask(long delay, TimeUnit unit, Runnable task) {
        this.task = task;
        setRunAfter(delay, unit);
    }

    public synchronized void setRunAfter(long delay, TimeUnit unit) {
        runAfter = System.currentTimeMillis() + unit.toMillis(delay);
    }

    public synchronized boolean tryRun() {
        if (executed) {
            // Already executed (e.g. flushed on chunk eviction); a looper snapshot may
            // still hold this task, but re-running it would write stale values.
            return false;
        }

        if (System.currentTimeMillis() < runAfter) {
            return false;
        }

        try {
            executed = true;
            task.run();
            return true;
        } catch (Exception e) {
            log.warn("An error occurred while running delayed task", e);
            return false;
        }
    }

    public synchronized boolean isExecuted() {
        return executed;
    }

    public synchronized void runUnsafely() {
        if (executed) {
            // Skip double execution: the task closure captures the original data
            // container, so running it twice can overwrite reloaded state.
            return;
        }

        try {
            executed = true;
            task.run();
        } catch (Exception e) {
            log.warn("An error occurred while running delayed task", e);
        }
    }
}
