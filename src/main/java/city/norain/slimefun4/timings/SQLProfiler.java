package city.norain.slimefun4.timings;

import city.norain.slimefun4.SlimefunExtended;
import city.norain.slimefun4.timings.entry.TimingEntry;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.bukkit.command.CommandSender;

public class SQLProfiler {
    private final ExecutorService reportExecutor = Executors.newFixedThreadPool(1, r -> {
        Thread t = new Thread(r, "Slimefun SQL Reporter");
        t.setUncaughtExceptionHandler((et, e) -> Slimefun.logger()
                .log(Level.SEVERE, "A error occurred in sql report generator thread " + et.getName(), e));
        return t;
    });

    @Getter
    private volatile boolean isProfiling = false;

    private final Map<TimingEntry, Long> samplingEntries = new HashMap<>();

    private final Map<TimingEntry, Long> entries = new HashMap<>();

    private final Set<CommandSender> subscribers = new HashSet<>();

    private long startTime = -1L;

    public void initSlowSqlCheck(@Nonnull Slimefun plugin) {
        plugin.getServer()
                .getScheduler()
                .runTaskTimerAsynchronously(plugin, new SlowSqlCheckTask(() -> samplingEntries), 20L, 20L);
    }

    public void start() {
        if (isProfiling) return;

        startTime = System.nanoTime();

        samplingEntries.clear();
        entries.clear();
        subscribers.clear();

        isProfiling = true;
    }

    public void subscribe(@Nonnull CommandSender sender) {
        subscribers.add(sender);
    }

    public void recordEntry(TimingEntry timingEntry) {
        if (!isProfiling) return;

        long now = System.nanoTime();
        samplingEntries.put(timingEntry, now);

        if (SlimefunExtended.isDatabaseDebugMode()) {
            Slimefun.logger().log(Level.INFO, "执行 SQL: " + timingEntry.normalize());
        }
    }

    public void finishEntry(TimingEntry timingEntry) {
        if (!isProfiling) return;

        Long startTime = samplingEntries.remove(timingEntry);
        if (startTime == null) {
            return;
        }
        entries.put(timingEntry, System.nanoTime() - startTime);
    }

    public void stop() {
        if (!isProfiling) return;

        samplingEntries.clear();
        isProfiling = false;

        reportExecutor.execute(this::generateReport);
    }

    public void shutdown() {
        stop();

        reportExecutor.shutdownNow();
    }

    public void generateReport() {
        if (isProfiling || entries.isEmpty()) return;

        Map<String, List<Map.Entry<TimingEntry, Long>>> groupByEntries = entries.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().getIdentifier()));

        var reportPath = generateReportFile(groupByEntries);

        if (reportPath == null) {
            subscribers.forEach(sub -> Slimefun.getLocalization().sendMessage(sub, "sf-cn.timings.warning", true));
        } else {
            subscribers.forEach(sub -> Slimefun.getLocalization()
                    .sendMessage(sub, "sf-cn.timings.stopped", true, msg -> msg.replace("{0}", reportPath)));
        }

        subscribers.clear();
        entries.clear();
        startTime = 0L;
    }

    private String generateReportFile(Map<String, List<Map.Entry<TimingEntry, Long>>> entries) {
        if (entries.isEmpty()) {
            return null;
        }

        File reportFile =
                new File("plugins/Slimefun/sql-timings/", "sql-timing-report-" + System.currentTimeMillis() + ".txt");

        try {
            reportFile.getParentFile().mkdirs();
            reportFile.createNewFile();
        } catch (Exception e) {
            Slimefun.logger().log(Level.WARNING, "Unable to create sql timing report!");
        }

        int entryCount = 0;
        Duration sqlTotalTime = Duration.ZERO;

        try (var writer = Files.newBufferedWriter(reportFile.toPath(), StandardCharsets.UTF_8)) {
            writer.append("Slimefun SQL Timing 报告");
            writer.newLine();
            writer.newLine();

            for (Map.Entry<String, List<Map.Entry<TimingEntry, Long>>> entry : entries.entrySet()) {
                String columnType = entry.getKey();
                List<Map.Entry<TimingEntry, Long>> value = entry.getValue().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toList());

                Collections.reverse(value);

                try {
                    writer.append("-- ").append(columnType);
                    writer.newLine();
                    for (Map.Entry<TimingEntry, Long> timingEntry : value) {
                        entryCount++;

                        var duration = Duration.ofNanos(timingEntry.getValue());
                        sqlTotalTime = sqlTotalTime.plus(duration);

                        var formattedTime = String.format(
                                "%ds%dms%dns",
                                duration.toSecondsPart(), duration.toMillisPart(), duration.toNanosPart());

                        writer.append(timingEntry.getKey().normalize())
                                .append(" -- ")
                                .append(formattedTime);
                        writer.newLine();
                    }
                    writer.newLine();
                    writer.newLine();
                } catch (IOException e) {
                    Slimefun.logger().log(Level.WARNING, "Unable to create sql timing report!", e);
                }
            }

            var totalTime = Duration.ofNanos(System.nanoTime()).minus(Duration.ofNanos(startTime));
            writer.append("已运行: ")
                    .append(String.format(
                            "%dh%dm%dns", totalTime.toHours(), totalTime.toMinutesPart(), totalTime.toSecondsPart()));
            writer.newLine();
            writer.append("总耗时: ")
                    .append(String.format(
                            "%dm%ds%dms",
                            sqlTotalTime.toSeconds(), sqlTotalTime.toMillisPart(), sqlTotalTime.toNanosPart()));
            writer.newLine();
            var avg = sqlTotalTime.dividedBy(entryCount);
            writer.append("平均耗时: ")
                    .append(String.format("%dm%ds%dms", avg.toSeconds(), avg.toMillisPart(), avg.toNanosPart()));
        } catch (IOException e) {
            Slimefun.logger().log(Level.WARNING, "Unable to create sql timing report!", e);
        }

        return reportFile.getAbsolutePath();
    }
}
