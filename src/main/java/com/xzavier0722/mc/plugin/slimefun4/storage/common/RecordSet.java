package com.xzavier0722.mc.plugin.slimefun4.storage.common;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.DataUtils;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.ToString;
import org.bukkit.inventory.ItemStack;

@ToString
public class RecordSet {
    private final Map<FieldKey, Object> data;
    private boolean readonly = false;

    public RecordSet() {
        data = new HashMap<>();
    }

    @ParametersAreNonnullByDefault
    public void put(FieldKey key, String val) {
        putValue(key, val);
    }

    @ParametersAreNonnullByDefault
    public void put(FieldKey key, byte[] val) {
        putValue(key, val.clone());
    }

    @ParametersAreNonnullByDefault
    public void put(FieldKey key, ItemStack itemStack) {
        putValue(key, DataUtils.serializeItemStackBytes(itemStack));
    }

    public void put(FieldKey key, boolean val) {
        put(key, val ? "1" : "0");
    }

    private void putValue(FieldKey key, Object value) {
        checkReadonly();
        data.put(key, value);
    }

    /**
     * Returns a legacy String view of this record.
     *
     * <p>Binary fields are represented as Base64 so existing addons that consume this method remain
     * compatible. New storage code should use {@link #getAllValues()}.
     *
     * @return immutable String view
     * @deprecated use {@link #getAllValues()}
     */
    @Deprecated
    @ParametersAreNonnullByDefault
    public Map<FieldKey, String> getAll() {
        var values = new HashMap<FieldKey, String>();
        data.forEach((key, value) -> values.put(key, valueAsString(value)));
        return Collections.unmodifiableMap(values);
    }

    @ParametersAreNonnullByDefault
    public Map<FieldKey, Object> getAllValues() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * Returns a legacy String view of a field.
     *
     * @param key field key
     * @return String value or Base64 for binary fields
     * @deprecated use {@link #getValue(FieldKey)}
     */
    @Deprecated
    @Nullable
    @ParametersAreNonnullByDefault
    public String get(FieldKey key) {
        return valueAsString(data.get(key));
    }

    @Nullable
    @ParametersAreNonnullByDefault
    public Object getValue(FieldKey key) {
        return data.get(key);
    }

    @ParametersAreNonnullByDefault
    public String getOrDef(FieldKey key, String def) {
        var value = get(key);
        return value == null ? def : value;
    }

    @ParametersAreNonnullByDefault
    public int getInt(FieldKey key) {
        return Integer.parseInt(requireString(key));
    }

    @ParametersAreNonnullByDefault
    public ItemStack getItemStack(FieldKey key) {
        var value = data.get(key);
        if (value instanceof byte[] bytes) {
            return DataUtils.deserializeItemStack(bytes);
        }
        return DataUtils.deserializeItemStack((String) value);
    }

    @ParametersAreNonnullByDefault
    public UUID getUUID(FieldKey key) {
        return UUID.fromString(requireString(key));
    }

    public boolean getBoolean(FieldKey key) {
        return getInt(key) == 1;
    }

    public void readonly() {
        readonly = true;
    }

    private String requireString(FieldKey key) {
        var value = get(key);
        if (value == null) {
            throw new IllegalStateException("Missing required field: " + key);
        }
        return value;
    }

    @Nullable private static String valueAsString(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof String string) {
            return string;
        }
        return String.valueOf(value);
    }


    private void checkReadonly() {
        if (readonly) {
            throw new IllegalStateException("RecordSet cannot be modified after readonly() was called.");
        }
    }
}
