package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import lombok.Getter;

public class SlimefunPoolExecutor extends ThreadPoolExecutor {
    @Getter
    private final String name;

    @Getter
    private final List<Long> runningThreads = new CopyOnWriteArrayList<>();

    public SlimefunPoolExecutor(
            String name,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            @Nonnull TimeUnit unit,
            @Nonnull BlockingQueue<Runnable> workQueue,
            @Nonnull ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);

        setRejectedExecutionHandler(new SlimefunRejectedExecutionHandler());

        this.name = name;

        Slimefun.getProfiler().registerPool(this);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);

        runningThreads.add(t.getId());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            super.afterExecute(r, t);

            if (t != null) {
                Slimefun.logger()
                        .log(
                                Level.SEVERE,
                                "An error occurred in " + name + "("
                                        + Thread.currentThread().getName() + ")",
                                t);
            }

            if (r instanceof FutureTask<?> future && future.isDone()) {
                try {
                    future.get();
                } catch (Exception e) {
                    Slimefun.logger()
                            .log(
                                    Level.SEVERE,
                                    "An error occurred in " + name + "("
                                            + Thread.currentThread().getName() + ")",
                                    e);
                }
            }
        } finally {
            runningThreads.remove(Thread.currentThread().getId());
        }
    }
}
