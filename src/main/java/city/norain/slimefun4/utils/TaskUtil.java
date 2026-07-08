package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

@UtilityClass
public class TaskUtil {
    @SneakyThrows
    public void runSyncMethod(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Slimefun.runSync(runnable);
        }
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable) {
        return runSyncMethod(callable, null, null);
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable, @Nonnull Location l) {
        return runSyncMethod(callable, l, null);
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable, @Nonnull Entity entity) {
        return runSyncMethod(callable, null, entity);
    }

    @SneakyThrows
    private <T> T runSyncMethod(@Nonnull Callable<T> callable, @Nullable Location l, @Nullable Entity entity) {
        if (!Slimefun.isFolia()) {
            return Bukkit.isPrimaryThread()
                    ? callable.call()
                    : Bukkit.getScheduler()
                            .callSyncMethod(Slimefun.instance(), callable)
                            .get(2, TimeUnit.SECONDS);
        }

        final CompletableFuture<T> result = new CompletableFuture<>();

        if (l != null) {
            Slimefun.getPlatformScheduler().runAtLocation(l, task -> {
                try {
                    result.complete(callable.call());
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
        } else {
            if (entity != null) {
                Slimefun.getPlatformScheduler().runAtEntity(entity, task -> {
                    try {
                        result.complete(callable.call());
                    } catch (Exception e) {
                        result.completeExceptionally(e);
                    }
                });
            } else {
                throw new IllegalArgumentException(
                        "Location or entity must be provided when executing sync task on Folia!");
            }
        }

        return result.get(2, TimeUnit.SECONDS);
    }
}
