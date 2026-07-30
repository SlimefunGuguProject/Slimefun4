package me.mrCookieSlime.Slimefun.Objects.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TestBlockTickerConcurrency {

    @Test
    void testUniqueTickRunsOnceAcrossConcurrentRegionUpdates() throws InterruptedException {
        AtomicInteger uniqueTicks = new AtomicInteger();
        BlockTicker ticker = new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return false;
            }

            @Override
            public void uniqueTick() {
                uniqueTicks.incrementAndGet();
            }
        };

        runConcurrentUpdates(ticker, 64);
        assertEquals(1, uniqueTicks.get());

        ticker.startNewTick();
        runConcurrentUpdates(ticker, 64);
        assertEquals(2, uniqueTicks.get());
    }

    private static void runConcurrentUpdates(BlockTicker ticker, int updates) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Runnable> tasks = new ArrayList<>(updates);

        for (int i = 0; i < updates; i++) {
            tasks.add(() -> {
                try {
                    start.await();
                    ticker.update();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        tasks.forEach(executor::execute);
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
