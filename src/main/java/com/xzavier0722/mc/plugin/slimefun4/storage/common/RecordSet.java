package com.xzavier0722.mc.plugin.slimefun4.storage.common;

import com.xzavier0722.mc.plugin.slimefun4.storage.util.DataUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
        checkReadonly();
        data.put(key, val);
    }

    @ParametersAreNonnullByDefault
    public void put(FieldKey key, ItemStack itemStack) {
        checkReadonly();
        data.put(key, DataUtils.serializeItemStackBytes(itemStack));
    }

    @ParametersAreNonnullByDefault
    public void put(FieldKey key, byte[] val) {
        checkReadonly();
        data.put(key, val);
    }

    public void put(FieldKey key, boolean val) {
        put(key, val ? "1" : "0");
    }

    /**
     * 获取兼容旧 API 的字符串数据视图，二进制值会转换为 Base64。
     *
     * @return 只读字符串数据视图
     * @deprecated 请使用 {@link #getAllValues()} 获取包含原始二进制值的数据
     */
    @Deprecated
    @ParametersAreNonnullByDefault
    public Map<FieldKey, String> getAll() {
        var stringData = new HashMap<FieldKey, String>();
        data.forEach((key, value) -> stringData.put(key, toStringValue(value)));
        return Collections.unmodifiableMap(stringData);
    }

    public Map<FieldKey, Object> getAllValues() {
        return Collections.unmodifiableMap(data);
    }

    @ParametersAreNonnullByDefault
    public String get(FieldKey key) {
        return toStringValue(data.get(key));
    }

    public Object getValue(FieldKey key) {
        return data.get(key);
    }

    @ParametersAreNonnullByDefault
    public String getOrDef(FieldKey key, String def) {
        var value = toStringValue(data.get(key));
        return value == null ? def : value;
    }

    @ParametersAreNonnullByDefault
    public int getInt(FieldKey key) {
        return Integer.parseInt(get(key));
    }

    @ParametersAreNonnullByDefault
    public ItemStack getItemStack(FieldKey key) {
        var value = data.get(key);
        return value instanceof byte[] bytes
                ? DataUtils.deserializeItemStack(bytes)
                : DataUtils.deserializeItemStack((String) value);
    }

    @ParametersAreNonnullByDefault
    public UUID getUUID(FieldKey key) {
        return UUID.fromString(get(key));
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

    private String toStringValue(Object value) {
        return value instanceof byte[] bytes ? java.util.Base64.getEncoder().encodeToString(bytes) : (String) value;
    }
}
