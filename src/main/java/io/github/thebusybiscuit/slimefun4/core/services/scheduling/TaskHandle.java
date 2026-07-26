package io.github.thebusybiscuit.slimefun4.core.services.scheduling;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunAPI;

/**
 * A server-scheduler-neutral handle for a scheduled task.
 */
@SlimefunAPI
public interface TaskHandle {

    /**
     * Cancels this task. Calling this method more than once is safe.
     */
    void cancel();

    /**
     * Returns whether this task has been cancelled.
     *
     * @return Whether this task is cancelled
     */
    boolean isCancelled();
}
