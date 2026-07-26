package com.xzavier0722.mc.plugin.slimefun4.storage.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LegacySkullProfileDataFixerTest {
    private static final String LEGACY_INTERNAL =
            "H4sIAAAAAAAA/02PwU7CMBiAfw0mWD168xU0jhEwnAwwJl22TmBua28DirS0QMZG7O6+DzdfxsfQq3FHj1++7/IhAATXs02p1Eu+WwnFm9AgmeaAhrP74S7nvpgjQLXc87wQ/HAJzYK/F2XODwgAzppwEWeq5HDixrNYuraWqacWBndrjmaWCrHcP+JtbOZD3MW69uN+1ze9f22nyJKOom1vzbaTcq5jy29PFR9PWwv9eiTOQFLpKiKXgkQjQxNsmDMVTJINsV3JooGiiauJdGVQeZI5Ezt03A17DqowemtTOxZBhW1S9W0aBRXTI7FKWz2AKzjHy3qi8X33+5N/PCWf9PT14N7cAvwBEgWufRkBAAA=";
    private static final String TEXTURE =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDBjYjFlNjdiNTEyYWIyZDRiZjNkN2FjZTBlYWFmNjFjMzJjZDQ2ODFkZGMzOTg3Y2ViMzI2NzA2YTMzZmEifX19";

    @Test
    void extractsProfileAndConvertsLegacyIntArrayUuidIdempotently() {
        var profile = LegacySkullProfileDataFixer.extractProfile(LEGACY_INTERNAL);

        assertNotNull(profile);
        assertEquals(UUID.fromString("f42cfef5-7285-3f57-b759-b0dc2f46141b"), profile.uniqueId());
        assertEquals("CS-CoreLib", profile.name());
        assertEquals(1, profile.properties().size());
        assertEquals("textures", profile.properties().getFirst().name());
        assertEquals(TEXTURE, profile.properties().getFirst().value());

        var fixed = LegacySkullProfileDataFixer.fix(LEGACY_INTERNAL);
        assertNotEquals(LEGACY_INTERNAL, fixed);
        assertEquals(fixed, LegacySkullProfileDataFixer.fix(fixed));
    }
}
