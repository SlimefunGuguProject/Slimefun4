package io.github.thebusybiscuit.slimefun4.core.services.protection;

import io.github.thebusybiscuit.slimefun4.api.annotations.SlimefunInternal;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Central fail-closed policy for optional protection-provider checks.
 */
@SlimefunInternal
public final class ProtectionCompatibility {

    private ProtectionCompatibility() {}

    /**
     * Evaluates a protection check while preserving explicit bypass permissions and local item restrictions.
     *
     * <p>Provider runtime and linkage failures are denied instead of allowing an unprotected interaction.
     *
     * @param bypass
     *            Whether the player has an explicit bypass permission
     * @param locallyAllowed
     *            Whether Slimefun's own item permission allows the interaction
     * @param providerCheck
     *            The external protection-provider check
     *
     * @return Whether the interaction is allowed
     */
    public static boolean isAllowed(boolean bypass, boolean locallyAllowed, BooleanSupplier providerCheck) {
        Objects.requireNonNull(providerCheck, "Protection provider check cannot be null");

        if (bypass) {
            return true;
        }

        if (!locallyAllowed) {
            return false;
        }

        try {
            return providerCheck.getAsBoolean();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }
}
