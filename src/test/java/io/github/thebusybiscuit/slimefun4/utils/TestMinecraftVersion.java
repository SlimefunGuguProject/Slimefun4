package io.github.thebusybiscuit.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TestMinecraftVersion {

    @Test
    @DisplayName("Three-arg API should match base legacy and new-version constants")
    void testThreeArgMatchesSelf() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_16.isMinecraftVersion(1, 16, 0));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_21.isMinecraftVersion(1, 21, 3));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(26, 1, 0));
    }

    @Test
    @DisplayName("Three-arg API should support 26.1.x series")
    void testThreeArgMatchesNewVersioning() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(26, 1, 0));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(26, 1, 1));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(26, 1, 99));
    }

    @Test
    @DisplayName("Three-arg API should split 1.20.x and 1.20.5+ by patch")
    void testThreeArgPatchRangeForOneTwenty() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20.isMinecraftVersion(1, 20, 0));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20.isMinecraftVersion(1, 20, 4));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_20.isMinecraftVersion(1, 20, 5));

        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_20_5.isMinecraftVersion(1, 20, 4));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20_5.isMinecraftVersion(1, 20, 5));
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_20_5.isMinecraftVersion(1, 20, 99));
    }

    @Test
    @DisplayName("Three-arg API should reject mismatched major/minor")
    void testThreeArgRejectsWrongMajorOrMinor() {
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(1, 26, 1));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(26, 2, 1));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_21.isMinecraftVersion(26, 1, 1));
    }

    @Test
    @DisplayName("Three-arg API should return false for virtual versions")
    void testThreeArgVirtualVersionHandling() {
        Assertions.assertFalse(MinecraftVersion.UNKNOWN.isMinecraftVersion(26, 1, 1));
        Assertions.assertFalse(MinecraftVersion.UNIT_TEST.isMinecraftVersion(1, 21, 1));
    }

    @Test
    @DisplayName("Legacy two-arg API should still identify 26.1.x correctly")
    void testTwoArgRegression() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(26, 1));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(26, 0));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_1.isMinecraftVersion(1, 26));
    }

    @Test
    @DisplayName("Enum order should keep 26.1 newer than 1.21")
    void testAtLeastOrderingForNewVersion() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_26_1.isAtLeast(MinecraftVersion.MINECRAFT_1_21));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_1_21.isAtLeast(MinecraftVersion.MINECRAFT_26_1));
    }

    @Test
    @DisplayName("isBefore should be symmetric for 1.21 and 26.1")
    void testIsBeforeForNewVersion() {
        Assertions.assertTrue(MinecraftVersion.MINECRAFT_1_21.isBefore(MinecraftVersion.MINECRAFT_26_1));
        Assertions.assertFalse(MinecraftVersion.MINECRAFT_26_1.isBefore(MinecraftVersion.MINECRAFT_1_21));
    }
}
