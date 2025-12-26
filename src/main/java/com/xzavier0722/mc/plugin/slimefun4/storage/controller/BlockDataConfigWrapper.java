package com.xzavier0722.mc.plugin.slimefun4.storage.controller;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@Deprecated
public class BlockDataConfigWrapper extends Config {
    private final ASlimefunDataContainer blockData;

    public BlockDataConfigWrapper(ASlimefunDataContainer blockData) {
        // fix the problem that Config.loadYaml continuously throw IO Exception during ticking old plugin's machines:
        super();
        this.blockData = blockData;
    }

    @Override
    public void save() {}

    @Override
    public void createFile() {}

    @Override
    public String getString(@Nonnull String path) {
        return blockData.getData(path);
    }

    @Nonnull
    @Override
    public Set<String> getKeys() {
        return new HashSet<>(blockData.getAllData().keySet());
    }

    @Nonnull
    @Override
    public Set<String> getKeys(@Nonnull String path) {
        return getKeys();
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public boolean contains(@Nonnull String path) {
        return getString(path) != null;
    }

    @Nullable @Override
    public Object getValue(@Nonnull String path) {
        return getString(path);
    }

    @Override
    public FileConfiguration getConfiguration() {
        return null;
    }

    @Override
    public void setDefaultValue(@Nonnull String path, @Nullable Object value) {
        if (!(value instanceof String str)) {
            throw new NotImplementedException();
        }
        if (getString(path) == null) {
            blockData.setData(path, str);
        }
    }

    @Override
    public void setValue(@Nonnull String path, Object value) {
        if (value == null) {
            blockData.removeData(path);
        }

        if (!(value instanceof String str)) {
            throw new NotImplementedException();
        }
        blockData.setData(path, str);
    }

    @Override
    public void save(@Nonnull File file) {}

    @Override
    public void reload() {}
}
