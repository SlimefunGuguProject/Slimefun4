package com.xzavier0722.mc.plugin.slimefun4.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlUtils;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.FieldKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import com.xzavier0722.mc.plugin.slimefun4.storage.util.DataUtils;
import java.util.Base64;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

class PublicStorageApiCompatibilityTest {

    @Test
    void keepsLegacyDataUtilsDescriptors() throws Exception {
        var serialize = DataUtils.class.getMethod("serializeItemStack", ItemStack.class);
        var deserialize = DataUtils.class.getMethod("deserializeItemStack", String.class);

        assertSame(String.class, serialize.getReturnType());
        assertSame(ItemStack.class, deserialize.getReturnType());
        assertTrue(serialize.isAnnotationPresent(Deprecated.class));
        assertTrue(deserialize.isAnnotationPresent(Deprecated.class));
        assertEquals("", DataUtils.serializeItemStack(null));
        assertNull(DataUtils.deserializeItemStack((String) null));
    }

    @Test
    void keepsLegacyRecordSetSourceSignature() throws Exception {
        var getAll = RecordSet.class.getMethod("getAll");
        var record = new RecordSet();
        var binaryItem = new byte[] {1, 2, 3};
        record.put(FieldKey.INVENTORY_ITEM, binaryItem);

        assertSame(Map.class, getAll.getReturnType());
        assertTrue(getAll.isAnnotationPresent(Deprecated.class));
        assertEquals(
                "java.util.Map<com.xzavier0722.mc.plugin.slimefun4.storage.common.FieldKey, java.lang.String>",
                getAll.getGenericReturnType().getTypeName());
        assertEquals(Base64.getEncoder().encodeToString(binaryItem), record.get(FieldKey.INVENTORY_ITEM));
        assertEquals(record.get(FieldKey.INVENTORY_ITEM), record.getAll().get(FieldKey.INVENTORY_ITEM));
    }

    @Test
    void keepsLegacySqlUtilsDescriptors() throws Exception {
        var buildKvStr = SqlUtils.class.getMethod("buildKvStr", FieldKey.class, String.class);
        var toSqlValStr = SqlUtils.class.getMethod("toSqlValStr", FieldKey.class, String.class);

        assertSame(String.class, buildKvStr.getReturnType());
        assertSame(String.class, toSqlValStr.getReturnType());
        assertTrue(buildKvStr.isAnnotationPresent(Deprecated.class));
        assertTrue(toSqlValStr.isAnnotationPresent(Deprecated.class));
    }
}
