package io.github.thebusybiscuit.slimefun4.core.services.scheduling;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunInternal;

/**
 * Centralized runtime detection for Paper's regionized Folia server implementation.
 *
 * <p>This deliberately uses class detection instead of server-name matching so forks that retain the Folia runtime
 * semantics are handled correctly as well.
 */
@SlimefunInternal
public final class FoliaSupport {

    private static final boolean FOLIA = isClassPresent("io.papermc.paper.threadedregions.RegionizedServer");

    private FoliaSupport() {}

    /**
     * Returns whether the current server uses Folia's regionized ticking model.
     *
     * @return whether Folia scheduler and ownership semantics are active
     */
    public static boolean isFolia() {
        return FOLIA;
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className, false, FoliaSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
}
