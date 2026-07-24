package io.github.thebusybiscuit.slimefun4.implementation.tasks;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

class TestViewedInventoryGuard {

    private static ServerMock server;

    @BeforeAll
    static void loadServer() {
        server = MockBukkit.mock();
    }

    @AfterAll
    static void unloadServer() {
        MockBukkit.unmock();
    }

    @Test
    @DisplayName("Viewed-machine tracking is isolated by block location")
    void testViewedLocationTracking() {
        World world = server.addSimpleWorld("viewed-machine-test");
        Location viewed = new Location(world, 10, 64, 10);
        Location neighbour = viewed.clone().add(1, 0, 0);
        TickerTask tickerTask = new TickerTask();

        Assertions.assertFalse(tickerTask.isInventoryViewed(viewed));

        tickerTask.setInventoryViewed(viewed, true);
        Assertions.assertTrue(tickerTask.isInventoryViewed(viewed));
        Assertions.assertFalse(tickerTask.isInventoryViewed(neighbour));

        tickerTask.setInventoryViewed(viewed, false);
        Assertions.assertFalse(tickerTask.isInventoryViewed(viewed));
    }
}
