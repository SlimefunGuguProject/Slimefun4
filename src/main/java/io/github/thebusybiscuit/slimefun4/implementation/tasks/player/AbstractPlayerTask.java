package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import io.github.thebusybiscuit.slimefun4.core.services.scheduling.TaskHandle;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import javax.annotation.Nonnull;
import org.bukkit.entity.Player;

abstract class AbstractPlayerTask implements Runnable {

    protected final Player p;
    private TaskHandle task;

    AbstractPlayerTask(@Nonnull Player p) {
        this.p = p;
    }

    public void schedule(long delay) {
        cancel();
        task = Slimefun.getSchedulerService().runForLater(p, this, delay);
    }

    public void scheduleRepeating(long delay, long interval) {
        cancel();
        task = Slimefun.getSchedulerService().runForAtFixedRate(p, this, delay, interval);
    }

    @Override
    public final void run() {
        if (isValid()) {
            executeTask();
        }
    }

    /**
     * This method cancels this {@link AbstractPlayerTask}.
     */
    public final void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * This method checks if this {@link AbstractPlayerTask} should be continued or cancelled.
     * It will also cancel this {@link AbstractPlayerTask} if it became invalid.
     *
     * @return Whether this {@link AbstractPlayerTask} is still valid
     */
    protected boolean isValid() {
        if (!p.isOnline() || !p.isValid() || p.isDead() || !p.isSneaking()) {
            cancel();
            return false;
        }

        return true;
    }

    protected abstract void executeTask();
}
