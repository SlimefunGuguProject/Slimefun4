package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.attributes.UniversalDataTrait;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class SlimefunUniversalBlockDataTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void keepsLocationDataUnresolvedWhenWorldIsMissing() {
        var data = new SlimefunUniversalBlockData(UUID.randomUUID(), "TEST_ITEM");
        var location = "missing_world;1:2:3";
        data.addTrait(UniversalDataTrait.BLOCK);
        data.setCacheInternal(UniversalDataTrait.BLOCK.getReservedKey(), location, false);
        data.setIsDataLoaded(true);

        assertNull(data.getLastPresent());
        assertEquals(location, data.getData(UniversalDataTrait.BLOCK.getReservedKey()));

        server.addSimpleWorld("missing_world");
        var resolved = data.getLastPresent();
        assertNotNull(resolved);
        assertEquals("missing_world", resolved.getWorld().getName());
    }

    @Test
    void keepsLegacyLocationDataUnresolvedWhenWorldIsMissing() {
        var data = new SlimefunUniversalBlockData(UUID.randomUUID(), "TEST_ITEM");
        var location = "[world=missing_world,x=1.0,y=2.0,z=3.0]";
        data.addTrait(UniversalDataTrait.BLOCK);
        data.setCacheInternal(UniversalDataTrait.BLOCK.getReservedKey(), location, false);
        data.setIsDataLoaded(true);

        assertNull(data.getLastPresent());
        assertEquals(location, data.getData(UniversalDataTrait.BLOCK.getReservedKey()));
    }
}
