package io.github.thebusybiscuit.slimefun4.core.services.stability;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestMachineCircuitBreaker {

    @Test
    void onlyOneHalfOpenProbeIsAllowed() {
        MachineCircuitBreaker<String> breaker = new MachineCircuitBreaker<>();
        breaker.open("machine", 100L);

        Assertions.assertFalse(breaker.canAttempt("machine", 99L));
        Assertions.assertTrue(breaker.canAttempt("machine", 100L));
        Assertions.assertFalse(breaker.canAttempt("machine", 101L));
    }

    @Test
    void reopeningResetsHalfOpenProbe() {
        MachineCircuitBreaker<String> breaker = new MachineCircuitBreaker<>();
        breaker.open("machine", 100L);
        Assertions.assertTrue(breaker.canAttempt("machine", 100L));

        breaker.open("machine", 200L);
        Assertions.assertFalse(breaker.canAttempt("machine", 199L));
        Assertions.assertTrue(breaker.canAttempt("machine", 200L));
        Assertions.assertTrue(breaker.clear("machine"));
        Assertions.assertTrue(breaker.canAttempt("machine", 0L));
    }
}
