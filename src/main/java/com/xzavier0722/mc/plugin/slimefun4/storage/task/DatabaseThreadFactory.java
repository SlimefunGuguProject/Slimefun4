package com.xzavier0722.mc.plugin.slimefun4.storage.task;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class DatabaseThreadFactory implements ThreadFactory {
    private final AtomicInteger threadCount = new AtomicInteger(0);
    private String prefix = "SF-Database-Thread #";

    public DatabaseThreadFactory() {}

    public DatabaseThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(@Nonnull Runnable r) {
        Thread t = new Thread(r, prefix + threadCount.getAndIncrement());
        t.setUncaughtExceptionHandler((et, e) ->
                Slimefun.logger().log(Level.SEVERE, "A error occurred in database thread " + et.getName(), e));

        return t;
    }
}
