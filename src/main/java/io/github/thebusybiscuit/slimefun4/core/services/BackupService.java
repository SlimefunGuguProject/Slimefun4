package io.github.thebusybiscuit.slimefun4.core.services;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.StorageType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.annotation.Nonnull;
import org.apache.commons.lang.Validate;

/**
 * This Service creates a Backup of your Slimefun world data on every server shutdown.
 *
 * @author TheBusyBiscuit
 *
 */
public class BackupService implements Runnable {

    /**
     * The maximum amount of backups to maintain
     */
    private static final int MAX_BACKUPS = 20;

    /**
     * Our {@link DateTimeFormatter} for formatting file names.
     */
    private final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm", Locale.ROOT);

    /**
     * The directory in which to create the backups
     */
    private final File directory = new File("data-storage/Slimefun/block-backups");

    @Override
    public void run() {
        var dbManager = Slimefun.getDatabaseManager();
        if (dbManager.getProfileStorageType() != StorageType.SQLITE
                && dbManager.getBlockDataStorageType() != StorageType.SQLITE) {
            return;
        }
        // Make sure that the directory exists.
        if (directory.exists()) {
            List<File> backups = Arrays.asList(directory.listFiles());

            if (backups.size() > MAX_BACKUPS) {
                try {
                    purgeBackups(backups);
                } catch (IOException e) {
                    Slimefun.logger().log(Level.WARNING, "无法删除旧备份文件", e);
                }
            }

            File file = new File(directory, format.format(LocalDateTime.now()) + ".zip");

            if (!file.exists()) {
                try {
                    if (file.createNewFile()) {
                        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(file))) {
                            createBackup(output);
                        }

                        Slimefun.logger().log(Level.INFO, "已备份 Slimefun 数据至: {0}", file.getName());
                    } else {
                        Slimefun.logger().log(Level.WARNING, "无法创建备份文件: {0}", file.getName());
                    }
                } catch (IOException x) {
                    Slimefun.logger()
                            .log(
                                    Level.SEVERE,
                                    x,
                                    () -> "An Exception occurred while creating a backup for Slimefun "
                                            + Slimefun.getVersion());
                }
            }
        }
    }

    private void createBackup(@Nonnull ZipOutputStream output) throws IOException {
        Validate.notNull(output, "The Output Stream cannot be null!");

        if (Slimefun.getDatabaseManager().getProfileStorageType() == StorageType.SQLITE) {
            addFile(output, new File("data-storage/Slimefun", "profile.db"), "");
        }

        if (Slimefun.getDatabaseManager().getBlockDataStorageType() == StorageType.SQLITE) {
            addFile(output, new File("data-storage/Slimefun", "block-storage.db"), "");
        }
    }

    private void addFile(ZipOutputStream output, File file, String path) throws IOException {
        var entry = new ZipEntry(path + "/" + file.getName());
        output.putNextEntry(entry);

        byte[] buffer = new byte[4096];
        try (var input = new FileInputStream(file)) {
            int length;

            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        }
        output.closeEntry();
    }

    private void addDirectory(@Nonnull ZipOutputStream output, @Nonnull File directory, @Nonnull String zipPath)
            throws IOException {
        for (File file : directory.listFiles()) {
            addFile(output, file, zipPath);
        }
    }

    /**
     * This method will delete old backups.
     *
     * @param backups
     *            The {@link List} of all backups
     *
     * @throws IOException
     *             An {@link IOException} is thrown if a {@link File} could not be deleted
     */
    private void purgeBackups(@Nonnull List<File> backups) throws IOException {
        var matchedBackup = backups.stream()
                .filter(f -> f.getName().matches("^\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}$"))
                .sorted((a, b) -> {
                    LocalDateTime time1 = LocalDateTime.parse(
                            a.getName().substring(0, a.getName().length() - 4), format);
                    LocalDateTime time2 = LocalDateTime.parse(
                            b.getName().substring(0, b.getName().length() - 4), format);

                    return time2.compareTo(time1);
                })
                .toList();

        for (int i = matchedBackup.size() - MAX_BACKUPS; i > 0; i--) {
            Files.delete(matchedBackup.get(i).toPath());
        }
    }
}
