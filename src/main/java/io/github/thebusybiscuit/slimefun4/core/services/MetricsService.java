package io.github.thebusybiscuit.slimefun4.core.services;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.Plugin;

/**
 * This service sends anonymous Slimefun Legacy usage statistics to bStats.
 *
 * <p>The built-in bStats charts automatically report server and player counts. Server owners can
 * disable bStats globally through {@code plugins/bStats/config.yml}.
 *
 * <p><b>Note:</b> Call {@link #start()} to begin metrics collection.
 */
public class MetricsService {

    private static final int BSTATS_PLUGIN_ID = 32960;
    private static final String BSTATS_VERSION = "3.2.1";

    private final Slimefun plugin;
    private final AtomicBoolean startRequested = new AtomicBoolean();
    private volatile Metrics metrics;

    /**
     * Constructs a new Slimefun Legacy metrics service.
     *
     * @param plugin
     *            the active {@link Slimefun} instance
     */
    public MetricsService(@Nonnull Slimefun plugin) {
        this.plugin = plugin;
    }

    /** Starts bStats for the registered Slimefun Legacy plugin ID. */
    public void start() {
        if (!startRequested.compareAndSet(false, true)) {
            return;
        }

        // Slimefun currently calls this service from its metrics worker thread. bStats initialization
        // is completed on the server thread to preserve the existing startup behavior.
        try {
            Slimefun.runSync(() -> {
                if (!startRequested.get() || !plugin.isEnabled()) {
                    startRequested.set(false);
                    return;
                }

                try {
                    metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);
                    plugin.getLogger()
                            .log(
                                    Level.INFO,
                                    "bStats metrics started for Slimefun Legacy (plugin id {0}).",
                                    BSTATS_PLUGIN_ID);
                } catch (Exception | LinkageError ex) {
                    startRequested.set(false);
                    plugin.getLogger()
                            .log(Level.WARNING, "Failed to start Slimefun Legacy bStats metrics.", ex);
                }
            });
        } catch (RuntimeException | LinkageError ex) {
            startRequested.set(false);
            plugin.getLogger().log(Level.WARNING, "Could not schedule Slimefun Legacy bStats startup.", ex);
        }
    }

    /** Stops the bStats scheduler and releases the current metrics instance. */
    public void cleanUp() {
        Metrics currentMetrics = metrics;
        metrics = null;
        startRequested.set(false);

        if (currentMetrics != null) {
            currentMetrics.shutdown();
        }
    }

    /**
     * Retained for source and binary compatibility with code that referenced the old downloaded
     * MetricsModule updater. bStats is now updated as a normal build dependency.
     *
     * @param currentVersion
     *            ignored legacy module version
     *
     * @return always {@code false}
     */
    public boolean checkForUpdate(@Nullable String currentVersion) {
        return false;
    }

    /**
     * Returns the bundled bStats library version.
     *
     * @return the bundled bStats version
     */
    @Nullable
    public String getVersion() {
        return BSTATS_VERSION;
    }

    /**
     * The old runtime MetricsModule updater is no longer used.
     *
     * @return always {@code false}
     */
    public boolean hasAutoUpdates() {
        return false;
    }
}
