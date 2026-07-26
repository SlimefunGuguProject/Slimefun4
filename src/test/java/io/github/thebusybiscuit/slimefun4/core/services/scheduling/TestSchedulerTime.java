package io.github.thebusybiscuit.slimefun4.core.services.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TestSchedulerTime {

    @Test
    void testTickConversion() {
        assertEquals(0L, SchedulerTime.ticksToMillis(0L));
        assertEquals(50L, SchedulerTime.ticksToMillis(1L));
        assertEquals(1000L, SchedulerTime.ticksToMillis(20L));
        assertEquals(Long.MAX_VALUE, SchedulerTime.ticksToMillis(Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> SchedulerTime.ticksToMillis(-1L));
    }
}
