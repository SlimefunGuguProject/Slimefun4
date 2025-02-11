package city.norain.slimefun4.utils;

import com.molean.folia.adapter.Folia;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import it.unimi.dsi.fastutil.Pair;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

@UtilityClass
public class TaskUtil {
    @SneakyThrows
    public <T> T runSyncMethod(Callable<T> callable, Pair<Entity, Location> pair) {
        if (Bukkit.isPrimaryThread()) {
            return callable.call();
        } else {
            try {

                CompletableFuture<T> future = new CompletableFuture<>();
                if (pair.left() == null) {
                    Folia.runSync(
                            () -> {
                                try {
                                    future.complete(callable.call());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            pair.right());
                } else {
                    Folia.runSync(
                            () -> {
                                try {
                                    future.complete(callable.call());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            },
                            pair.left());
                }
                return future.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Slimefun.logger().log(Level.WARNING, "Timeout when executing sync method", e);
                return null;
            }
        }
    }
}
