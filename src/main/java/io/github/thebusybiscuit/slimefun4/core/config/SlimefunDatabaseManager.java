package io.github.thebusybiscuit.slimefun4.core.config;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.IDataSourceAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.mysql.MysqlAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.mysql.MysqlConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.postgresql.PostgreSqlAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.postgresql.PostgreSqlConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite.SqliteAdapter;
import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlite.SqliteConfig;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataType;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ChunkDataLoadMode;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ControllerHolder;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ProfileDataController;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.StorageType;
import io.github.bakedlibs.dough.config.Config;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.logging.Level;
import javax.annotation.Nullable;

public class SlimefunDatabaseManager {
    private static final String PROFILE_CONFIG_FILE_NAME = "profile-storage.yml";
    private static final String BLOCK_STORAGE_FILE_NAME = "block-storage.yml";
    private final Slimefun plugin;
    private final Config profileConfig;
    private final Config blockStorageConfig;
    private StorageType profileStorageType;
    private StorageType blockDataStorageType;
    private IDataSourceAdapter<?> profileAdapter;
    private IDataSourceAdapter<?> blockStorageAdapter;
    private final File cleanShutdownMarker;
    private final File storageInitializedMarker;
    private boolean previousShutdownClean = true;

    public SlimefunDatabaseManager(Slimefun plugin) {
        this.plugin = plugin;

        if (!new File(plugin.getDataFolder(), PROFILE_CONFIG_FILE_NAME).exists()) {
            plugin.saveResource(PROFILE_CONFIG_FILE_NAME, true);
        }

        if (!new File(plugin.getDataFolder(), BLOCK_STORAGE_FILE_NAME).exists()) {
            plugin.saveResource(BLOCK_STORAGE_FILE_NAME, true);
        }

        profileConfig = new Config(plugin, PROFILE_CONFIG_FILE_NAME);
        blockStorageConfig = new Config(plugin, BLOCK_STORAGE_FILE_NAME);
        cleanShutdownMarker = new File("data-storage/Slimefun", ".clean-shutdown");
        storageInitializedMarker = new File("data-storage/Slimefun", ".storage-initialized");
    }

    public void init() {
        markStartupInProgress();
        initDefaultVal();

        try {
            blockDataStorageType = StorageType.valueOf(blockStorageConfig.getString("storageType"));
            var readExecutorThread = blockStorageConfig.getInt("readExecutorThread");
            var writeExecutorThread =
                    blockDataStorageType == StorageType.SQLITE ? 1 : blockStorageConfig.getInt("writeExecutorThread");
            var connectionPoolSize = getConnectionPoolSize(blockDataStorageType, blockStorageConfig);

            if (readExecutorThread + writeExecutorThread > connectionPoolSize) {
                plugin.getLogger().log(Level.WARNING, "Detected that the block‑storage connection pool size is configured smaller than the total number of read/write threads, which may lead to performance issues.");
            }

            initAdapter(blockDataStorageType, DataType.BLOCK_STORAGE, blockStorageConfig);

            var blockDataController =
                    ControllerHolder.createController(BlockDataController.class, blockDataStorageType);
            blockDataController.init(blockStorageAdapter, readExecutorThread, writeExecutorThread);

            if (blockStorageConfig.getBoolean("delayedWriting.enable")) {
                plugin.getLogger().log(Level.INFO, "Enabled delayed write functionality");
                blockDataController.initDelayedSaving(
                        plugin,
                        blockStorageConfig.getInt("delayedWriting.delayedSecond"),
                        blockStorageConfig.getInt("delayedWriting.forceSavePeriod"));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load Slimefun block storage adapter", e);
            return;
        }

        try {
            profileStorageType = StorageType.valueOf(profileConfig.getString("storageType"));
            var readExecutorThread = profileConfig.getInt("readExecutorThread");
            var writeExecutorThread =
                    profileStorageType == StorageType.SQLITE ? 1 : profileConfig.getInt("writeExecutorThread");
            var connectionPoolSize = getConnectionPoolSize(profileStorageType, profileConfig);

            if (readExecutorThread + writeExecutorThread > connectionPoolSize) {
                plugin.getLogger().log(Level.WARNING, "Detected that the profile‑storage connection pool size is configured smaller than the total number of read/write threads, which may lead to performance issues.");
            }

            initAdapter(profileStorageType, DataType.PLAYER_PROFILE, profileConfig);
            var profileController = ControllerHolder.createController(ProfileDataController.class, profileStorageType);
            profileController.init(profileAdapter, readExecutorThread, writeExecutorThread);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player profile adapter", e);
        }
    }

    private void initAdapter(StorageType storageType, DataType dataType, Config databaseConfig) throws IOException {
        switch (storageType) {
            case MYSQL -> {
                var adapter = new MysqlAdapter();

                adapter.prepare(new MysqlConfig(
                        databaseConfig.getString("mysql.host"),
                        databaseConfig.getInt("mysql.port"),
                        databaseConfig.getString("mysql.database"),
                        databaseConfig.getString("mysql.tablePrefix"),
                        databaseConfig.getString("mysql.user"),
                        databaseConfig.getString("mysql.password"),
                        databaseConfig.getBoolean("mysql.useSSL"),
                        databaseConfig.getInt("mysql.maxConnection")));

                switch (dataType) {
                    case BLOCK_STORAGE -> blockStorageAdapter = adapter;
                    case PLAYER_PROFILE -> profileAdapter = adapter;
                }
            }
            case SQLITE -> {
                var adapter = new SqliteAdapter();

                File databasePath = null;

                switch (dataType) {
                    case PLAYER_PROFILE -> {
                        databasePath = new File("data-storage/Slimefun", "profile.db");
                        profileAdapter = adapter;
                    }
                    case BLOCK_STORAGE -> {
                        databasePath = new File("data-storage/Slimefun", "block-storage.db");
                        blockStorageAdapter = adapter;
                    }
                }
                adapter.prepare(new SqliteConfig(
                        databasePath.getAbsolutePath(), databaseConfig.getInt("sqlite.maxConnection")));
            }
            case POSTGRESQL -> {
                var adapter = new PostgreSqlAdapter();

                adapter.prepare(new PostgreSqlConfig(
                        databaseConfig.getString("postgresql.host"),
                        databaseConfig.getInt("postgresql.port"),
                        databaseConfig.getString("postgresql.database"),
                        databaseConfig.getString("postgresql.tablePrefix"),
                        databaseConfig.getString("postgresql.user"),
                        databaseConfig.getString("postgresql.password"),
                        databaseConfig.getBoolean("postgresql.useSSL"),
                        databaseConfig.getInt("postgresql.maxConnection")));

                switch (dataType) {
                    case BLOCK_STORAGE -> blockStorageAdapter = adapter;
                    case PLAYER_PROFILE -> profileAdapter = adapter;
                }
            }
        }
    }

    private int getConnectionPoolSize(StorageType storageType, Config config) {
        return config.getInt(storageType.name().toLowerCase(Locale.ROOT) + ".maxConnection");
    }

    @Nullable public ProfileDataController getProfileDataController() {
        return ControllerHolder.getController(ProfileDataController.class, profileStorageType);
    }

    public BlockDataController getBlockDataController() {
        return ControllerHolder.getController(BlockDataController.class, blockDataStorageType);
    }

    public void shutdown() {
        boolean clean = true;

        try {
            if (getProfileDataController() != null) {
                getProfileDataController().shutdown();
            }
            if (getBlockDataController() != null) {
                getBlockDataController().shutdown();
            }
            clean = (getProfileDataController() == null || getProfileDataController().wasLastShutdownClean())
                    && (getBlockDataController() == null || getBlockDataController().wasLastShutdownClean());
        } catch (RuntimeException ex) {
            clean = false;
            plugin.getLogger().log(Level.SEVERE, "Failed to drain Slimefun database controllers", ex);
        }

        try {
            if (blockStorageAdapter != null) {
                blockStorageAdapter.shutdown();
            }
            if (profileAdapter != null) {
                profileAdapter.shutdown();
            }
        } catch (RuntimeException ex) {
            clean = false;
            plugin.getLogger().log(Level.SEVERE, "Failed to close Slimefun database adapters", ex);
        } finally {
            ControllerHolder.clearControllers();
        }

        if (clean) {
            writeCleanShutdownMarker();
        } else {
            plugin.getLogger().warning("Slimefun did not complete a clean database shutdown. A storage warning will be shown on the next startup.");
        }
    }

    private void markStartupInProgress() {
        File storageDirectory = cleanShutdownMarker.getParentFile();
        if (!storageDirectory.exists() && !storageDirectory.mkdirs()) {
            plugin.getLogger().warning("Could not create the Slimefun data-storage directory for shutdown tracking.");
            return;
        }

        previousShutdownClean = cleanShutdownMarker.exists() || !storageInitializedMarker.exists();
        if (!previousShutdownClean) {
            plugin.getLogger().warning("The previous Slimefun session did not leave a clean-shutdown marker. Back up data-storage/Slimefun and run /sf stability status.");
        }

        try {
            Files.writeString(storageInitializedMarker.toPath(), "initialized\n", StandardCharsets.UTF_8);
            Files.deleteIfExists(cleanShutdownMarker.toPath());
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not clear the Slimefun clean-shutdown marker", ex);
        }
    }

    private void writeCleanShutdownMarker() {
        try {
            Files.writeString(
                    cleanShutdownMarker.toPath(),
                    "Slimefun Legacy clean shutdown\n",
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Could not write the Slimefun clean-shutdown marker", ex);
        }
    }

    public boolean wasPreviousShutdownClean() {
        return previousShutdownClean;
    }

    public int getPendingWriteTaskCount() {
        int pending = 0;
        ProfileDataController profile = getProfileDataController();
        BlockDataController blocks = getBlockDataController();
        if (profile != null) {
            pending += profile.getPendingWriteTaskCount();
        }
        if (blocks != null) {
            pending += blocks.getPendingWriteTaskCount();
        }
        return pending;
    }

    public boolean isBlockDataBase64Enabled() {
        return blockStorageConfig.getBoolean("base64EncodeVal");
    }

    public boolean isProfileDataBase64Enabled() {
        return profileConfig.getBoolean("base64EncodeVal");
    }

    public ChunkDataLoadMode getChunkDataLoadMode() {
        return ChunkDataLoadMode.valueOf(blockStorageConfig.getString("dataLoadMode"));
    }

    public StorageType getBlockDataStorageType() {
        return blockDataStorageType;
    }

    public StorageType getProfileStorageType() {
        return profileStorageType;
    }

    private void initDefaultVal() {
        if (profileConfig.getString("sqlite.maxConnection") == null) {
            profileConfig.setDefaultValue("sqlite.maxConnection", 5);
            profileConfig.save();
        }

        boolean changed = false;

        if (blockStorageConfig.getString("sqlite.maxConnection") == null) {
            blockStorageConfig.setDefaultValue("sqlite.maxConnection", 5);
            changed = true;
        }

        if (blockStorageConfig.getString("dataLoadMode") == null) {
            blockStorageConfig.setDefaultValue("dataLoadMode", "LOAD_WITH_CHUNK");
            changed = true;
        }

        if (changed) blockStorageConfig.save();
    }
}
