package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;

@UtilityClass
public class TaskUtil {
    @SneakyThrows
    public void runSyncMethod(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Slimefun.getSchedulerService().run(runnable);
        }
    }

    /**
     * Legacy blocking bridge for call sites that still require an immediate return value.
     *
     * @deprecated Prefer {@link #callSyncMethod(Callable)} or {@link #callAt(Location, Callable)} and compose the
     *             returned future.
     */
    @Deprecated
    @SneakyThrows
    public <T> T runSyncMethod(Callable<T> callable) {
        if (Bukkit.isPrimaryThread()) {
            return callable.call();
        }

        try {
            return callSyncMethod(callable).get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Slimefun.logger().log(Level.WARNING, "Timeout when executing sync method", e);
            return null;
        }
    }

    /**
     * Executes a callable on the global server scheduler without blocking the caller.
     *
     * @param callable
     *            The callable to execute
     * @param <T>
     *            The result type
     *
     * @return A future completed with the callable result
     */
    public <T> CompletableFuture<T> callSyncMethod(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();

        if (Bukkit.isPrimaryThread()) {
            complete(future, callable);
        } else {
            Slimefun.getSchedulerService().run(() -> complete(future, callable));
        }

        return future;
    }

    /**
     * Executes a callable on the thread that owns the target location without blocking the caller.
     *
     * @param location
     *            The target location
     * @param callable
     *            The callable to execute
     * @param <T>
     *            The result type
     *
     * @return A future completed with the callable result
     */
    public <T> CompletableFuture<T> callAt(Location location, Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();

        if (Slimefun.getSchedulerService().isOwnedByCurrentRegion(location)) {
            complete(future, callable);
        } else {
            Slimefun.getSchedulerService().runAt(location, () -> complete(future, callable));
        }

        return future;
    }

    private <T> void complete(CompletableFuture<T> future, Callable<T> callable) {
        try {
            future.complete(callable.call());
        } catch (Exception | LinkageError throwable) {
            future.completeExceptionally(throwable);
        }
    }
}
