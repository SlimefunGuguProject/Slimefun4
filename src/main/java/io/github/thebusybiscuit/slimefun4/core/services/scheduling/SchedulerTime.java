package io.github.thebusybiscuit.slimefun4.core.services.scheduling;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunInternal;
import javax.annotation.Nonnegative;

/**
 * Internal conversions shared by scheduler backends.
 */
@SlimefunInternal
public final class SchedulerTime {

    private static final long MILLIS_PER_TICK = 50L;

    private SchedulerTime() {}

    /**
     * Converts server ticks to wall-clock milliseconds without overflowing.
     *
     * @param ticks the non-negative tick count
     * @return the corresponding milliseconds, saturated at {@link Long#MAX_VALUE}
     */
    public static long ticksToMillis(@Nonnegative long ticks) {
        if (ticks < 0) {
            throw new IllegalArgumentException("Ticks cannot be negative");
        }

        return ticks > Long.MAX_VALUE / MILLIS_PER_TICK ? Long.MAX_VALUE : ticks * MILLIS_PER_TICK;
    }
}
