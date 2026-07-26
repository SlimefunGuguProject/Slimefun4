package io.github.thebusybiscuit.slimefun4.core.services.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class TestProtectionCompatibility {

    @Test
    void testBypassSkipsProvider() {
        AtomicBoolean called = new AtomicBoolean();

        assertTrue(ProtectionCompatibility.isAllowed(true, false, () -> {
            called.set(true);
            return false;
        }));
        assertFalse(called.get());
    }

    @Test
    void testLocalDenialSkipsProvider() {
        AtomicBoolean called = new AtomicBoolean();

        assertFalse(ProtectionCompatibility.isAllowed(false, false, () -> {
            called.set(true);
            return true;
        }));
        assertFalse(called.get());
    }

    @Test
    void testProviderDecisionIsPreserved() {
        assertTrue(ProtectionCompatibility.isAllowed(false, true, () -> true));
        assertFalse(ProtectionCompatibility.isAllowed(false, true, () -> false));
    }

    @Test
    void testProviderRuntimeFailureDenies() {
        assertFalse(ProtectionCompatibility.isAllowed(false, true, () -> {
            throw new IllegalStateException("broken provider");
        }));
    }

    @Test
    void testProviderLinkageFailureDenies() {
        assertFalse(ProtectionCompatibility.isAllowed(false, true, () -> {
            throw new NoClassDefFoundError("missing optional integration");
        }));
    }
}
