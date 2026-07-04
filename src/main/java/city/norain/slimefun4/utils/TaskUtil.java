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

/**
 * 线程工具类, 提供跨 Paper/Folia 的同步方法执行能力。
 *
 * <h3>Folia 死锁避免策略</h3>
 * <p>
 * Folia 上没有全局主线程, 取而代之的是区域线程。
 * 当事件回调在区域线程触发, 且回调内通过调度器向同一区域派发任务时,
 * {@code CompletableFuture.get()} 会阻塞当前线程等待自己 —— 导致死锁。
 * <p>
 * 解决方法: {@link #isTickThread()} 检测当前线程名是否包含
 * {@code "Region Scheduler"} 或 {@code "TickThread"}。
 * 若已在区域线程上, 直接同步执行; 否则通过 {@link PlatformScheduler} 派发。
 *
 * <h3>已知限制</h3>
 * <ul>
 *   <li>无 location/entity 的回退使用 {@code runNextTick()} (全局调度器),
 *       回调中的方块/实体操作可能因不在正确区域线程而失败。</li>
 *   <li>建议调用方始终传入 Location 或 Entity 参数以获得正确的区域调度。</li>
 * </ul>
 *
 * @author SlimefunGuguProject
 * @author qzgeek (Folia isTickThread 死锁修复)
 */
@UtilityClass
public class TaskUtil {
    /**
     * Check if the current thread is a Folia region tick thread
     * (on which block/entity operations are safe).
     */
    private static boolean isTickThread() {
        if (!Slimefun.isFolia()) {
            return Bukkit.isPrimaryThread();
        }
        String name = Thread.currentThread().getName();
        return name.contains("Region Scheduler") || name.contains("TickThread");
    }

    @SneakyThrows
    public void runSyncMethod(Runnable runnable) {
        if (isTickThread()) {
            runnable.run();
        } else {
            Slimefun.runSync(runnable);
        }
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable) {
        // When no location is provided on Folia, try to execute directly
        // if we're on a tick thread; otherwise dispatch via runNextTick
        if (isTickThread()) {
            return callable.call();
        }
        if (!Slimefun.isFolia()) {
            return Bukkit.getScheduler()
                    .callSyncMethod(Slimefun.instance(), callable)
                    .get(5, TimeUnit.SECONDS);
        }
        final CompletableFuture<T> result = new CompletableFuture<>();
        Slimefun.getPlatformScheduler().runNextTick(task -> {
            try {
                result.complete(callable.call());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable, @Nonnull Location l) {
        // On Folia: if we're on a region tick thread, execute directly
        // to avoid dispatching to ourselves and deadlocking
        if (Slimefun.isFolia() && isTickThread()) {
            return callable.call();
        }
        if (!Slimefun.isFolia()) {
            return Bukkit.isPrimaryThread()
                    ? callable.call()
                    : Bukkit.getScheduler()
                            .callSyncMethod(Slimefun.instance(), callable)
                            .get(5, TimeUnit.SECONDS);
        }
        final CompletableFuture<T> result = new CompletableFuture<>();
        Slimefun.getPlatformScheduler().runAtLocation(l, task -> {
            try {
                result.complete(callable.call());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    @SneakyThrows
    public <T> T runSyncMethod(@Nonnull Callable<T> callable, @Nonnull Entity entity) {
        if (Slimefun.isFolia() && isTickThread()) {
            return callable.call();
        }
        if (!Slimefun.isFolia()) {
            return Bukkit.isPrimaryThread()
                    ? callable.call()
                    : Bukkit.getScheduler()
                            .callSyncMethod(Slimefun.instance(), callable)
                            .get(5, TimeUnit.SECONDS);
        }
        final CompletableFuture<T> result = new CompletableFuture<>();
        Slimefun.getPlatformScheduler().runAtEntity(entity, task -> {
            try {
                result.complete(callable.call());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
