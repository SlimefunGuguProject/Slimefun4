package io.github.thebusybiscuit.slimefun4.core.services.profiler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestProfilerQueueRecovery {

    @Test
    @DisplayName("Cancelled ticker samples cannot leave the profiler queue stuck")
    void testCancelScheduledEntry() {
        SlimefunProfiler profiler = new SlimefunProfiler();
        try {
            profiler.start();
            profiler.scheduleEntries(1);
            Assertions.assertEquals(1, profiler.getQueuedEntries());

            profiler.cancelScheduledEntry();
            Assertions.assertEquals(0, profiler.getQueuedEntries());

            profiler.cancelScheduledEntry();
            Assertions.assertEquals(0, profiler.getQueuedEntries(), "The queue must never become negative");
        } finally {
            profiler.kill();
        }
    }
}
