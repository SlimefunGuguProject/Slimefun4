package io.github.thebusybiscuit.slimefun4.implementation.tasks.player;

import com.molean.folia.adapter.Folia;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import javax.annotation.Nonnull;
import org.bukkit.entity.Player;

abstract class AbstractPlayerTask implements Runnable {

    protected final Player p;
    private ScheduledTask id;

    AbstractPlayerTask(@Nonnull Player p) {
        this.p = p;
    }

    private void setID(ScheduledTask id) {
        this.id = id;
    }

    public void schedule(long delay) {
        setID(Folia.getScheduler().scheduleSyncDelayedTask(Slimefun.instance(), p, this, delay));
    }

    public void scheduleRepeating(long delay, long interval) {
        setID(Folia.getScheduler().runTaskTimer(Slimefun.instance(), this, p, (int) delay, interval));
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
        id.cancel();
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
