package city.norain.slimefun4.utils;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
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
    public <T> T runSyncMethod(@Nonnull Callable<T> callable) {
        if (Slimefun.folia().isFolia()) {
            throw new IllegalArgumentException("Location must be provided when executing sync task on Folia!");
        }

        return runSyncMethod(callable, null, null);
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable, @Nonnull Location l) {
        return runSyncMethod(callable, l, null);
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable, @Nonnull Entity entity) {
        if (Slimefun.folia().isFolia()) {
            throw new IllegalArgumentException("Entity must be provided when executing sync task on Folia!");
        }

        return runSyncMethod(callable, null, entity);
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable, @Nullable Location l, @Nullable Entity entity) {
        if (Bukkit.isPrimaryThread()) {
            return callable.call();
        } else {
            try {
                if (Slimefun.folia().isFolia()) {
                    final CompletableFuture<T> result = new CompletableFuture<>();

                    System.out.println("Sync task created, hashcode = " + result.hashCode());

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
                } else {
                    return Bukkit.getScheduler()
                            .callSyncMethod(Slimefun.instance(), callable)
                            .get(1, TimeUnit.SECONDS);
                }
            } catch (TimeoutException e) {
                Slimefun.logger().log(Level.WARNING, "Timeout when executing sync method", e);
                return null;
            }
        }
    }
}
