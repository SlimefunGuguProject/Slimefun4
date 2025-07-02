package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import city.norain.slimefun4.utils.TaskTimer;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.IDataSourceAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.callback.IAsyncReadCallback;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataType;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.ScopeKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.task.DatabaseThreadFactory;
import com.xzavier0722.mc.plugin.slimefun4.storage.task.QueuedWriteTask;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.OverridingMethodsMustInvokeSuper;

public abstract class ADataController {
    private final DataType dataType;
    private final Map<ScopeKey, QueuedWriteTask> scheduledWriteTasks;
    private final ScopedLock lock;

    private volatile IDataSourceAdapter<?> dataAdapter;
    /**
     * 数据库读取调度器
     */
    private ExecutorService readExecutor;
    /**
     * 数据库写入调度器
     */
    private ExecutorService writeExecutor;
    /**
     * 数据库回调调度器
     */
    private ExecutorService callbackExecutor;
    /**
     * 标记当前控制器是否已被关闭
     */
    private volatile boolean destroyed = false;

    protected final Logger logger;

    protected ADataController(DataType dataType) {
        this.dataType = dataType;
        scheduledWriteTasks = new ConcurrentHashMap<>();
        lock = new ScopedLock();
        logger = Logger.getLogger("SF-" + dataType.name() + "-Controller");
    }

    /**
     * 初始化 {@link ADataController}
     */
    @OverridingMethodsMustInvokeSuper
    public void init(IDataSourceAdapter<?> dataAdapter, int maxReadThread, int maxWriteThread) {
        this.dataAdapter = dataAdapter;
        dataAdapter.initStorage(dataType);
        dataAdapter.patch();
        readExecutor = Executors.newFixedThreadPool(maxReadThread, new DatabaseThreadFactory("SF-DB-Read-Thread #"));
        writeExecutor = Executors.newFixedThreadPool(maxWriteThread, new DatabaseThreadFactory("SF-DB-Write-Thread #"));
        callbackExecutor = new ThreadPoolExecutor(
                0,
                128,
                10,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(128),
                new DatabaseThreadFactory("SF-DB-CB-Thread #"));
    }

    /**
     * 正常关闭 {@link ADataController}
     */
    @OverridingMethodsMustInvokeSuper
    public void shutdown() {
        if (destroyed) {
            return;
        }
        destroyed = true;
        readExecutor.shutdownNow();
        callbackExecutor.shutdownNow();
        try {
            float totalTask = scheduledWriteTasks.size();
            var pendingTask = scheduledWriteTasks.size();
            var taskTimer = new TaskTimer();
            var previousTask = pendingTask;

            while (pendingTask > 0) {
                var doneTaskPercent = String.format("%.1f", (totalTask - pendingTask) / totalTask * 100);
                logger.log(Level.INFO, "数据保存中，请稍候... 剩余 {0} 个任务 ({1}%)", new Object[] {pendingTask, doneTaskPercent});
                TimeUnit.SECONDS.sleep(1);
                pendingTask = scheduledWriteTasks.size();

                if (previousTask > pendingTask) {
                    taskTimer.reset();
                    previousTask = pendingTask;
                    continue;
                }

                // 展示疑似死锁任务
                if ((taskTimer.peek() / 1000 / 60) > 2) {
                    logger.log(Level.WARNING, "检测到数据保存时出现的长耗时任务，可以截图下列信息供反馈参考 ({0}):\n", new Object[] {
                        scheduledWriteTasks.size()
                    });
                    var taskSnapshot = Map.copyOf(scheduledWriteTasks);
                    for (var task : taskSnapshot.entrySet()) {
                        var key = task.getKey();
                        var value = task.getValue();
                        logger.log(Level.WARNING, "On scope {0}:", new Object[] {key});
                        logger.log(Level.WARNING, "     {0}", new Object[] {value});
                        logger.log(Level.WARNING, " ");
                    }
                }
            }

            logger.info("数据保存完成.");
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Exception thrown while saving data: ", e);
        }
        writeExecutor.shutdownNow();
        dataAdapter = null;
    }

    protected void scheduleDeleteTask(ScopeKey scopeKey, RecordKey key, boolean forceScopeKey) {
        scheduleWriteTask(
                scopeKey,
                key,
                () -> {
                    dataAdapter.deleteData(key);
                },
                forceScopeKey);
    }

    protected void scheduleWriteTask(ScopeKey scopeKey, RecordKey key, RecordSet data, boolean forceScopeKey) {
        scheduleWriteTask(scopeKey, key, () -> dataAdapter.setData(key, data), forceScopeKey);
    }

    protected void scheduleWriteTask(ScopeKey scopeKey, RecordKey key, Runnable task, boolean forceScopeKey) {
        lock.lock(scopeKey);
        try {
            var scopeToUse = forceScopeKey ? scopeKey : key;
            var queuedTask = scheduledWriteTasks.get(scopeKey);
            if (queuedTask == null && scopeKey != scopeToUse) {
                queuedTask = scheduledWriteTasks.get(scopeToUse);
            }

            if (queuedTask != null && queuedTask.queue(key, task)) {
                return;
            }

            queuedTask = new QueuedWriteTask() {
                @Override
                protected void onSuccess() {
                    scheduledWriteTasks.remove(scopeToUse);
                }

                @Override
                protected void onError(Throwable e) {
                    Slimefun.logger().log(Level.SEVERE, "Exception thrown while executing write task: ");
                    e.printStackTrace();
                }
            };
            queuedTask.queue(key, task);
            scheduledWriteTasks.put(scopeToUse, queuedTask);
            writeExecutor.submit(queuedTask);
        } finally {
            lock.unlock(scopeKey);
        }
    }

    protected void checkDestroy() {
        if (destroyed) {
            throw new IllegalStateException("Controller cannot be accessed after destroyed.");
        }
    }

    protected <T> void invokeCallback(IAsyncReadCallback<T> callback, T result) {
        if (callback == null) {
            return;
        }

        Runnable cb;
        if (result == null) {
            cb = callback::onResultNotFound;
        } else {
            cb = () -> callback.onResult(result);
        }

        if (callback.runOnMainThread()) {
            Slimefun.runSync(cb);
        } else {
            callbackExecutor.submit(cb);
        }
    }

    protected void scheduleReadTask(Runnable run) {
        checkDestroy();
        readExecutor.submit(run);
    }

    protected void scheduleWriteTask(Runnable run) {
        checkDestroy();
        writeExecutor.submit(run);
    }

    protected List<RecordSet> getData(RecordKey key) {
        return getData(key, false);
    }

    protected List<RecordSet> getData(RecordKey key, boolean distinct) {
        return dataAdapter.getData(key, distinct);
    }

    protected void setData(RecordKey key, RecordSet data) {
        dataAdapter.setData(key, data);
    }

    protected void deleteData(RecordKey key) {
        dataAdapter.deleteData(key);
    }

    protected void abortScopeTask(ScopeKey key) {
        var task = scheduledWriteTasks.remove(key);
        if (task != null) {
            task.abort();
        }
    }

    public final DataType getDataType() {
        return dataType;
    }
}
