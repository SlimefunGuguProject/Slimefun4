package io.github.thebusybiscuit.slimefun4.core.services.stability;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestBackpackOpenRegistry {

    @Test
    void duplicateBackpackReservationDoesNotReleaseOriginalPlayer() {
        BackpackOpenRegistry registry = new BackpackOpenRegistry();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        Assertions.assertTrue(registry.reserve(first, "uuid:backpack"));
        Assertions.assertFalse(registry.reserve(second, "uuid:backpack"));
        Assertions.assertTrue(registry.isOpening(first));
        Assertions.assertFalse(registry.isOpening(second));

        registry.release(first, "uuid:backpack");
        Assertions.assertTrue(registry.reserve(second, "uuid:backpack"));
    }

    @Test
    void wrongReservationKeyCannotUnlockBackpack() {
        BackpackOpenRegistry registry = new BackpackOpenRegistry();
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        Assertions.assertTrue(registry.reserve(player, "uuid:one"));
        registry.release(player, "uuid:two");
        Assertions.assertTrue(registry.isOpening(player));
        Assertions.assertFalse(registry.reserve(player, "uuid:two"));
        Assertions.assertFalse(registry.reserve(other, "uuid:one"));
    }
}
