package com.xzavier0722.mc.plugin.slimefun4.storage.common;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.DataUtils;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.ToString;
import org.bukkit.inventory.ItemStack;

@ToString
public class RecordSet {
    private final Map<FieldKey, String> data;
    private boolean readonly = false;

    public RecordSet() {
        data = new HashMap<>();
    }

    @ParametersAreNonnullByDefault
    public void put(FieldKey key, String val) {
        checkReadonly();
        data.put(key, val);
    }

    @ParametersAreNonnullByDefault
    public void put(FieldKey key, ItemStack itemStack) {
        checkReadonly();
        data.put(key, DataUtils.serializeItemStack(itemStack));
    }

    public void put(FieldKey key, boolean val) {
        put(key, val ? "1" : "0");
    }

    @ParametersAreNonnullByDefault
    public Map<FieldKey, String> getAll() {
        return Collections.unmodifiableMap(data);
    }

    @ParametersAreNonnullByDefault
    public String get(FieldKey key) {
        return data.get(key);
    }

    @ParametersAreNonnullByDefault
    public String getOrDef(FieldKey key, String def) {
        return data.getOrDefault(key, def);
    }

    @ParametersAreNonnullByDefault
    public int getInt(FieldKey key) {
        return Integer.parseInt(data.get(key));
    }

    @ParametersAreNonnullByDefault
    public ItemStack getItemStack(FieldKey key) {
        try {
            return DataUtils.deserializeItemStack(data.get(key));
        } catch (Exception e) {
            Slimefun.logger().log(Level.SEVERE, "反序列化数据库中的物品失败! 对应物品将不会显示", e);
            Slimefun.logger().log(Level.SEVERE, "相关物品信息: [{0}]", new Object[] {
                data.entrySet().stream()
                        .filter((entry) -> entry.getKey() != key)
                        .map(entry -> entry.getKey().name() + "=" + entry.getValue())
                        .collect(Collectors.toSet())
            });
            return null;
        }
    }

    @ParametersAreNonnullByDefault
    public UUID getUUID(FieldKey key) {
        return UUID.fromString(data.get(key));
    }

    public boolean getBoolean(FieldKey key) {
        return getInt(key) == 1;
    }

    public void readonly() {
        readonly = true;
    }

    private void checkReadonly() {
        if (readonly) {
            throw new IllegalStateException("RecordSet cannot be modified after readonly() called.");
        }
    }
}
