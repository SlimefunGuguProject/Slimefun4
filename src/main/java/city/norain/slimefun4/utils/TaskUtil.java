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
        if (!Slimefun.getSchedulerService().isFolia() && Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Slimefun.getSchedulerService().run(runnable);
        }
    }

    /**
     * Legacy blocking bridge for global work.
     *
     * @deprecated Prefer {@link #callSyncMethod(Callable)} or {@link #callAt(Location, Callable)}.
     */
    @Deprecated
    @SneakyThrows
    public <T> T runSyncMethod(Callable<T> callable) {
        if (!Slimefun.getSchedulerService().isFolia() && Bukkit.isPrimaryThread()) {
            return callable.call();
        }

        return await(callSyncMethod(callable));
    }

    /**
     * Blocking location-owned compatibility bridge. Callers should prefer composing the future from
     * {@link #callAt(Location, Callable)} whenever possible.
     */
    @SneakyThrows
    public <T> T runSyncMethod(Location location, Callable<T> callable) {
        if (Slimefun.getSchedulerService().isOwnedByCurrentRegion(location)) {
            return callable.call();
        }

        return await(callAt(location, callable));
    }

    public <T> CompletableFuture<T> callSyncMethod(Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();

        if (!Slimefun.getSchedulerService().isFolia() && Bukkit.isPrimaryThread()) {
            complete(future, callable);
        } else {
            Slimefun.getSchedulerService().run(() -> complete(future, callable));
        }

        return future;
    }

    public <T> CompletableFuture<T> callAt(Location location, Callable<T> callable) {
        CompletableFuture<T> future = new CompletableFuture<>();

        if (Slimefun.getSchedulerService().isOwnedByCurrentRegion(location)) {
            complete(future, callable);
        } else {
            Slimefun.getSchedulerService().runAt(location, () -> complete(future, callable));
        }

        return future;
    }

    private <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            Slimefun.logger().log(Level.WARNING, "Timeout when executing scheduler-owned method", e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Scheduler-owned method failed", cause);
        }
    }

    private <T> void complete(CompletableFuture<T> future, Callable<T> callable) {
        try {
            future.complete(callable.call());
        } catch (Exception | LinkageError throwable) {
            future.completeExceptionally(throwable);
        }
    }
}
