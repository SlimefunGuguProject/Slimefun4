package com.xzavier0722.mc.plugin.slimefun4.storage.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FieldMapper<T> {

    private final Map<FieldKey, T> map;
    private final Map<T, FieldKey> inverseMap;

    public FieldMapper(Map<FieldKey, T> map) {
        this.map = map;
        this.inverseMap = Collections.synchronizedMap(new HashMap<>());
        map.forEach((k, v) -> inverseMap.put(v, k));
    }

    public T get(FieldKey key) {
        return map.get(key);
    }

    public FieldKey get(T key) {
        return inverseMap.get(key);
    }
}
