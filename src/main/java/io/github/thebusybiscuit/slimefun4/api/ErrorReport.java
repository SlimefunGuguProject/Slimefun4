package io.github.thebusybiscuit.slimefun4.api;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.papermc.lib.PaperLib;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * This class represents an {@link ErrorReport}.
 * Error reports are thrown when a {@link BlockTicker} is causing problems.
 * To ensure that the console doesn't get too spammy, we destroy the block and generate
 * an {@link ErrorReport} instead.
 * Error reports get saved in the plugin folder.
 *
 * @param <T>
 *            The type of {@link Throwable} which has spawned this {@link ErrorReport}
 *
 * @author TheBusyBiscuit
 *
 */
public class ErrorReport<T extends Throwable> {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.ROOT);
    private static final AtomicInteger count = new AtomicInteger(0);

    private final SlimefunAddon addon;
    private final T throwable;

    private File file;

    /**
     * This is the base constructor for an {@link ErrorReport}. It will only
     * print the necessary info and provides a {@link Consumer} for any more detailed
     * needs.
     *
     * @param throwable
     *            The {@link Throwable} which caused this {@link ErrorReport}.
     * @param addon
     *            The {@link SlimefunAddon} responsible.
     * @param printer
     *            A custom {@link Consumer} to add more details.
     */
    @ParametersAreNonnullByDefault
    public ErrorReport(T throwable, SlimefunAddon addon, Consumer<PrintStream> printer) {
        this.throwable = throwable;
        this.addon = addon;

        Slimefun.runSync(() -> print(printer));
    }

    /**
     * This constructs a new {@link ErrorReport} for the given {@link Location} and
     * {@link SlimefunItem}.
     *
     * @param throwable
     *            The {@link Throwable} which caused this {@link ErrorReport}.
     * @param l
     *            The {@link Location} at which the error was thrown.
     * @param item
     *            The {@link SlimefunItem} responsible.
     */
    @ParametersAreNonnullByDefault
    public ErrorReport(T throwable, Location l, SlimefunItem item) {
        this(throwable, item.getAddon(), stream -> {
            stream.println("方块信息:");
            stream.println("  世界: " + l.getWorld().getName());
            stream.println("  X: " + l.getBlockX());
            stream.println("  Y: " + l.getBlockY());
            stream.println("  Z: " + l.getBlockZ());
            stream.println("  方块类型: " + l.getBlock().getType());
            stream.println("  方块数据: " + l.getBlock().getBlockData().getClass().getName());
            stream.println("  状态: " + l.getBlock().getState().getClass().getName());
            stream.println();

            if (item.getBlockTicker() != null) {
                stream.println("Ticker 信息:");
                stream.println("  类型: " + (item.getBlockTicker().isSynchronized() ? "同步" : "异步"));
                stream.println();
            }

            if (item instanceof EnergyNetProvider) {
                stream.println("Ticker 信息:");
                stream.println("  类型: 间接 (由能源网络管理)");
                stream.println();
            }

            stream.println("Slimefun 数据:");
            stream.println("  ID: " + item.getId());
            var blockData =
                    Slimefun.getDatabaseManager().getBlockDataController().getBlockData(l);

            if (blockData == null) {
                Slimefun.getBlockDataService()
                        .getUniversalDataUUID(l.getBlock())
                        .ifPresentOrElse(
                                uuid -> {
                                    var universalData = Slimefun.getDatabaseManager()
                                            .getBlockDataController()
                                            .getUniversalDataFromCache(uuid);
                                    if (universalData != null) {
                                        stream.println("  数据加载状态: " + universalData.isDataLoaded());
                                        stream.println("  物品栏: " + (universalData.getUniversalMenu() != null));
                                        stream.println("  数据: ");
                                        universalData
                                                .getAllData()
                                                .forEach((k, v) -> stream.println("    " + k + ": " + v));
                                    } else {
                                        stream.println("该方块没有任何数据.");
                                    }
                                },
                                () -> stream.println("该方块没有任何数据."));
            } else {
                stream.println("  数据加载状态: " + blockData.isDataLoaded());
                stream.println("  物品栏: " + (blockData.getBlockMenu() != null));
                stream.println("  数据: ");
                blockData.getAllData().forEach((k, v) -> stream.println("    " + k + ": " + v));
            }
            stream.println();
        });
    }

    /**
     * This constructs a new {@link ErrorReport} for the given {@link SlimefunItem}.
     *
     * @param throwable
     *            The {@link Throwable} which caused this {@link ErrorReport}.
     * @param item
     *            The {@link SlimefunItem} responsible.
     */
    @ParametersAreNonnullByDefault
    public ErrorReport(T throwable, SlimefunItem item) {
        this(throwable, item.getAddon(), stream -> {
            stream.println("SlimefunItem:");
            stream.println("  ID: " + item.getId());
            stream.println("  Plugin: "
                    + (item.getAddon() == null ? "Unknown" : item.getAddon().getName()));
            stream.println();
        });
    }

    /**
     * This method returns the {@link File} this {@link ErrorReport} has been written to.
     *
     * @return The {@link File} for this {@link ErrorReport}
     */
    public @Nonnull File getFile() {
        return file;
    }

    /**
     * This returns the {@link Throwable} that was thrown.
     *
     * @return The {@link Throwable}
     */
    public @Nonnull T getThrown() {
        return throwable;
    }

    /**
     * This method returns the amount of {@link ErrorReport ErrorReports} created in this session.
     *
     * @return The amount of {@link ErrorReport ErrorReports} created.
     */
    public static int count() {
        return count.get();
    }

    private void print(@Nonnull Consumer<PrintStream> printer) {
        this.file = getNewFile();
        count.incrementAndGet();

        try (PrintStream stream = new PrintStream(file, StandardCharsets.UTF_8.name())) {
            stream.println();

            stream.println("Error Generated: " + dateFormat.format(LocalDateTime.now()));
            stream.println();

            stream.println("Java Environment:");
            stream.println("  Operating System: " + System.getProperty("os.name"));
            stream.println("  Java Version: " + System.getProperty("java.version"));
            stream.println();

            String serverSoftware = PaperLib.isSpigot() && !PaperLib.isPaper() ? "Spigot" : Bukkit.getName();
            stream.println("Server Software: " + serverSoftware);
            stream.println("  Build: " + Bukkit.getVersion());
            stream.println("  Minecraft v" + Bukkit.getBukkitVersion());
            stream.println();

            stream.println("Slimefun Environment:");
            stream.println("  Slimefun v" + Slimefun.getVersion());
            stream.println("  Caused by: " + addon.getName() + " v" + addon.getPluginVersion());
            stream.println();

            List<String> plugins = new ArrayList<>();
            List<String> addons = new ArrayList<>();

            scanPlugins(plugins, addons);

            stream.println("Installed Addons (" + addons.size() + ")");
            addons.forEach(stream::println);

            stream.println();

            stream.println("Installed Plugins (" + plugins.size() + "):");
            plugins.forEach(stream::println);

            stream.println();

            printer.accept(stream);

            stream.println("Stacktrace:");
            stream.println();
            throwable.printStackTrace(stream);

            addon.getLogger().log(Level.WARNING, "");
            addon.getLogger().log(Level.WARNING, "An Error occurred! It has been saved as: ");
            addon.getLogger().log(Level.WARNING, "/plugins/Slimefun/error-reports/{0}", file.getName());
            addon.getLogger()
                    .log(
                            Level.WARNING,
                            "Please put this file on https://pastebin.com/ and report this to the developer(s).");

            if (addon.getBugTrackerURL() != null) {
                addon.getLogger().log(Level.WARNING, "Bug Tracker: {0}", addon.getBugTrackerURL());
            }

            addon.getLogger().log(Level.WARNING, "");
        } catch (Exception x) {
            addon.getLogger()
                    .log(
                            Level.SEVERE,
                            x,
                            () -> "An Error occurred while saving an Error-Report for Slimefun "
                                    + Slimefun.getVersion());
        }
    }

    private static void scanPlugins(@Nonnull List<String> plugins, @Nonnull List<String> addons) {
        String dependency = "Slimefun";

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                plugins.add("  + " + plugin.getName() + ' '
                        + plugin.getDescription().getVersion());

                if (plugin.getDescription().getDepend().contains(dependency)
                        || plugin.getDescription().getSoftDepend().contains(dependency)) {
                    addons.add("  + " + plugin.getName() + ' '
                            + plugin.getDescription().getVersion());
                }
            } else {
                plugins.add("  - " + plugin.getName() + ' '
                        + plugin.getDescription().getVersion());

                if (plugin.getDescription().getDepend().contains(dependency)
                        || plugin.getDescription().getSoftDepend().contains(dependency)) {
                    addons.add("  - " + plugin.getName() + ' '
                            + plugin.getDescription().getVersion());
                }
            }
        }
    }

    private static @Nonnull File getNewFile() {
        String path = "plugins/Slimefun/error-reports/" + dateFormat.format(LocalDateTime.now());
        File newFile = new File(path + ".err");

        if (newFile.exists()) {
            IntStream stream =
                    IntStream.iterate(1, i -> i + 1).filter(i -> !new File(path + " (" + i + ").err").exists());
            int id = stream.findFirst().getAsInt();

            newFile = new File(path + " (" + id + ").err");
        }

        return newFile;
    }

    /**
     * This helper method wraps the given {@link Runnable} into a try-catch block.
     * When an {@link Exception} occurs, a new {@link ErrorReport} will be generated using
     * the provided {@link Function}.
     *
     * @param function
     *            The {@link Function} to generate a new {@link ErrorReport}
     * @param runnable
     *            The code to execute
     */
    public static void tryCatch(
            @Nonnull Function<Exception, ErrorReport<Exception>> function, @Nonnull Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception x) {
            function.apply(x);
        }
    }
}
